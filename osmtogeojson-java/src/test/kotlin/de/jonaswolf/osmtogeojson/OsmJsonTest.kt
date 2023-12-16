package de.jonaswolf.osmtogeojson

import org.json.JSONArray
import org.json.JSONObject
import kotlin.test.Test
import kotlin.test.assertEquals

class OsmJsonTest : AbstractTest() {

    override val defaultConverter = OsmToGeoJson()

    @Test
    fun node() {
        compareJson(loadJsonFiles("osm_json/node"))
    }

    @Test
    fun way() {
        compareJson(loadJsonFiles("osm_json/way"))
    }

    @Test
    fun polygon() {
        compareJson(loadJsonFiles("osm_json/polygon"))
    }

    @Test
    fun simpleMultipolygon() {
        compareJson(loadJsonFiles("osm_json/simple_multipolygon"))
        compareJson(loadJsonFiles("osm_json/simple_multipolygon_invalid"))
    }

    @Test
    fun multipolygon() {
        val (json, geoJson) = loadJsonFiles("osm_json/multipolygon")
        compareJson(json, geoJson)
        // handle role-less members as outer ways
        json.put("elements", json.getJSONArray("elements").apply {
            put(0, getJSONObject(0).apply {
                put("members", getJSONArray("members").apply {
                    put(3, getJSONObject(3).apply {
                        put("role", "")
                    })
                })
            })
        })
        geoJson.put("features", geoJson.getJSONArray("features").apply {
            put(2, getJSONObject(2).apply {
                put("properties", getJSONObject("properties").apply {
                    put("relations", getJSONArray("relations").apply {
                        put(0, getJSONObject(0).apply {
                            put("role", "")
                        })
                    })
                })
            })
        })
        compareJson(json, geoJson)
    }

    @Test
    fun routeRelation() {
        val (json, geoJson) = loadJsonFiles("osm_json/route")
        val result = JSONObject(
            defaultConverter.convertOverpassJsonToGeoJson(json.toString())
        )
        val sorter = fun(a: JSONArray, b: JSONArray): Int { return a.length() - b.length() }
        result.put("features", result.getJSONArray("features").apply {
            put(0, getJSONObject(0).apply {
                put("geometry", getJSONObject("geometry").apply {
                    put(
                        "coordinates",
                        JSONArray(
                            getJSONArray("coordinates").toListOfJSONArrays().filterNotNull().sortedWith(sorter)
                        )
                    )
                })
            })
        })
        geoJson.put("features", geoJson.getJSONArray("features").apply {
            put(0, getJSONObject(0).apply {
                put("geometry", getJSONObject("geometry").apply {
                    put(
                        "coordinates",
                        JSONArray(
                            getJSONArray("coordinates").toListOfJSONArrays().filterNotNull().sortedWith(sorter)
                        )
                    )
                })
            })
        })
        assertEquals(geoJson.toString(INDENT_FACTOR), result.toString(INDENT_FACTOR))
    }

    @Test
    fun tagsWaysNodesPois() {
        compareJson(loadJsonFiles("osm_json/tags_ways_nodes_pois"))
    }

    @Test
    fun oneNodeWays() {
        checkEmptyJson(JSONObject(loadFile("osm_json/one_node_ways.json")))
    }

    @Test
    fun invalidMultipolygonEmpty() {
        checkEmptyJson(JSONObject(loadFile("osm_json/invalid_multipolygon_empty.json")))
    }

    @Test
    fun invalidMultipolygonMissingMembers() {
        checkEmptyJson(JSONObject(loadFile("osm_json/invalid_multipolygon_missing_members.json")))
    }

    @Test
    fun invalidMultipolygonEmptyMembers() {
        checkEmptyJson(JSONObject(loadFile("osm_json/invalid_multipolygon_empty_members.json")))
    }

    @Test
    fun invalidRouteEmpty() {
        checkEmptyJson(JSONObject(loadFile("osm_json/invalid_route_empty.json")))
    }

