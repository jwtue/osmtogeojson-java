package de.jonaswolf.osmtogeojson

import org.json.JSONObject

private val UNINTERESTING_TAGS = listOf(
    "source",
    "source_ref",
    "source:ref",
    "history",
    "attribution",
    "created_by",
    "tiger:county",
    "tiger:tlid",
    "tiger:upload_uuid"
)

interface OsmChild {
    val type: String
}

data class OsmNode(
    val id: String?,
    val lat: Double,
    val lon: Double,
    val version: Double = 0.0,
    val ref: Int = 0,
    var role: String? = null,
    val isCenterPlaceholder: Boolean = false
) : OsmChild {
    override val type = "node"
    val tags = mutableMapOf<String, String>()

    fun hasInterestingTags(ignoreTags: List<String> = listOf<String>()): Boolean {
        return tags.keys.any { k ->
            !UNINTERESTING_TAGS.contains(k) && !ignoreTags.contains(k)
        }
    }

    companion object {
        fun create(element: JSONObject): OsmNode {
            return OsmNode(
                element.getString("id"),
                element.getDouble("lat"),
                element.getDouble("lon"),
                version = element.getDouble("version"),
                ref = element.getInt("ref"),
                role = element.getString("role")
            )
        }

        fun deduplicate(a: OsmNode, b: OsmNode): OsmNode {
            return if (a.version != b.version) {
                if (a.version > b.version) a else b
            } else {
                OsmNode(
                    a.id,
                    a.lat,
                    a.lon,
                    version = a.version,
                    ref = a.ref,
                    role = a.role,
                    isCenterPlaceholder = a.isCenterPlaceholder
                ).apply {
                    tags.putAll(a.tags)
                    tags.putAll(b.tags)
                }
            }
        }
    }
}

data class OsmWay(
    val id: String?,
    val version: Double = 0.0,
    val ref: Int = 0,
    var role: String? = null,
    val isBoundsPlaceholder: Boolean = false
) : OsmChild {
    override val type = "way"
    val nodes = mutableListOf<String?>()

    companion object {
        fun create(element: JSONObject): OsmWay {
            return OsmWay(
                element.getString("id"),
                version = element.getDouble("version"),
                ref = element.getInt("ref"),
                role = element.getString("role")
            )
        }
        fun deduplicate(a: OsmWay, b: OsmWay): OsmWay {
            return if (a.version != b.version) {
                if (a.version > b.version) a else b
            } else {
                OsmWay(
                    a.id,
                    version = a.version,
                    ref = a.ref,
                    role = a.role,
                    isBoundsPlaceholder = a.isBoundsPlaceholder
                ).apply {
                    nodes.addAll(a.nodes.filterNotNull())
                    nodes.addAll(b.nodes.filterNotNull())
                }
            }
        }
    }
}

data class OsmRelation(
    val id: String,
    val version: Double = 0.0,
    val ref: Int = 0,
    var role: String? = null,
) : OsmChild {
    override val type = "relation"

    val members = mutableListOf<OsmChild>()

    companion object {
        fun create(element: JSONObject): OsmRelation {
            return OsmRelation(
                element.getString("id"),
                version = element.getDouble("version"),
                ref = element.getInt("ref"),
                role = element.getString("role")
            )
        }
        fun deduplicate(a: OsmRelation, b: OsmRelation): OsmRelation {
            return if (a.version != b.version) {
                if (a.version > b.version) a else b
            } else {
                OsmRelation(
                    a.id,
                    version = a.version,
                    ref = a.ref,
                    role = a.role
                ).apply {
                    members.addAll(a.members)
                    members.addAll(b.members)
                }
            }
        }
    }
}
