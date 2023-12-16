package de.jonaswolf.osmtogeojson

import org.json.JSONObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TaintedDataTest : AbstractTest() {

    override val defaultConverter = OsmToGeoJson()

    @Test
    fun taintedGeometries() {
        compareJson(loadJsonFiles("tainted_data/tainted_geometries"))
    }

    @Test
    fun idsOnly() {
        checkEmptyJson(JSONObject(loadFile("tainted_data/ids_only_node.json")))
        checkEmptyJson(JSONObject(loadFile("tainted_data/ids_only_way.json")))
        checkEmptyJson(JSONObject(loadFile("tainted_data/ids_only_relation.json")))
    }

    @Test
    fun taintedWay() {
        var json = loadFile("tainted_data/tainted_way.json")
        var result = JSONObject(
            defaultConverter.convertOverpassJsonToGeoJson(json)
        )
        assertEquals(1, result.getJSONArray("features").length())
        assertEquals(1, result.getJSONArray("features").getJSONObject(0).getJSONObject("properties").opt("id"))
        assertEquals("[[0,0],[1,1]]", result.getJSONArray("features").getJSONObject(0).getJSONObject("geometry").opt("coordinates").toString())
        assertTrue(result.getJSONArray("features").getJSONObject(0).getJSONObject("properties").optBoolean("tainted"))
    }

    @Test
    fun emptyMultipolygon() {

    }

    @Test
    fun taintedSimpleMultipolygon() {
        var json = loadFile("tainted_data/tainted_simple_multipolygon_missing_way.json")
        var result = JSONObject(
            defaultConverter.convertOverpassJsonToGeoJson(json)
        )
        assertEquals(1, result.getJSONArray("features").length())
        assertEquals(2, result.getJSONArray("features").getJSONObject(0).getJSONObject("properties").opt("id"))
        assertTrue(result.getJSONArray("features").getJSONObject(0).getJSONObject("properties").optBoolean("tainted"))

        json = loadFile("tainted_data/tainted_simple_multipolygon_missing_nodes.json")
        result = JSONObject(
            defaultConverter.convertOverpassJsonToGeoJson(json)
        )
        assertEquals(0, result.getJSONArray("features").length())

        json = loadFile("tainted_data/tainted_simple_multipolygon_missing_node.json")
        result = JSONObject(
            defaultConverter.convertOverpassJsonToGeoJson(json)
        )
        assertEquals(1, result.getJSONArray("features").length())
        assertEquals(2, result.getJSONArray("features").getJSONObject(0).getJSONObject("properties").opt("id"))
        assertTrue(result.getJSONArray("features").getJSONObject(0).getJSONObject("properties").optBoolean("tainted"))
    }

    @Test
    fun taintedMultipolygon() {
        var json = loadFile("tainted_data/tainted_multipolygon_missing_way.json")
        var result = JSONObject(
            defaultConverter.convertOverpassJsonToGeoJson(json)
        )
        assertEquals(1, result.getJSONArray("features").length())
        assertEquals(1, result.getJSONArray("features").getJSONObject(0).getJSONObject("properties").opt("id"))
        assertTrue(result.getJSONArray("features").getJSONObject(0).getJSONObject("properties").optBoolean("tainted"))

        json = loadFile("tainted_data/tainted_multipolygon_missing_node.json")
        result = JSONObject(
            defaultConverter.convertOverpassJsonToGeoJson(json)
        )
        assertEquals(1, result.getJSONArray("features").length())
        assertEquals(1, result.getJSONArray("features").getJSONObject(0).getJSONObject("properties").opt("id"))
        assertTrue(result.getJSONArray("features").getJSONObject(0).getJSONObject("properties").optBoolean("tainted"))
    }

    @Test
    fun degenerateMultipolygon() {
        checkEmptyJson(JSONObject(loadFile("tainted_data/degenerate_multipolygon_no_coordinates.json")))
        checkEmptyJson(JSONObject(loadFile("tainted_data/degenerate_multipolygon_no_outer_ring.json")))
        // expected behaviour: do not return a degenerate (multi)polygon.
        // this could in principle return just the way that is now sort of unused
        // but just as with an (untagged) node of a one-node-way we're going to
        // assume that those outlines aren't interesting enough.
        checkEmptyJson(JSONObject(loadFile("tainted_data/degenerate_multipolygon_incomplete_outer_ring.json")))
    }
}