    @Test
    fun invalidRouteMissingMembers() {
        checkEmptyJson(JSONObject(loadFile("osm_json/invalid_route_missing_members_test.json")))
    }

    @Test
    fun invalidRouteEmptyMembers() {
        checkEmptyJson(JSONObject(loadFile("osm_json/invalid_route_empty_members.json")))
    }

    @Test
    fun relationsIdSpaces() {
        compareJson(loadJsonFiles("osm_json/relations_id_spaces"))
    }

    @Test
    fun metaData() {
        // nodes
        compareJson(loadJsonFiles("osm_json/meta_data_node"))

        // ways and rels
        val json =
            JSONObject(loadFile("osm_json/meta_data_ways_rels.json"))
        val result = JSONObject(
            defaultConverter.convertOverpassJsonToGeoJson(json.toString())
        )
        assertEquals(4, result.getJSONArray("features").length())
        assert(
            result.getJSONArray("features").getJSONObject(0).getJSONObject("properties").getJSONObject("meta")
                .has("user")
        )
        assert(
            result.getJSONArray("features").getJSONObject(1).getJSONObject("properties").getJSONObject("meta")
                .has("user")
        )
        assert(
            result.getJSONArray("features").getJSONObject(2).getJSONObject("properties").getJSONObject("meta")
                .has("user")
        )
        assert(
            result.getJSONArray("features").getJSONObject(3).getJSONObject("properties").getJSONObject("meta")
                .has("user")
        )
    }


    @Test
    // multipolygon detection corner case
    // see https://github.com/tyrasd/osmtogeojson/issues/7
    fun multipolygonOuterWayTagging() {
        val json =
            JSONObject(loadFile("osm_json/multipolygon_outer_way_tagging.json"))
        val result = JSONObject(
            defaultConverter.convertOverpassJsonToGeoJson(json.toString())
        )
        assertEquals(2, result.getJSONArray("features").length())
        assertEquals(
            "1",
            result.getJSONArray("features").getJSONObject(0).getJSONObject("properties").get("id").toString()
        )
        assertEquals(
            "2",
            result.getJSONArray("features").getJSONObject(1).getJSONObject("properties").get("id").toString()
        )
    }

    @Test
    // non-matching inner and outer rings
    fun multipolygonNonMatchingInnerOuterRings() {
        // complex polygon
        var json =
            JSONObject(loadFile("osm_json/multipolygon_non_matching_inner_outer_rings_complex.json"))
        var result = JSONObject(
            defaultConverter.convertOverpassJsonToGeoJson(json.toString())
        )
        assertEquals(1, result.getJSONArray("features").length())
        assertEquals(
            "1",
            result.getJSONArray("features").getJSONObject(0).getJSONObject("properties").get("id").toString()
        )
        assertEquals(
            "Polygon",
            result.getJSONArray("features").getJSONObject(0).getJSONObject("geometry").get("type")
                .toString()
        )
        assertEquals(
            1,
            result.getJSONArray("features").getJSONObject(0).getJSONObject("geometry").getJSONArray("coordinates")
                .length()
        )

        // simple polygon
        json =
            JSONObject(loadFile("osm_json/multipolygon_non_matching_inner_outer_rings_simple.json"))
        result = JSONObject(
            defaultConverter.convertOverpassJsonToGeoJson(json.toString())
        )
        assertEquals(1, result.getJSONArray("features").length())
        assertEquals(
            "2",
            result.getJSONArray("features").getJSONObject(0).getJSONObject("properties").get("id").toString()
        )
        assertEquals(
            "Polygon",
            result.getJSONArray("features").getJSONObject(0).getJSONObject("geometry").get("type").toString()
        )
        assertEquals(
            1,
            result.getJSONArray("features").getJSONObject(0).getJSONObject("geometry").getJSONArray("coordinates")
                .length()
        )
    }

