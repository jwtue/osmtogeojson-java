package de.jonaswolf.osmtogeojson

import de.jonaswolf.osmtogeojson.options.*
import de.jonaswolf.osmtogeojson.parser.OsmXmlParser
import de.jonaswolf.osmtogeojson.parser.OverpassJsonParser
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.w3c.dom.Document
import org.xml.sax.InputSource
import org.xml.sax.SAXException
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory

// source: https://github.com/tyrasd/osmtogeojson/blob/gh-pages/index.js
class OsmToGeoJson(
    val verbose: Boolean = false,
    val flatProperties: Boolean = false,
    val uninterestingTags: List<UninterestingTag> = listOf(
        OverallUninterestingTag("source"),
        OverallUninterestingTag("source_ref"),
        OverallUninterestingTag("source:ref"),
        OverallUninterestingTag("history"),
        OverallUninterestingTag("attribution"),
        OverallUninterestingTag("created_by"),
        OverallUninterestingTag("tiger:county"),
        OverallUninterestingTag("tiger:tlid"),
        OverallUninterestingTag("tiger:upload_uuid")
    ),
    val uninterestingTagsValidator: UninterestingTagsValidator? = null,
    additionalPolygonFeatures: JSONObject? = null,
    val polygonFeaturesValidator: PolygonFeatureValidator? = null,
    val deduplicator: Deduplicator = DefaultDeduplicator()
) {

    val polygonFeatures = OsmPolygonFeatures.polygonFeatures

    init {
        if (additionalPolygonFeatures != null) {
            polygonFeatures.merge(
                additionalPolygonFeatures
            )
        }
    }

    fun convertOverpassJsonToGeoJson(data: String, featureCallback: ((String) -> Unit)? = null): String {
        val container = try {
            JSONObject(data)
        } catch (_: JSONException) {
            null
        }
        val element = try {
            container?.optJSONArray("elements") ?: JSONArray(
                data
            )
        } catch (e: JSONException) {
            throw IllegalArgumentException("Input must be a JSON array of elements or a JSON object containing such an array at the key \"elements\".")
        }
        return overpassJson2geoJson(element, featureCallback).toString()
    }

    fun convertOsmXmlToGeoJson(data: String, featureCallback: ((String) -> Unit)? = null): String {
        try {
            val dbFactory = DocumentBuilderFactory.newInstance()
            val dBuilder = dbFactory.newDocumentBuilder()
            val doc = dBuilder.parse(InputSource(StringReader(data)))
            return osmXml2geoJSON(doc, featureCallback).toString()
        } catch (e: SAXException) {
            throw IllegalArgumentException("Input must be a valid XML document.")
        }
    }

    private fun overpassJson2geoJson(data: JSONArray, featureCallback: ((String) -> Unit)? = null): JSONObject {
        val parser = OverpassJsonParser.parse(data)
        return convert2geoJson(parser.nodes, parser.ways, parser.rels, featureCallback)
    }

    private fun osmXml2geoJSON(doc: Document, featureCallback: ((String) -> Unit)? = null): JSONObject {
        val parser = OsmXmlParser.parse(doc)
        return convert2geoJson(parser.nodes, parser.ways, parser.rels, featureCallback)
    }

    private fun convert2geoJson(
        nodes: List<JSONObject>,
        ways: List<JSONObject>,
        rels: List<JSONObject>,
        featureCallback: ((String) -> Unit)? = null
    ): JSONObject {

        // some data processing (e.g. filter nodes only used for ways)
        val nodeIds = mutableMapOf<Any, JSONObject>()
        val poiNIds = mutableMapOf<Any, Boolean>()

        for (it in nodes) {
            var node = it
            nodeIds[node.get("id")]?.let {
                // handle input data duplication
                node = deduplicator.deduplicate(node, it)
            }
            nodeIds[node.get("id")] = node
            if (hasInterestingTags(node.optJSONObject("tags"))) { // this checks if the node has any tags other than "created_by"
                poiNIds[node.get("id")] = true
            }
        }

        // todo -> after deduplication of relations??
        for (rel in rels) {
            val members = rel.optJSONArray("members") ?: JSONArray()
            for (member in members.toListOfJSONObjects().filterNotNull()) {
                if (member.getString("type") == "node") {
                    poiNIds[member.get("ref")] = true
                }
            }
        }

        val wayIds = mutableMapOf<Any, JSONObject>()
        val wayNIds = mutableMapOf<Any, Boolean>()

        for (it in ways) {
            var way = it
            wayIds[way.get("id")]?.let {
                // handle input data duplication
                way = deduplicator.deduplicate(way, it)
            }
            wayIds[way.get("id")] = way
            val wayNodes = way.optJSONArray("nodes")
            if (wayNodes != null) {
                for (j in wayNodes.indices) {
                    if (wayNodes.optJSONObject(j) != null) continue // ignore already replaced way node objects
                    wayNIds[wayNodes[j]] = true
                    wayNodes.put(j, nodeIds[wayNodes[j]])
                }
                way.put("nodes", wayNodes)
            }
        }

        val pois = mutableListOf<JSONObject>()
        for ((id, node) in nodeIds) {
            if (!wayNIds.getOrDefault(id, false) || poiNIds.getOrDefault(id, false)) {
                pois.add(node)
            }
        }
        val relIds = mutableMapOf<Any, JSONObject>()
        for (it in rels) {
            var rel = it
            relIds[rel.get("id")]?.let {
                // handle input data duplication
                rel = deduplicator.deduplicate(rel, it)
            }
            relIds[rel.get("id")] = rel
        }

        val relsmap = Relsmap()
        for ((_, rel) in relIds) {
            if (rel.optJSONArray("members") == null) {
                // ignore relations without members (e.g. returned by an ids_only query)
                warn("Relation", rel.opt("type")?.toString() + "/" + rel.opt("id"), "ignored because it has no members")
                break
            }
            val members = rel.getJSONArray("members").toListOfJSONObjects().filterNotNull()
            for (m in members) {
                val mType = m.get("type")
                var mRef = m.get("ref")
                if (m.optNumber("ref") == null) {
                    // de-namespace full geometry content
                    mRef = m.getString("ref").replace("_fullGeom", "")
                }
                if (!relsmap.contains(mType)) {
                    warn(
                        "Relation",
                        rel.opt("type")?.toString() + "/" + rel.opt("id"),
                        "ignored because it has an invalid type"
                    )
                    continue
                }
                if (!relsmap.contains(mType) || !relsmap[mType]!!.contains(mRef)) {
                    relsmap[mType]?.put(mRef, mutableListOf())
                }
                relsmap[mType]?.get(mRef)?.add(
                    RelsmapEntry(
                        m.opt("role"),
                        rel.optNumber("id") ?: rel.get("id"),
                        rel.optJSONObject("tags",
                            JSONObject()
                        )
                    )
                )
            }
        }

        // construct geojson
        val geoJsonNodes = mutableListOf<JSONObject>()

        for (poi in pois) {
            if (!poi.has("lon") || !poi.has("lat")) {
                warn("POI", poi.opt("type")?.toString() + "/" + poi.opt("id"), "ignored because it lacks coordinates");
                continue // lon and lat are required for showing a point
            }
            val feature = JSONObject().apply {
                put("type", "Feature")
                put("id", poi.get("type").toString() + "/" + poi.get("id"))
                put("properties", JSONObject().apply {
                    put("type", poi.get("type"))
                    put("id", poi.optNumber("id") ?: poi.get("id"))
                    put("tags", poi.optJSONObject("tags",
                        JSONObject()
                    ))
                    put("relations", relsmap.node[poi.get("id")]?.map { it.json }?.let { JSONArray(it) } ?: JSONArray())
                    put("meta", buildMetaInformation(poi))
                })
                put("geometry", JSONObject().apply {
                    put("type", "Point")
                    put("coordinates", JSONArray().apply {
                        put(poi.getNumber("lon"))
                        put(poi.getNumber("lat"))
                    })
                })
            }
            if (poi.optBoolean("__is_center_placeholder", false)) {
                val properties = feature.getJSONObject("properties")
                properties.put("geometry", "center")
                feature.put("properties", properties)
            }
            featureCallback?.invoke(feature.toString())
            geoJsonNodes.add(feature)
        }

        val geoJsonLines = mutableListOf<JSONObject>()
        val geoJsonPolygons = mutableListOf<JSONObject>()
        // process multipolygons
        for (rel in rels) {
            // todo: refactor such that this loops over relids instead of rels?
            if (relIds[rel["id"]] != rel) {
                // skip relation because it's a deduplication artifact
                continue
            }
            val tags = rel.optJSONObject("tags",
                JSONObject()
            )
            if (tags.opt("type") == "route" || tags.opt("type") == "waterway") {
                if (rel.optJSONArray("members") == null) {
                    warn(
                        "Route",
                        rel.opt("type")?.toString() + "/" + rel.opt("id"),
                        "ignored because it has no members"
                    )
                    continue // ignore relations without members (e.g. returned by an ids_only query)
                }
                rel.getJSONArray("members").toListOfJSONObjects().filterNotNull().forEach { m ->
                    wayIds[m["ref"]]?.let { wayIdsMRef ->
                        if (!hasInterestingTags(wayIdsMRef.optJSONObject("tags"))) {
                            wayIdsMRef.put("is_skippablerelationmember", true)
                            wayIds[m["ref"]] = wayIdsMRef
                        }
                    }
                }
                val feature = constructMultiLineString(wayIds, relsmap, rel)
                if (feature == null) {
                    warn(
                        "Route",
                        rel.opt("type")?.toString() + "/" + rel.opt("id"),
                        "ignored because it has invalid geometry"
                    )
                    continue // abort if feature could not be constructed
                }
                featureCallback?.invoke(GeoJsonRewind.rewind(feature).toString())
                geoJsonPolygons.add(feature)
            }// end construct multilinestring for route relations
            if (tags.opt("type") == "multipolygon" || tags.opt("type") == "boundary") {
                if (rel.optJSONArray("members") == null) {
                    warn(
                        "Multipolygon",
                        rel.opt("type")?.toString() + "/" + rel.opt("id"),
                        "ignored because it has no members"
                    )
                    continue // ignore relations without members (e.g. returned by an ids_only query)
                }
                var outerCount = 0
                for ((j, member) in rel.getJSONArray("members").toListOfJSONObjects().withIndex()) {
                    if (member?.get("role") == "outer") {
                        outerCount++
                    } else if (member?.get("role") != "inner") {
                        warn(
                            "Multipolygon",
                            rel.opt("type")?.toString() + "/" + rel.opt("id"),
                            "member",
                            rel.optJSONArray("members")?.optJSONObject(j)?.opt("type")
                                ?.toString() + '/' + rel.optJSONArray("members")?.optJSONObject(j)?.opt("ref"),
                            "ignored because it has an invalid role: \"" + rel.optJSONArray("members")?.optJSONObject(j)
                                ?.opt("role") + "\""
                        )
                    }
                }
                (rel.optJSONArray("members") ?: JSONArray()).toListOfJSONObjects().filterNotNull().forEach { m ->
                    wayIds[m.opt("ref")!!]?.let {
                        // this even works in the following corner case:
                        // a multipolygon amenity=xxx with outer line tagged amenity=yyy
                        // see https://github.com/tyrasd/osmtogeojson/issues/7
                        if (m["role"] == "outer" && !hasInterestingTags(
                                it.optJSONObject("tags"),
                                rel.optJSONObject("tags",
                                    JSONObject()
                                )
                            )
                        ) {
                            it.put("is_skippablerelationmember", true)
                        } else if (m["role"] == "inner" && !hasInterestingTags(it.optJSONObject("tags"))) {
                            it.put("is_skippablerelationmember", true)
                        }
                    }
                }
                if (outerCount == 0) {
                    warn(
                        "Multipolygon relation",
                        rel.opt("type")?.toString() + "/" + rel.opt("id"),
                        "ignored because it has no outer ways"
                    )
                    continue // ignore multipolygons without outer ways
                }
                var simpleMp = false
                if (outerCount == 1 && !hasInterestingTags(
                        rel.optJSONObject("tags"),
                        JSONObject(mapOf("type" to true))
                    )
                ) {
                    simpleMp = true
                }
                val feature = if (!simpleMp)
                    constructMultipolygon(wayIds, relsmap, rel, rel, simpleMp)
                else {
                    // simple multipolygon
                    var outerWay = (rel.optJSONArray("members") ?: JSONArray()).toListOfJSONObjects()
                        .first { m -> m?.get("role") == "outer" }
                    outerWay = wayIds[outerWay?.get("ref")]
                    if (outerWay == null) {
                        warn(
                            "Multipolygon relation",
                            rel.opt("type")?.toString() + "/" + rel.opt("id"),
                            "ignored because outer way is missing"
                        )
                        continue // abort if outer way object is not present
                    }
                    outerWay.put("is_skippablerelationmember", true)
                    constructMultipolygon(wayIds, relsmap, outerWay, rel, simpleMp)
                }
                if (feature == null) {
                    warn(
                        "Multipolygon relation",
                        rel.opt("type")?.toString() + "/" + rel.opt("id"),
                        "ignored because it has invalid geometry"
                    )
                    continue // abort if feature could not be constructed
                }
                featureCallback?.invoke(GeoJsonRewind.rewind(feature).toString())
                geoJsonPolygons.add(feature)
            }
        }
        // process lines and polygons
        for (way in ways) {
            // todo: refactor such that this loops over wayids instead of ways?
            if (wayIds[way["id"]] != way) {
                // skip way because it's a deduplication artifact
                continue
            }
            if (way.optJSONArray("nodes") == null) {
                warn("Way", way.opt("type")?.toString() + '/' + way.opt("id"), "ignored because it has no nodes")
                continue // ignore ways without nodes (e.g. returned by an ids_only query)
            }
            if (way.optBoolean("is_skippablerelationmember", false)) {
                continue // ignore ways which are already rendered as (part of) a multipolygon
            }
            if (way.optNumber("id") == null) {
                // remove full geometry namespace for output
                way.put("id", way.getString("id").replace("_fullGeom", ""))
            }
            way.put("tainted", false)
            way.put("hidden", false)
            var coords = mutableListOf<JSONArray>()
            val wayNodes = way.optJSONArray("nodes") ?: JSONArray()
            for (node in wayNodes.toListOfJSONObjects()) {
                if (node != null) {
                    coords.add(
                        JSONArray().apply {
                            put(node.optNumber("lon")?.toDouble())
                            put(node.optNumber("lat")?.toDouble())
                        }
                    )
                } else {
                    warn("Way", way.opt("type")?.toString() + '/' + way.opt("id"), "is tainted by an invalid node")
                    way.put("tainted", true)
                }
            }
            if (coords.size <= 1) { // invalid way geometry
                warn(
                    "Way",
                    way.opt("type")?.toString() + '/' + way.opt("id"),
                    "ignored because it contains too few nodes"
                )
                continue
            }
            var wayType = "LineString" // default
            if (wayNodes.opt(0) != null && wayNodes.opt(wayNodes.length() - 1) != null && // way has its start/end nodes loaded
                wayNodes.getJSONObject(0)["id"] == wayNodes.getJSONObject(wayNodes.length() - 1)["id"] && // ... and forms a closed ring
                (
                        way.optJSONObject("tags") != null && // ... and has tags
                                isPolygonFeature(way.getJSONObject("tags"))  // ... and tags say it is a polygon
                                || // or is a placeholder for a bounds geometry
                                way.optBoolean("__is_bounds_placeholder", false)
                        )
            ) {
                wayType = "Polygon"
                coords = mutableListOf(JSONArray(coords))
            }
            val feature = JSONObject(
                mapOf(
                    "type" to "Feature",
                    "id" to way.get("type").toString() + "/" + way.get("id"),
                    "properties" to JSONObject(
                        mapOf(
                            "type" to way.get("type"),
                            "id" to (way.optNumber("id") ?: way.get("id")),
                            "tags" to way.optJSONObject(
                                "tags",
                                JSONObject()
                            ),
                            "relations" to (relsmap.way[way["id"]]?.map { it.json }
                                ?: JSONArray()),
                            "meta" to buildMetaInformation(way)
                        )
                    ).apply {
                        if (way.optBoolean("tainted", false)) {
                            warn("Way", way.opt("type")?.toString() + '/' + way.opt("id"), "is tainted")
                            put("tainted", true)
                        }
                        if (way.optBoolean("__is_bounds_placeholder", false)) {
                            put("geometry", "bounds")
                        }
                    },
                    "geometry" to JSONObject(
                        mapOf(
                            "type" to wayType,
                            "coordinates" to JSONArray(
                                coords
                            )
                        )
                    )
                )
            )
            featureCallback?.invoke(GeoJsonRewind.rewind(feature).toString())
            if (wayType == "LineString") {
                geoJsonLines.add(feature)
            } else {
                geoJsonPolygons.add(feature)
            }
        }

        var geoJson = JSONObject().apply {
            put("type", "FeatureCollection")
            put("features", JSONArray().apply {
                putAll(geoJsonPolygons)
                putAll(geoJsonLines)
                putAll(geoJsonNodes)
            })
        }
        // optionally, flatten properties
        if (flatProperties) {
            val features = JSONArray().apply {
                geoJson.getJSONArray("features").toListOfJSONObjects().filterNotNull().forEach { f ->
                    f.put(
                        "properties", merge(
                            f.getJSONObject("properties").optJSONObject("meta"),
                            f.getJSONObject("properties").optJSONObject("tags"),
                            JSONObject(
                                mapOf(
                                    "id" to f.getJSONObject("properties").get("type")
                                        .toString() + "/" + f.getJSONObject("properties").get("id")
                                )
                            )
                        )
                    )
                    put(f)
                }
            }
            geoJson.put("features", features)
        }
        // fix polygon winding
        geoJson = GeoJsonRewind.rewind(geoJson)
        return geoJson
    }

    private fun constructMultiLineString(
        wayIds: MutableMap<Any, JSONObject>,
        relsmap: Relsmap,
        rel: JSONObject
    ): JSONObject? {
        var isTainted = false
        // prepare route members
        var members = (rel.optJSONArray("members") ?: JSONArray()).toListOfJSONObjects().filterNotNull()
            .filter { it.opt("type") == "way" }
        members = members.mapNotNull { m ->
            val way = wayIds[m["ref"]]
            if (way?.optJSONArray("nodes") == null) { // check for missing ways
                warn(
                    "Route",
                    rel.opt("type")?.toString() + "/" + rel.opt("id"),
                    "tainted by a missing or incomplete way",
                    m.opt("type")?.toString() + "/" + m.opt("ref")
                )
                isTainted = true
                return@mapNotNull null
            }
            return@mapNotNull JSONObject().apply {
                put("id", m["ref"])
                put("role", m["role"])
                put("way", way)
                put("nodes", JSONArray().apply {
                    val values = (way.optJSONArray("nodes") ?: JSONArray()).toListOfJSONObjects()
                    val valuesNotNull = values.filterNotNull()
                    if (values.size != valuesNotNull.size) {
                        isTainted = true
                    }
                    putAll(valuesNotNull)
                })
            }
        }
        // construct connected linestrings
        val linestrings = join(members)

        // sanitize mp-coordinates (remove empty clusters or rings, {lat,lon,...} to [lon,lat]
        val coords = linestrings.map { linestring ->
            linestring.toListOfJSONObjects().filterNotNull().mapNotNull { node ->
                if (node.has("lon") && node.has("lat"))
                    JSONArray(
                        arrayOf(
                            node.get("lon"),
                            node.get("lat")
                        )
                    )
                else null
            }
        }

        if (coords.isEmpty()) {
            warn("Route", rel.opt("type")?.toString() + "/" + rel.opt("id"), "contains no coordinates")
            return null // ignore routes without coordinates
        }

        // mp parsed, now construct the geoJSON
        return JSONObject(
            mapOf(
                "type" to "Feature",
                "id" to rel.get("type").toString() + "/" + rel.get("id"),
                "properties" to JSONObject(mapOf(
                    "type" to rel.get("type"),
                    "id" to (rel.optNumber("id") ?: rel.get("id")),
                    "tags" to rel.optJSONObject(
                        "tags",
                        JSONObject()
                    ),
                    "relations" to JSONArray(
                        relsmap[rel["type"]]?.get(
                            rel["id"]
                        )?.map { it.json }
                            ?: listOf<JSONObject>()),
                    "meta" to buildMetaInformation(rel)
                )).apply {
                    if (isTainted) {
                        warn("Route", rel.opt("type")?.toString() + "/" + rel.opt("id"), "is tainted")
                        put("tainted", true)
                    }
                },
                "geometry" to JSONObject(
                    mapOf(
                        "type" to if (coords.size == 1) "LineString" else "MultiLineString",
                        "coordinates" to JSONArray(if (coords.size == 1) coords.first() else coords)
                    )
                )
            )
        )
    }

    private fun constructMultipolygon(
        wayIds: MutableMap<Any, JSONObject>,
        relsmap: Relsmap,
        tagObject: JSONObject,
        rel: JSONObject,
        simpleMp: Boolean
    ): JSONObject? {
        var isTainted = false
        val mpGeometry = if (simpleMp) "way" else "relation"
        val mpId = if (tagObject.optNumber("id") != null) tagObject.getNumber("id") else tagObject.getString("id")
            .replace("_fullGeom", "")
        // prepare mp members
        var members: List<JSONObject> =
            (rel.optJSONArray("members") ?: JSONArray()).toListOfJSONObjects().filterNotNull()
                .filter { it.get("type") == "way" }
        members = members.mapNotNull { m ->
            val way = wayIds[m["ref"]]
            if (way?.optJSONArray("nodes") == null) { // check for missing ways
                warn(
                    "Multipolygon",
                    mpGeometry + "/" + mpId,
                    "tainted by a missing or incomplete way",
                    m.opt("type")?.toString() + "/" + m.opt("ref")
                )
                isTainted = true
                return@mapNotNull null
            }
            var role : String? = m.optString("role")
            if (role == null || role.isEmpty()) role = "outer"
            return@mapNotNull JSONObject(
                mapOf(
                    "id" to m["ref"],
                    "role" to role,
                    "way" to way,
                    "nodes" to JSONArray().apply {
                        val values = (way.optJSONArray("nodes")
                            ?: JSONArray()).toListOfJSONObjects()
                        val valuesNotNull = values.filterNotNull()
                        if (values.size != valuesNotNull.size) {
                            warn(
                                "Route",
                                rel.opt("type")?.toString() + "/" + rel.opt("id"),
                                "tainted by a way",
                                m.opt("type")?.toString() + "/" + m.opt("ref"),
                                "with a missing node"
                            )
                            isTainted = true
                            warn(
                                "Multipolygon",
                                mpGeometry + "/" + mpId,
                                "tainted by a way",
                                m.opt("type")?.toString() + "/" + m.opt("ref"),
                                "with a missing node"
                            )
                        }
                        putAll(valuesNotNull)
                    })
            )
        }
        // construct outer and inner rings
        val outers = join(members.filter { it.opt("role") == "outer" })
        val inners = join(members.filter { it.opt("role") == "inner" })
        // sort rings
        val mp: MutableList<JSONArray?> = outers.map {
            JSONArray(
                listOf(it)
            )
        }.toMutableList()
        for (j in inners.indices) {
            val o = findOuter(outers, inners[j].toListOfJSONObjects().filterNotNull())
            if (o != null) {
                val currentArray = mp[o] ?: JSONArray()
                currentArray.put(inners[j])
                mp[o] = currentArray
            } else {
                warn("Multipolygon", mpGeometry + '/' + mpId, "contains an inner ring with no containing outer");
                // so, no outer ring for this inner ring is found.
                // We're going to ignore holes in empty space.
            }
        }
        // sanitize mp-coordinates (remove empty clusters or rings, {lat,lon,...} to [lon,lat]
        var mpCoords: List<Any?> = mp.filterNotNull().mapNotNull { cluster ->
            val cl = cluster.toListOfJSONArrays().filterNotNull().mapNotNull { ring ->
                if (ring.length() < 4) { // todo: is this correct: ring.length < 4 ?
                    warn("Multipolygon", mpGeometry + '/' + mpId, "contains a ring with less than four nodes")
                    null
                } else {
                    JSONArray(
                        ring.toListOfJSONObjects().filterNotNull().map { node ->
                            JSONArray().apply {
                                put(node.getNumber("lon").toDouble())
                                put(node.getNumber("lat").toDouble())
                            }
                        })
                }
            }
            if (cl.isEmpty()) {
                warn("Multipolygon", mpGeometry + '/' + mpId, "contains an empty ring cluster")
                null
            } else cl
        }

        if (mpCoords.isEmpty()) {
            warn("Multipolygon", mpGeometry + '/' + mpId, "contains no coordinates")
            return null // ignore multipolygons without coordinates
        }
        var mpType = "MultiPolygon"
        if (mpCoords.size == 1) {
            mpType = "Polygon"
            mpCoords = mpCoords[0] as List<Any?>
        }
        val feature = JSONObject(mapOf(
            "type" to "Feature",
            "id" to tagObject.get("type").toString() + "/" + mpId,
            "properties" to JSONObject(mapOf(
                "type" to tagObject.get("type"),
                "id" to mpId,
                "tags" to tagObject.optJSONObject(
                    "tags",
                    JSONObject()
                ),
                "relations" to (relsmap[tagObject.getString("type")]?.get(tagObject.get("id"))
                    ?.map { it.json }?.let {
                        JSONArray(it)
                    } ?: JSONArray()),
                "meta" to buildMetaInformation(tagObject)
            )).apply {
                if (isTainted) {
                    warn("Multipolygon", mpGeometry + '/' + mpId, "contains no coordinates")
                    put("tainted", true)
                }
            },
            "geometry" to JSONObject(
                mapOf(
                    "type" to mpType,
                    "coordinates" to JSONArray(mpCoords)
                )
            )
        )
        )
        return feature
    }


    private fun buildMetaInformation(obj: JSONObject): JSONObject {
        return JSONObject().apply {
            if (obj.has("timestamp")) put("timestamp", obj["timestamp"])
            if (obj.has("version")) put("version", obj["version"])
            if (obj.has("changeset")) put("changeset", obj["changeset"])
            if (obj.has("user")) put("user", obj["user"])
            if (obj.has("uid")) put("uid", obj["uid"])
        }
    }

    private fun hasInterestingTags(tags: JSONObject?, ignoreTags: List<UninterestingTag> = listOf()): Boolean {
        val checkTags = (tags ?: JSONObject())
        if (uninterestingTagsValidator != null) {
            return !uninterestingTagsValidator.validate(tags ?: JSONObject(), ignoreTags)
        }
        for ((k, v) in checkTags.toMap()) {
            if (!(uninterestingTags.any { it.matches(k, v) }) && !(ignoreTags.any { it.matches(k, v) })
            ) {
                return true
            }
        }
        return false
    }

    private fun hasInterestingTags(tags: JSONObject?, ignoreTags: JSONObject): Boolean {
        val mutableList = mutableListOf<UninterestingTag>()
        for ((k, v) in ignoreTags.toMap()) {
            if (v is Boolean && v == true) {
                mutableList.add(OverallUninterestingTag(k))
            } else if (v is String) {
                mutableList.add(ValueDependantUninterestingTag(k, v))
            }
        }
        return hasInterestingTags(tags, mutableList)
    }

    private fun join(ways: List<JSONObject>): List<JSONArray> {
        // helper that joins adjacent osm ways into linestrings or linear rings
        val fitTogether = fun(n1: JSONObject?, n2: JSONObject?): Boolean {
            return n1 != null && n2 != null && n1.get("id") == n2.get("id")
        }
        val push = fun(array: JSONArray, element: JSONArray) {
            array.putAll(element)
        }
        val unshift = fun(array: JSONArray, element: JSONArray) {
            for (i in array.indices.reversed()) {
                array.put(i + element.length(), array[i])
            }
            for (i in element.indices) {
                array.put(i, element[i])
            }
        }

        // stolen from iD/relation.js
        val joined = mutableListOf<JSONArray>()
        val waysQueue = ArrayDeque(ways)
        var current: JSONArray
        var first: JSONObject?
        var last: JSONObject?
        var i: Int
        var how: ((JSONArray, JSONArray) -> Unit)? = null
        var what: JSONArray? = null
        outer@ while (waysQueue.isNotEmpty()) {
            val currentWay = waysQueue.removeLast()
            current = currentWay.getJSONArray("nodes").slice()
            joined.add(current)
            middle@ while (waysQueue.isNotEmpty() && !fitTogether(
                    current.firstAsJSONObject(),
                    current.lastAsJSONObject()
                )
            ) {
                first = current.firstAsJSONObject()
                last = current.lastAsJSONObject()
                i = 0
                inner@ while (i < waysQueue.size) {
                    what = waysQueue[i].getJSONArray("nodes")
                    if (fitTogether(last, what.firstAsJSONObject())) {
                        how = push
                        what = what.slice(1)
                        break@inner
                    } else if (fitTogether(last, what.lastAsJSONObject())) {
                        how = push
                        what = what.slice(0, -1).reverse()
                        break@inner
                    } else if (fitTogether(first, what.lastAsJSONObject())) {
                        how = unshift
                        what = what.slice(0, -1)
                        break@inner
                    } else if (fitTogether(first, what.firstAsJSONObject())) {
                        how = unshift
                        what = what.slice(1).reverse()
                        break@inner
                    } else {
                        what = null
                        how = null
                    }
                    i++
                }
                if (what == null) {
                    break@middle // Invalid geometry (dangling way, unclosed ring)
                }
                if (i < waysQueue.size)
                    waysQueue.removeAt(i)
                how?.invoke(current, what)
            }
        }
        return joined
    }

    // stolen from iD/geo.js,
    // based on https://github.com/substack/point-in-polygon,
    // ray-casting algorithm based on http://www.ecse.rpi.edu/Homepages/wrf/Research/Short_Notes/pnpoly.html
    private fun pointInPolygon(point: JSONArray, polygon: JSONArray): Boolean {
        val x = point.getDouble(0)
        val y = point.getDouble(1)
        var inside = false
        var i = 0
        var j = polygon.length() - 1
        while (i < polygon.length()) {
            val xi = polygon.getJSONArray(i).getDouble(0)
            val yi = polygon.getJSONArray(i).getDouble(1)
            val xj = polygon.getJSONArray(j).getDouble(0)
            val yj = polygon.getJSONArray(j).getDouble(1)
            val intersect = ((yi > y) != (yj > y)) && (x < (xj - xi) * (y - yi) / (yj - yi) + xi)
            if (intersect) inside = !inside
            j = i++
        }
        return inside
    }

    private fun polygonIntersectsPolygon(outer: List<JSONArray>, inner: List<JSONArray>): Boolean {
        for (i in inner) {
            if (pointInPolygon(i, JSONArray(outer)))
                return true
        }
        return false
    }

    private fun mapCoordinates(from: List<JSONObject>): List<JSONArray> {
        return from.map { n ->
            JSONArray(
                listOf(
                    n.getNumber("lat"),
                    n.getNumber("lon")
                )
            )
        }
    }

    private fun findOuter(outers: List<JSONArray>, inner: List<JSONObject>): Int? {
        // stolen from iD/relation.js
        // todo: all this coordinate mapping makes this unneccesarily slow.
        // see the "todo: this is slow! :(" above.
        val mappedInner = mapCoordinates(inner)
        /*for (o = 0; o < outers.length; o++) {
          outer = mapCoordinates(outers[o]);
          if (polygonContainsPolygon(outer, inner))
            return o;
        }*/
        for ((o, oo) in outers.withIndex()) {
            val outer = mapCoordinates(oo.toListOfJSONObjects().filterNotNull())
            if (polygonIntersectsPolygon(outer, mappedInner)) {
                return o
            }
        }
        return null
    }

    private fun isPolygonFeature(tags: JSONObject): Boolean {

        if (polygonFeaturesValidator != null) {
            return polygonFeaturesValidator.validate(tags)
        }

        // explicitely tagged non-areas
        if (tags.opt("area") == "no")
            return false

        // assuming that a typical OSM way has in average less tags than
        // the polygonFeatures list, this way around should be faster
        for ((key, vall) in tags.toStringMap()) {
            val pfk = polygonFeatures.opt(key)
            // continue with next if tag is unknown or not "categorizing"
            if (pfk == null)
                continue
            // continue with next if tag is explicitely un-set ("building=no")
            if (vall == "no")
                continue
            // check polygon features for: general acceptance, included or excluded values
            if (pfk == true)
                return true
            if (pfk is JSONObject) {
                if (pfk.optJSONObject("included_values") != null && pfk.optJSONObject("included_values")?.optBoolean(vall, false) == true) {
                    return true
                }
                if (pfk.optJSONObject("excluded_values") != null && pfk.optJSONObject("excluded_values")?.optBoolean(vall, false) != true) {
                    return true
                }
            }
        }
        // if no tags matched, this ain't no area.
        return false
    }

    private fun merge(vararg sources: Any?): JSONObject {
        var output = (sources.first() as? JSONObject) ?: JSONObject()
        for (source in sources) {
            if (source == output) continue
            (source as? JSONObject)?.let {
                for (k in it.keys()) {
                    if (it[k] is JSONObject) {
                        output = merge(output, it[k])
                    } else if (it[k] != JSONObject.NULL) {
                        output.put(k, it[k])
                    }
                }
            }
            (source as? JSONArray)?.let {
                for (item in it) {
                    output = merge(output, it)
                }
            }
        }
        return output
    }

    private fun warn(vararg msgs: String) {
        if (verbose) {
            println("Warning: " + msgs.joinToString(" "))
        }
    }

    data class ParserResult(
        val nodes: List<JSONObject>,
        val ways: List<JSONObject>,
        val rels: List<JSONObject>
    )
}