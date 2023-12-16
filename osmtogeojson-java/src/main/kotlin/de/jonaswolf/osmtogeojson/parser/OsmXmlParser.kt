package de.jonaswolf.osmtogeojson.parser

import org.json.JSONArray
import org.json.JSONObject
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList

class OsmXmlParser(val doc: Document) : AbstractParser() {

    companion object {
        fun parse(doc: Document): OsmXmlParser {
            return OsmXmlParser(doc).apply { parse() }
        }
    }

    private fun parse() {

        doc.getElementsByTagName("node").toListOfElements().forEach { node ->
            val tags = mutableMapOf<String, Any>()
            node.getElementsByTagName("tag").toListOfElements().forEach { tag ->
                tags[tag.getAttribute("k")] = tag.getAttribute("v")
            }
            nodes.add(JSONObject().apply {
                put("type", "node")
                copyAttribute(node, this, "id")
                copyAttribute(node, this, "lat")
                copyAttribute(node, this, "lon")
                copyAttribute(node, this, "version")
                copyAttribute(node, this, "timestamp")
                copyAttribute(node, this, "changeset")
                copyAttribute(node, this, "uid")
                copyAttribute(node, this, "user")
                if (tags.isNotEmpty()) {
                    put("tags", JSONObject(tags))
                }
            })
        }
        doc.getElementsByTagName("way").toListOfElements().forEach { way ->
            val tags = mutableMapOf<String, Any>()
            way.getElementsByTagName("tag").toListOfElements().forEach { tag ->
                tags[tag.getAttribute("k")] = tag.getAttribute("v")
            }
            val wnodes = mutableMapOf<Int, String>()
            var hasFullGeometry = false
            way.getElementsByTagName("nd").toListOfElements().forEachIndexed { i, nd ->
                val id = nd.getAttribute("ref")
                if (id.isNotEmpty()) {
                    wnodes[i] = id
                }
                if (!hasFullGeometry && nd.hasAttribute("lat")) {
                    hasFullGeometry = true
                }
            }
            ways.add(JSONObject().apply {
                put("type", "way")
                copyAttribute(way, this, "id")
                copyAttribute(way, this, "version")
                copyAttribute(way, this, "timestamp")
                copyAttribute(way, this, "changeset")
                copyAttribute(way, this, "uid")
                copyAttribute(way, this, "user")
                if (wnodes.isNotEmpty()) {
                    put("nodes", JSONArray(wnodes.values))
                }
                if (tags.isNotEmpty()) {
                    put("tags", JSONObject(tags))
                }
                if (way.getElementsByTagName("center").length > 0) {
                    centerGeometry(this, way.getElementsByTagName("center").toListOfElements().first())
                }
                if (hasFullGeometry) {
                    fullGeometryWay(this, way.getElementsByTagName("nd").toListOfElements())
                } else if (way.getElementsByTagName("bounds").length > 0) {
                    boundsGeometry(this, way.getElementsByTagName("bounds").toListOfElements().first())
                }
            })
        }
        doc.getElementsByTagName("relation").toListOfElements().forEach { rel ->
            val tags = mutableMapOf<String, Any>()
            rel.getElementsByTagName("tag").toListOfElements().forEach { tag ->
                tags[tag.getAttribute("k")] = tag.getAttribute("v")
            }
            val members = mutableMapOf<Int, JSONObject>()
            var hasFullGeometry = false
            rel.getElementsByTagName("member").toListOfElements().forEachIndexed { i, member ->
                members[i] = JSONObject().apply {
                    copyAttribute(member, this, "ref")
                    copyAttribute(member, this, "role")
                    copyAttribute(member, this, "type")
                    if (!hasFullGeometry && (opt("type") == "node" && member.hasAttribute("lat")) || (opt("type") == "way" && member.getElementsByTagName(
                            "nd"
                        ).length > 0)
                    ) {
                        hasFullGeometry = true
                    }
                }
            }
            rels.add(JSONObject().apply {
                put("type", "relation")
                copyAttribute(rel, this, "id")
                copyAttribute(rel, this, "version")
                copyAttribute(rel, this, "timestamp")
                copyAttribute(rel, this, "changeset")
                copyAttribute(rel, this, "uid")
                copyAttribute(rel, this, "user")
                if (members.isNotEmpty()) {
                    put("members",
                        JSONArray(members.values)
                    )
                }
                if (tags.isNotEmpty()) {
                    put("tags", JSONObject(tags))
                }
                if (rel.getElementsByTagName("center").length > 0) {
                    centerGeometry(this, rel.getElementsByTagName("center").toListOfElements().first())
                }
                if (hasFullGeometry) {
                    fullGeometryRelation(this, rel.getElementsByTagName("member").toListOfElements())
                } else if (rel.getElementsByTagName("bounds").length > 0) {
                    boundsGeometry(this, rel.getElementsByTagName("bounds").toListOfElements().first())
                }
            })
        }
    }

