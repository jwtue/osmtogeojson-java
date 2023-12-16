package de.jonaswolf.osmtogeojson.parser

import de.jonaswolf.osmtogeojson.first
import de.jonaswolf.osmtogeojson.getNumber
import de.jonaswolf.osmtogeojson.putAll
import de.jonaswolf.osmtogeojson.toListOfJSONObjects
import org.json.JSONArray
import org.json.JSONObject

class OverpassJsonParser private constructor(val data: JSONArray) : AbstractParser() {

    companion object {
        fun parse(data: JSONArray) : OverpassJsonParser {
            return OverpassJsonParser(data).apply { parse() }
        }
    }

    private fun parse() {
        for (element in data.toListOfJSONObjects().filterNotNull()) {
            when (element.getString("type")) {
                "node" -> nodes.add(element)
                "way" -> {
                    val way =
                        JSONObject(element.toString())
                    ways.add(way)
                    if (way.has("nodes")) {
                        way.put("nodes",
                            JSONArray(
                                way.getJSONArray("nodes").toString()
                            )
                        )
                    }/* else {
                        way.put("nodes", JSONObject.NULL)
                    }*/
                    if (element.has("center")) centerGeometry(way)
                    if (element.has("geometry")) fullGeometryWay(way)
                    else if (element.has("bounds")) boundsGeometry(way)
                }

                "relation" -> {
                    val rel =
                        JSONObject(element.toString())
                    if (rel.has("members")) {
                        rel.put("members",
                            JSONArray(
                                rel.getJSONArray("members").toString()
                            )
                        )
                    }
                    rels.add(rel)
                    val hasFullGeometry =
                        element.has("members") && element.getJSONArray("members").toListOfJSONObjects().filterNotNull()
                            .any { member ->
                                member.getString("type") == "node" && member.has("lat") || member.getString("type") == "way" && member.has(
                                    "geometry"
                                ) && member.getJSONArray("geometry").length() > 0
                            }
                    if (element.has("center")) centerGeometry(rel)
                    if (hasFullGeometry) fullGeometryRelation(rel)
                    else if (element.has("bounds")) boundsGeometry(rel)
                }
            }
        }
    }

    private fun centerGeometry(geometry: JSONObject) {
        val pseudoNode = JSONObject(geometry.toString())
        pseudoNode.put("lat", geometry.getJSONObject("center").getNumber("lat"))
        pseudoNode.put("lon", geometry.getJSONObject("center").getNumber("lon"))
        pseudoNode.put("__is_center_placeholder", true)
        nodes.add(pseudoNode)
    }

    private fun boundsGeometry(geometry: JSONObject) {
        val pseudoWay = JSONObject(geometry.toString())
        pseudoWay.put("nodes", JSONArray())
        addPseudoNode(
            pseudoWay,
            geometry.getJSONObject("bounds").getNumber("minlat"),
            geometry.getJSONObject("bounds").getNumber("minlon"),
            1
        )
        addPseudoNode(
            pseudoWay,
            geometry.getJSONObject("bounds").getNumber("maxlat"),
            geometry.getJSONObject("bounds").getNumber("minlon"),
            2
        )
        addPseudoNode(
            pseudoWay,
            geometry.getJSONObject("bounds").getNumber("maxlat"),
            geometry.getJSONObject("bounds").getNumber("maxlon"),
            3
        )
        addPseudoNode(
            pseudoWay,
            geometry.getJSONObject("bounds").getNumber("minlat"),
            geometry.getJSONObject("bounds").getNumber("maxlon"),
            4
        )
        val pseudoWayNodes = pseudoWay.getJSONArray("nodes")
        pseudoWayNodes.put(pseudoWayNodes.first())
        pseudoWay.put("nodes", pseudoWayNodes)
        pseudoWay.put("__is_bounds_placeholder", true)
        ways.add(pseudoWay)
    }

    private fun addPseudoNode(pseudoWay: JSONObject, lat: Number, lon: Number, i: Int) {
        val pseudoNode = JSONObject().apply {
            put("type", "node")
            put("id", "_way/${pseudoWay.get("id")}bounds$i")
            put("lat", lat)
            put("lon", lon)
        }
        val pseudoWayNodes: JSONArray? = pseudoWay.optJSONArray("nodes")
        pseudoWayNodes?.put(pseudoNode.get("id"))
        pseudoWay.put("nodes", pseudoWayNodes)
        nodes.add(pseudoNode)
    }

    private fun fullGeometryWay(way: JSONObject) {
        if (way.optJSONArray("nodes") == null) {
            way.put("nodes", JSONArray().apply {
                putAll(way.getJSONArray("geometry").toListOfJSONObjects().map { node ->
                    if (node != null)
                        "_anonymous@${node.getNumber("lat")}/${node.getNumber("lon")}"
                    else
                        "_anonymous@unknown_location"
                })
            })
        }
        way.getJSONArray("geometry").toListOfJSONObjects().forEachIndexed { index, node ->
            if (node == null) return@forEachIndexed
            addFullGeometryNode(
                node.getNumber("lat"),
                node.getNumber("lon"),
                way.getJSONArray("nodes").get(index)
            )
        }
    }

    private fun addFullGeometryNode(lat: Number, lon: Number, id: Any) {
        val geometryNode = JSONObject().apply {
            put("type", "node")
            put("id", id)
            put("lat", lat)
            put("lon", lon)
        }
        nodes.add(geometryNode)
    }

    private fun addFullGeometryWay(
        geometry: JSONArray,
        id: Any
    ) {
        // shared multipolygon ways cannot be defined multiple times with the same id.
        if (ways.any { // todo: this is slow :(
                it.optString("type") == "type" && it.opt("id") == id
            }) return

        val geometryWay = JSONObject().apply {
            put("type", "way")
            put("id", id)
        }
        val geometryWayNodes = JSONArray()
        geometry.toListOfJSONObjects().forEach { node ->
            if (node != null)
                addFullGeometryWayPseudoNode(geometryWayNodes, node.getNumber("lat"), node.getNumber("lon"))
            else geometryWayNodes.put(JSONObject.NULL)
        }
        geometryWay.put("nodes", geometryWayNodes)
        ways.add(geometryWay)
    }

    private fun addFullGeometryWayPseudoNode(
        geometryWayNodes: JSONArray,
        lat: Number,
        lon: Number
    ) {
        // todo? do not save the same pseudo node multiple times
        val geometryPseudoNode = JSONObject().apply {
            put("type", "node")
            put("id", "_anonymous@$lat/$lon")
            put("lat", lat)
            put("lon", lon)
        }
        geometryWayNodes.put(geometryPseudoNode.get("id"))
        nodes.add(geometryPseudoNode)
    }

    private fun fullGeometryRelation(rel: JSONObject) {
        rel.getJSONArray("members").toListOfJSONObjects().forEach { member ->
            if (member == null) return
            if (member.optString("type") == "node") {
                if (member.has("lat")) {
                    addFullGeometryNode(
                        member.getNumber("lat"),
                        member.getNumber("lon"),
                        member["ref"]
                    )
                }
            } else if (member.optString("type") == "way") {
                val geometry = member.opt("geometry")
                if (geometry != null && geometry != JSONObject.NULL) {
                    member.put("ref", "_fullGeom${member["ref"]}")
                    addFullGeometryWay(member.getJSONArray("geometry"), member["ref"])
                }
            }
        }
    }

}