    @Test
    fun multipolygonNonTrivialRingBuilding() {
        // way order
        var json =
            JSONObject(loadFile("osm_json/multipolygon_non_trivial_ring_building_way_order.json"))
        var result = JSONObject(
            defaultConverter.convertOverpassJsonToGeoJson(json.toString())
        )
        assertEquals(1, result.getJSONArray("features").length())
        assertEquals(
            "1",
            result.getJSONArray("features").getJSONObject(0).getJSONObject("properties").get("id").toString()
        )
        assertEquals(
            "Polygon",
            result.getJSONArray("features").getJSONObject(0).getJSONObject("geometry").get("type").toString()
        )
        assertEquals(
            1,
            result.getJSONArray("features").getJSONObject(0).getJSONObject("geometry").getJSONArray("coordinates")
                .length()
        )
        assertEquals(
            4,
            result.getJSONArray("features").getJSONObject(0).getJSONObject("geometry").getJSONArray("coordinates")
                .getJSONArray(0).length()
        )

        // way direction
        json =
            JSONObject(loadFile("osm_json/multipolygon_non_trivial_ring_building_way_directions.json"))
        result = JSONObject(
            defaultConverter.convertOverpassJsonToGeoJson(json.toString())
        )
        assertEquals(1, result.getJSONArray("features").length())
        assertEquals(
            "1",
            result.getJSONArray("features").getJSONObject(0).getJSONObject("properties").get("id").toString()
        )
        assertEquals(
            "Polygon",
            result.getJSONArray("features").getJSONObject(0).getJSONObject("geometry").get("type").toString()
        )
        assertEquals(
            1,
            result.getJSONArray("features").getJSONObject(0).getJSONObject("geometry").getJSONArray("coordinates")
                .length()
        )
        assertEquals(
            7,
            result.getJSONArray("features").getJSONObject(0).getJSONObject("geometry").getJSONArray("coordinates")
                .getJSONArray(0).length()
        )
    }

    @Test
    fun multipolygonUnclosedRing() {
        // unclosed ring, non-matching ways
        var json =
            JSONObject(loadFile("osm_json/multipolygon_unclosed_ring_nonmatching_ways.json"))
        var result = JSONObject(
            defaultConverter.convertOverpassJsonToGeoJson(json.toString())
        )
        assertEquals(1, result.getJSONArray("features").length())
        assertEquals(
            "1",
            result.getJSONArray("features").getJSONObject(0).getJSONObject("properties").get("id").toString()
        )
        assertEquals(
            "Polygon",
            result.getJSONArray("features").getJSONObject(0).getJSONObject("geometry").get("type").toString()
        )
        assertEquals(
            1,
            result.getJSONArray("features").getJSONObject(0).getJSONObject("geometry").getJSONArray("coordinates")
                .length()
        )
        assertEquals(
            4,
            result.getJSONArray("features").getJSONObject(0).getJSONObject("geometry").getJSONArray("coordinates")
                .getJSONArray(0).length()
        )
        assert(result.getJSONArray("features").getJSONObject(0).getJSONObject("properties").opt("tainted") != true)

        // unclosed ring, matching ways
        json =
            JSONObject(loadFile("osm_json/multipolygon_unclosed_ring_matching_ways.json"))
        result = JSONObject(
            defaultConverter.convertOverpassJsonToGeoJson(json.toString())
        )
        assertEquals(1, result.getJSONArray("features").length())
        assertEquals(
            "1",
            result.getJSONArray("features").getJSONObject(0).getJSONObject("properties").get("id").toString()
        )
        assertEquals(
            "Polygon",
            result.getJSONArray("features").getJSONObject(0).getJSONObject("geometry").get("type").toString()
        )
        assertEquals(
            1,
            result.getJSONArray("features").getJSONObject(0).getJSONObject("geometry").getJSONArray("coordinates")
                .length()
        )
        assertEquals(
            4,
            result.getJSONArray("features").getJSONObject(0).getJSONObject("geometry").getJSONArray("coordinates")
                .getJSONArray(0).length()
        )
        assert(result.getJSONArray("features").getJSONObject(0).getJSONObject("properties").opt("tainted") != true)
    }

    @Test
    fun overpassArea() {
        checkEmptyJson(JSONObject(loadFile("osm_json/area.json")))
    }
}