    private fun copyAttribute(xmlElement: Element, outputObject: JSONObject, attr: String) {
        if (xmlElement.hasAttribute(attr)) {
            outputObject.put(attr, xmlElement.getAttribute(attr))
        }
    }

    private fun centerGeometry(geometry: JSONObject, centroid: Element) {
        val pseudoNode = JSONObject(geometry.toString())
        copyAttribute(centroid, pseudoNode, "lat")
        copyAttribute(centroid, pseudoNode, "lon")
        pseudoNode.put("__is_center_placeholder", true)
        nodes.add(pseudoNode)
    }

    private fun addPseudoNode(pseudoWay: JSONObject, lat: String, lon: String, i: Int) {
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

    private fun boundsGeometry(geometry: JSONObject, bounds: Element) {
        val pseudoWay = JSONObject(geometry.toString())
        pseudoWay.put("nodes", JSONArray())
        addPseudoNode(
            pseudoWay,
            bounds.getAttribute("minlat"),
            bounds.getAttribute("minlon"),
            1
        )
        addPseudoNode(
            pseudoWay,
            bounds.getAttribute("maxlat"),
            bounds.getAttribute("minlon"),
            2
        )
        addPseudoNode(
            pseudoWay,
            bounds.getAttribute("maxlat"),
            bounds.getAttribute("maxlon"),
            3
        )
        addPseudoNode(
            pseudoWay,
            bounds.getAttribute("minlat"),
            bounds.getAttribute("maxlon"),
            4
        )
        val pseudoWayNodes = pseudoWay.getJSONArray("nodes")
        pseudoWayNodes.put(pseudoWayNodes.first())
        pseudoWay.put("nodes", pseudoWayNodes)
        pseudoWay.put("__is_bounds_placeholder", true)
        ways.add(pseudoWay)
    }

    private fun fullGeometryWay(way: JSONObject, nodes: List<Element>) {
        if (way.optJSONArray("nodes") == null) {
            way.put("nodes", JSONArray().apply {
                putAll(nodes.map { nd ->
                    "_anonymous@" + nd.getAttribute("lat") + "/" + nd.getAttribute("lon")
                })
            })
        }
        nodes.forEachIndexed { index, node ->
            if (node.hasAttribute("lat")) {
                addFullGeometryNode(
                    node.getAttribute("lat"),
                    node.getAttribute("lon"),
                    way.getJSONArray("nodes").getString(index)
                )
            }
        }
    }

    private fun addFullGeometryNode(lat: String, lon: String, id: Any) {
        val geometryNode = JSONObject().apply {
            put("type", "node")
            put("id", id)
            put("lat", lat)
            put("lon", lon)
        }
        nodes.add(geometryNode)
    }

    private fun addFullGeometryWay(
        nds: List<Element>,
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
        nds.forEach { nd ->
            if (nd.hasAttribute("lat"))
                addFullGeometryWayPseudoNode(geometryWayNodes, nd.getAttribute("lat"), nd.getAttribute("lon"))
            else geometryWayNodes.put(JSONObject.NULL)
        }
        geometryWay.put("nodes", geometryWayNodes)
        ways.add(geometryWay)
    }

    private fun addFullGeometryWayPseudoNode(
        geometryWayNodes: JSONArray,
        lat: String,
        lon: String
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

    private fun fullGeometryRelation(rel: JSONObject, members: List<Element>) {
        members.forEachIndexed { i, member ->
            if (rel.optJSONArray("members")?.optJSONObject(i)?.opt("type") == "node") {
                if (member.hasAttribute("lat")) {
                    addFullGeometryNode(
                        member.getAttribute("lat"),
                        member.getAttribute("lon"),
                        rel.getJSONArray("members").getJSONObject(i).get("ref")
                    )
                }
            } else if (rel.optJSONArray("members")?.optJSONObject(i)?.opt("type") == "way") {
                if (member.getElementsByTagName("nd").length > 0) {
                    val relMembers = rel.getJSONArray("members")
                    val relMember = relMembers.getJSONObject(i)
                    relMember.put("ref", "_fullGeom"+relMember.opt("ref"))
                    relMembers.put(i, relMember)
                    rel.put("members", relMembers)
                    addFullGeometryWay(
                        member.getElementsByTagName("nd").toListOfElements(),
                        relMember.get("ref")
                    )
                }
            }
        }
    }

    fun NodeList.toList(): List<Node> {
        return (0 until length).map { item(it) }
    }

    fun NodeList.toListOfElements(): List<Element> {
        return toList().mapNotNull { it as? Element }
    }
}