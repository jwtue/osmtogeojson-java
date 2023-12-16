package de.jonaswolf.osmtogeojson.options

import de.jonaswolf.osmtogeojson.toListOfJSONObjects
import org.json.JSONArray
import org.json.JSONObject

object OsmPolygonFeatures {

    private val src = """
        [
            {
                "key": "building",
                "polygon": "all"
            },
            {
                "key": "highway",
                "polygon": "whitelist",
                "values": [
                    "services",
                    "rest_area",
                    "escape",
                    "elevator"
                ]
            },
            {
                "key": "natural",
                "polygon": "blacklist",
                "values": [
                    "coastline",
                    "cliff",
                    "ridge",
                    "arete",
                    "tree_row"
                ]
            },
            {
                "key": "landuse",
                "polygon": "all"
            },
            {
                "key": "waterway",
                "polygon": "whitelist",
                "values": [
                    "riverbank",
                    "dock",
                    "boatyard",
                    "dam"
                ]
            },
            {
                "key": "amenity",
                "polygon": "all"
            },
            {
                "key": "leisure",
                "polygon": "all"
            },
            {
                "key": "barrier",
                "polygon": "whitelist",
                "values": [
                    "city_wall",
                    "ditch",
                    "hedge",
                    "retaining_wall",
                    "wall",
                    "spikes"
                ]
            },
            {
                "key": "railway",
                "polygon": "whitelist",
                "values": [
                    "station",
                    "turntable",
                    "roundhouse",
                    "platform"
                ]
            },
            {
                "key": "area",
                "polygon": "all"
            },
            {
                "key": "boundary",
                "polygon": "all"
            },
            {
                "key": "man_made",
                "polygon": "blacklist",
                "values": [
                    "cutline",
                    "embankment",
                    "pipeline"
                ]
            },
            {
                "key": "power",
                "polygon": "whitelist",
                "values": [
                    "plant",
                    "substation",
                    "generator",
                    "transformer"
                ]
            },
            {
                "key": "place",
                "polygon": "all"
            },
            {
                "key": "shop",
                "polygon": "all"
            },
            {
                "key": "aeroway",
                "polygon": "blacklist",
                "values": [
                    "taxiway"
                ]
            },
            {
                "key": "tourism",
                "polygon": "all"
            },
            {
                "key": "historic",
                "polygon": "all"
            },
            {
                "key": "public_transport",
                "polygon": "all"
            },
            {
                "key": "office",
                "polygon": "all"
            },
            {
                "key": "building:part",
                "polygon": "all"
            },
            {
                "key": "military",
                "polygon": "all"
            },
            {
                "key": "ruins",
                "polygon": "all"
            },
            {
                "key": "area:highway",
                "polygon": "all"
            },
            {
                "key": "craft",
                "polygon": "all"
            },
            {
                "key": "golf",
                "polygon": "all"
            },
            {
                "key": "indoor",
                "polygon": "all"
            }
        ]
        """".trimIndent()

    val polygonFeatures = JSONObject().apply {
        JSONArray(src)
            .toListOfJSONObjects().filterNotNull().forEach { tags ->
            if (tags["polygon"] == "all") {
                put(tags["key"].toString(), true)
            } else {
                val list = if (tags["polygon"] == "whitelist") "included_values" else "excluded_values"
                val tagValuesObj = JSONObject()
                tags.getJSONArray("values").forEach { value ->
                    tagValuesObj.put(value.toString(), true)
                }
                put(tags["key"].toString(), JSONObject(
                    mapOf(
                        list to tagValuesObj
                    )
                )
                )
            }
        }
    }
}