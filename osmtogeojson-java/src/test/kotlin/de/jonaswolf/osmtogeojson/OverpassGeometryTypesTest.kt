package de.jonaswolf.osmtogeojson

import org.json.JSONObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OverpassGeometryTypesTest : AbstractTest() {
    override val defaultConverter = OsmToGeoJson()

    @Test
    fun centerXml() {
        // a way
        var xml = loadFile("overpass_geometry_types/center_way.xml")
        var result =
            JSONObject(defaultConverter.convertOsmXmlToGeoJson(xml))

        assertEquals(1, result.getJSONArray("features").length())
        assertEquals("way/1", result.getJSONArray("features").getJSONObject(0).get("id"))
        assertEquals("Point", result.getJSONArray("features").getJSONObject(0).getJSONObject("geometry").get("type"))
        assertEquals(
            "[4.321,1.234]",
            result.getJSONArray("features").getJSONObject(0).getJSONObject("geometry").getJSONArray("coordinates")
                .toString()
        )
        assertEquals(
            "center",
            result.getJSONArray("features").getJSONObject(0).getJSONObject("properties").get("geometry")
        )

        // a relation
        xml = loadFile("overpass_geometry_types/center_relation.xml")
        result =
            JSONObject(defaultConverter.convertOsmXmlToGeoJson(xml))

        assertEquals(1, result.getJSONArray("features").length())
        assertEquals("relation/1", result.getJSONArray("features").getJSONObject(0).get("id"))
        assertEquals("Point", result.getJSONArray("features").getJSONObject(0).getJSONObject("geometry").get("type"))
        assertEquals(
            "[4.321,1.234]",
            result.getJSONArray("features").getJSONObject(0).getJSONObject("geometry").getJSONArray("coordinates")
                .toString()
        )
        assertEquals(
            "center",
            result.getJSONArray("features").getJSONObject(0).getJSONObject("properties").get("geometry")
        )
    }

    @Test
    fun centerJson() {
        // a way
        var json = loadFile("overpass_geometry_types/center_way.json")
        var result = JSONObject(
            defaultConverter.convertOverpassJsonToGeoJson(json)
        )

        assertEquals(1, result.getJSONArray("features").length())
        assertEquals("way/1", result.getJSONArray("features").getJSONObject(0).get("id"))
        assertEquals("Point", result.getJSONArray("features").getJSONObject(0).getJSONObject("geometry").get("type"))
        assertEquals(
            "[4.321,1.234]",
            result.getJSONArray("features").getJSONObject(0).getJSONObject("geometry").getJSONArray("coordinates")
                .toString()
        )
        assertEquals(
            "center",
            result.getJSONArray("features").getJSONObject(0).getJSONObject("properties").get("geometry")
        )

        // a relation
        json = loadFile("overpass_geometry_types/center_relation.json")
        result = JSONObject(
            defaultConverter.convertOverpassJsonToGeoJson(json)
        )

        assertEquals(1, result.getJSONArray("features").length())
        assertEquals("relation/1", result.getJSONArray("features").getJSONObject(0).get("id"))
        assertEquals("Point", result.getJSONArray("features").getJSONObject(0).getJSONObject("geometry").get("type"))
        assertEquals(
            "[4.321,1.234]",
            result.getJSONArray("features").getJSONObject(0).getJSONObject("geometry").getJSONArray("coordinates")
                .toString()
        )
        assertEquals(
            "center",
            result.getJSONArray("features").getJSONObject(0).getJSONObject("properties").get("geometry")
        )
    }

    @Test
    fun boundsXml() {
        // a way
        var xml = loadFile("overpass_geometry_types/bounds_way.xml")
        var result =
            JSONObject(defaultConverter.convertOsmXmlToGeoJson(xml))

        assertEquals(1, result.getJSONArray("features").length())
        assertEquals("way/1", result.getJSONArray("features").getJSONObject(0).get("id"))
        assertEquals("Polygon", result.getJSONArray("features").getJSONObject(0).getJSONObject("geometry").get("type"))
        assertEquals(
            "bounds",
            result.getJSONArray("features").getJSONObject(0).getJSONObject("properties").get("geometry")
        )

        // a relation
        xml = loadFile("overpass_geometry_types/bounds_relation.xml")
        result =
            JSONObject(defaultConverter.convertOsmXmlToGeoJson(xml))

        assertEquals(1, result.getJSONArray("features").length())
        assertEquals("relation/1", result.getJSONArray("features").getJSONObject(0).get("id"))
        assertEquals("Polygon", result.getJSONArray("features").getJSONObject(0).getJSONObject("geometry").get("type"))
        assertEquals(
            "bounds",
            result.getJSONArray("features").getJSONObject(0).getJSONObject("properties").get("geometry")
        )
    }

    @Test
    fun boundsJson() {
        // a way
        var json = loadFile("overpass_geometry_types/bounds_way.json")
        var result = JSONObject(
            defaultConverter.convertOverpassJsonToGeoJson(json)
        )

        assertEquals(1, result.getJSONArray("features").length())
        assertEquals("way/1", result.getJSONArray("features").getJSONObject(0).get("id"))
        assertEquals("Polygon", result.getJSONArray("features").getJSONObject(0).getJSONObject("geometry").get("type"))
        assertEquals(
            "bounds",
            result.getJSONArray("features").getJSONObject(0).getJSONObject("properties").get("geometry")
        )

        // a relation
        json = loadFile("overpass_geometry_types/bounds_relation.json")
        result = JSONObject(
            defaultConverter.convertOverpassJsonToGeoJson(json)
        )

        assertEquals(1, result.getJSONArray("features").length())
        assertEquals("relation/1", result.getJSONArray("features").getJSONObject(0).get("id"))
        assertEquals("Polygon", result.getJSONArray("features").getJSONObject(0).getJSONObject("geometry").get("type"))
        assertEquals(
            "bounds",
            result.getJSONArray("features").getJSONObject(0).getJSONObject("properties").get("geometry")
        )
    }

    @Test
    fun fullXml() {
        // a way
        var xml = loadFile("overpass_geometry_types/full_way.xml")
        var result =
            JSONObject(defaultConverter.convertOsmXmlToGeoJson(xml))

        assertEquals(1, result.getJSONArray("features").length())
        assertEquals("way/1", result.getJSONArray("features").getJSONObject(0).get("id"))
        assertEquals("Polygon", result.getJSONArray("features").getJSONObject(0).getJSONObject("geometry").get("type"))
        assertEquals(
            4,
            result.getJSONArray("features").getJSONObject(0).getJSONObject("geometry").getJSONArray("coordinates")
                .getJSONArray(0).length()
        )

        // a way (ref-less nodes)
        xml = loadFile("overpass_geometry_types/full_way_refless_nodes.xml")
        result =
            JSONObject(defaultConverter.convertOsmXmlToGeoJson(xml))

        assertEquals(1, result.getJSONArray("features").length())
        assertEquals("way/1", result.getJSONArray("features").getJSONObject(0).get("id"))
        assertEquals("Polygon", result.getJSONArray("features").getJSONObject(0).getJSONObject("geometry").get("type"))
        assertEquals(
            4,
            result.getJSONArray("features").getJSONObject(0).getJSONObject("geometry").getJSONArray("coordinates")
                .getJSONArray(0).length()
        )
        assertEquals(
            1,
            result.getJSONArray("features").getJSONObject(0).getJSONObject("geometry").getJSONArray("coordinates")
                .getJSONArray(0).getJSONArray(2).get(0)
        )

        // a relation
        xml = loadFile("overpass_geometry_types/full_relation.xml")
        result =
            JSONObject(defaultConverter.convertOsmXmlToGeoJson(xml))

        assertEquals(2, result.getJSONArray("features").length())
        assertEquals("relation/1", result.getJSONArray("features").getJSONObject(0).get("id"))
        assertEquals(
            "MultiPolygon",
            result.getJSONArray("features").getJSONObject(0).getJSONObject("geometry").get("type")
        )
        assertEquals(
            2,
            result.getJSONArray("features").getJSONObject(0).getJSONObject("geometry").getJSONArray("coordinates")
                .length()
        )
        assertEquals(
            9,
            result.getJSONArray("features").getJSONObject(0).getJSONObject("geometry").getJSONArray("coordinates")
                .getJSONArray(0).getJSONArray(0).length() + result.getJSONArray("features").getJSONObject(0)
                .getJSONObject("geometry").getJSONArray("coordinates").getJSONArray(1).getJSONArray(0).length()
        )

        assertEquals(false, result.getJSONArray("features").getJSONObject(0).getJSONObject("properties").optBoolean("tainted", false))
        assertEquals("node/1", result.getJSONArray("features").getJSONObject(1).get("id"))
        assertEquals(
            0.5,
            result.getJSONArray("features").getJSONObject(1).getJSONObject("geometry").getJSONArray("coordinates").getDouble(0)
        )

        // two more complex relations
        xml = loadFile("overpass_geometry_types/full_two_complex_relations.xml")
        result =
            JSONObject(defaultConverter.convertOsmXmlToGeoJson(xml))
        assertEquals(2, result.getJSONArray("features").length())
        assertEquals(
            "Polygon",
            result.getJSONArray("features").getJSONObject(0).getJSONObject("geometry").get("type")
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
        assertEquals(
            "Polygon",
            result.getJSONArray("features").getJSONObject(1).getJSONObject("geometry").get("type")
        )
        assertEquals(
            1,
            result.getJSONArray("features").getJSONObject(1).getJSONObject("geometry").getJSONArray("coordinates")
                .length()
        )
        assertEquals(
            4,
            result.getJSONArray("features").getJSONObject(1).getJSONObject("geometry").getJSONArray("coordinates")
                .getJSONArray(0).length()
        )
    }

    @Test
    fun fullJson() {
        // a way
        var json = loadFile("overpass_geometry_types/full_way.json")
        var result = JSONObject(
            defaultConverter.convertOverpassJsonToGeoJson(json)
        )

        assertEquals(1, result.getJSONArray("features").length())
        assertEquals("way/1", result.getJSONArray("features").getJSONObject(0).get("id"))
        assertEquals("Polygon", result.getJSONArray("features").getJSONObject(0).getJSONObject("geometry").get("type"))
        assertEquals(
            4,
            result.getJSONArray("features").getJSONObject(0).getJSONObject("geometry").getJSONArray("coordinates")
                .getJSONArray(0).length()
        )

        // a way (ref-less nodes)
        json = loadFile("overpass_geometry_types/full_way_refless_nodes.json")
        result = JSONObject(
            defaultConverter.convertOverpassJsonToGeoJson(json)
        )

        assertEquals(1, result.getJSONArray("features").length())
        assertEquals("way/1", result.getJSONArray("features").getJSONObject(0).get("id"))
        assertEquals("Polygon", result.getJSONArray("features").getJSONObject(0).getJSONObject("geometry").get("type"))
        assertEquals(
            4,
            result.getJSONArray("features").getJSONObject(0).getJSONObject("geometry").getJSONArray("coordinates")
                .getJSONArray(0).length()
        )
        assertEquals(
            1,
            result.getJSONArray("features").getJSONObject(0).getJSONObject("geometry").getJSONArray("coordinates")
                .getJSONArray(0).getJSONArray(2).get(0)
        )

        // a relation
        json = loadFile("overpass_geometry_types/full_relation.json")
        result = JSONObject(
            defaultConverter.convertOverpassJsonToGeoJson(json)
        )

        assertEquals(2, result.getJSONArray("features").length())
        assertEquals("relation/1", result.getJSONArray("features").getJSONObject(0).get("id"))
        assertEquals(
            "MultiPolygon",
            result.getJSONArray("features").getJSONObject(0).getJSONObject("geometry").get("type")
        )
        assertEquals(
            2,
            result.getJSONArray("features").getJSONObject(0).getJSONObject("geometry").getJSONArray("coordinates")
                .length()
        )
        assertEquals(
            9,
            result.getJSONArray("features").getJSONObject(0).getJSONObject("geometry").getJSONArray("coordinates")
                .getJSONArray(0).getJSONArray(0).length() + result.getJSONArray("features").getJSONObject(0)
                .getJSONObject("geometry").getJSONArray("coordinates").getJSONArray(1).getJSONArray(0).length()
        )

        assertEquals(false, result.getJSONArray("features").getJSONObject(0).getJSONObject("properties").optBoolean("tainted", false))
        assertEquals("node/1", result.getJSONArray("features").getJSONObject(1).get("id"))
        assertEquals(
            0.5,
            result.getJSONArray("features").getJSONObject(1).getJSONObject("geometry").getJSONArray("coordinates").getDouble(0)
        )

        // two more complex relations
        json = loadFile("overpass_geometry_types/full_two_complex_relations.json")
        result = JSONObject(
            defaultConverter.convertOverpassJsonToGeoJson(json)
        )
        assertEquals(2, result.getJSONArray("features").length())
        assertEquals(
            "Polygon",
            result.getJSONArray("features").getJSONObject(0).getJSONObject("geometry").get("type")
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
        assertEquals(
            "Polygon",
            result.getJSONArray("features").getJSONObject(1).getJSONObject("geometry").get("type")
        )
        assertEquals(
            1,
            result.getJSONArray("features").getJSONObject(1).getJSONObject("geometry").getJSONArray("coordinates")
                .length()
        )
        assertEquals(
            4,
            result.getJSONArray("features").getJSONObject(1).getJSONObject("geometry").getJSONArray("coordinates")
                .getJSONArray(0).length()
        )
    }

    @Test
    fun fullMixedContentXml() {
        val xml = loadFile("overpass_geometry_types/full_mixed.xml")
        val result =
            JSONObject(defaultConverter.convertOsmXmlToGeoJson(xml))
        // do not include full geometry nd's as node in output
        assertEquals(2, result.getJSONArray("features").length())
        assertEquals("way/1", result.getJSONArray("features").getJSONObject(0).get("id"))
        assertEquals("node/2", result.getJSONArray("features").getJSONObject(1).get("id"))
        assertEquals("bar", result.getJSONArray("features").getJSONObject(1).getJSONObject("properties").getJSONObject("tags").get("foo"))
    }

    @Test
    fun fullMixedContentJson() {
        val json = loadFile("overpass_geometry_types/full_mixed.json")
        val result = JSONObject(
            defaultConverter.convertOverpassJsonToGeoJson(json)
        )
        // do not include full geometry nd's as node in output
        assertEquals(2, result.getJSONArray("features").length())
        assertEquals("way/1", result.getJSONArray("features").getJSONObject(0).get("id"))
        assertEquals("node/2", result.getJSONArray("features").getJSONObject(1).get("id"))
        assertEquals("bar", result.getJSONArray("features").getJSONObject(1).getJSONObject("properties").getJSONObject("tags").get("foo"))
    }

    @Test
    fun fullTaintedXml() {
        // a way
        var xml = loadFile("overpass_geometry_types/full_tainted_way.xml")
        var result =
            JSONObject(defaultConverter.convertOsmXmlToGeoJson(xml))

        assertEquals(1, result.getJSONArray("features").length())
        assertEquals("way/1", result.getJSONArray("features").getJSONObject(0).get("id"))
        assertEquals("LineString", result.getJSONArray("features").getJSONObject(0).getJSONObject("geometry").get("type"))
        assertEquals(2, result.getJSONArray("features").getJSONObject(0).getJSONObject("geometry").getJSONArray("coordinates").length())
        assertTrue(result.getJSONArray("features").getJSONObject(0).getJSONObject("properties").getBoolean("tainted"))

        // a way (ref-less)
        xml = loadFile("overpass_geometry_types/full_tainted_way_refless.xml")
        result =
            JSONObject(defaultConverter.convertOsmXmlToGeoJson(xml))

        assertEquals(1, result.getJSONArray("features").length())
        assertEquals("way/1", result.getJSONArray("features").getJSONObject(0).get("id"))
        assertEquals("LineString", result.getJSONArray("features").getJSONObject(0).getJSONObject("geometry").get("type"))
        assertEquals(2, result.getJSONArray("features").getJSONObject(0).getJSONObject("geometry").getJSONArray("coordinates").length())
        assertTrue(result.getJSONArray("features").getJSONObject(0).getJSONObject("properties").getBoolean("tainted"))

        // relations
        xml = loadFile("overpass_geometry_types/full_tainted_relations.xml")
        result =
            JSONObject(defaultConverter.convertOsmXmlToGeoJson(xml))

        assertEquals(2, result.getJSONArray("features").length())
        assertEquals("way/1", result.getJSONArray("features").getJSONObject(0).get("id"))
        assertEquals("Polygon", result.getJSONArray("features").getJSONObject(0).getJSONObject("geometry").get("type"))
        assertEquals(1, result.getJSONArray("features").getJSONObject(0).getJSONObject("geometry").getJSONArray("coordinates").length())
        assertEquals(5, result.getJSONArray("features").getJSONObject(0).getJSONObject("geometry").getJSONArray("coordinates").getJSONArray(0).length())
        assertTrue(result.getJSONArray("features").getJSONObject(0).getJSONObject("properties").getBoolean("tainted"))
        assertEquals("way/3", result.getJSONArray("features").getJSONObject(1).get("id"))
        assertEquals("Polygon", result.getJSONArray("features").getJSONObject(1).getJSONObject("geometry").get("type"))
        assertEquals(1, result.getJSONArray("features").getJSONObject(1).getJSONObject("geometry").getJSONArray("coordinates").length())
        assertEquals(4, result.getJSONArray("features").getJSONObject(1).getJSONObject("geometry").getJSONArray("coordinates").getJSONArray(0).length())
        assertTrue(result.getJSONArray("features").getJSONObject(1).getJSONObject("properties").getBoolean("tainted"))
    }

    @Test
    fun fullTaintedJson() {
        // a way
        var json = loadFile("overpass_geometry_types/full_tainted_way.json")
        var result = JSONObject(
            defaultConverter.convertOverpassJsonToGeoJson(json)
        )

        assertEquals(1, result.getJSONArray("features").length())
        assertEquals("way/1", result.getJSONArray("features").getJSONObject(0).get("id"))
        assertEquals("LineString", result.getJSONArray("features").getJSONObject(0).getJSONObject("geometry").get("type"))
        assertEquals(2, result.getJSONArray("features").getJSONObject(0).getJSONObject("geometry").getJSONArray("coordinates").length())
        assertTrue(result.getJSONArray("features").getJSONObject(0).getJSONObject("properties").getBoolean("tainted"))

        // a way (ref-less)
        json = loadFile("overpass_geometry_types/full_tainted_way_refless.json")
        result = JSONObject(
            defaultConverter.convertOverpassJsonToGeoJson(json)
        )

        assertEquals(1, result.getJSONArray("features").length())
        assertEquals("way/1", result.getJSONArray("features").getJSONObject(0).get("id"))
        assertEquals("LineString", result.getJSONArray("features").getJSONObject(0).getJSONObject("geometry").get("type"))
        assertEquals(2, result.getJSONArray("features").getJSONObject(0).getJSONObject("geometry").getJSONArray("coordinates").length())
        assertTrue(result.getJSONArray("features").getJSONObject(0).getJSONObject("properties").getBoolean("tainted"))

        // relations
        json = loadFile("overpass_geometry_types/full_tainted_relations.json")
        result = JSONObject(
            defaultConverter.convertOverpassJsonToGeoJson(json)
        )

        assertEquals(2, result.getJSONArray("features").length())
        assertEquals("way/1", result.getJSONArray("features").getJSONObject(0).get("id"))
        assertEquals("Polygon", result.getJSONArray("features").getJSONObject(0).getJSONObject("geometry").get("type"))
        assertEquals(1, result.getJSONArray("features").getJSONObject(0).getJSONObject("geometry").getJSONArray("coordinates").length())
        assertEquals(5, result.getJSONArray("features").getJSONObject(0).getJSONObject("geometry").getJSONArray("coordinates").getJSONArray(0).length())
        assertTrue(result.getJSONArray("features").getJSONObject(0).getJSONObject("properties").getBoolean("tainted"))
        assertEquals("way/3", result.getJSONArray("features").getJSONObject(1).get("id"))
        assertEquals("Polygon", result.getJSONArray("features").getJSONObject(1).getJSONObject("geometry").get("type"))
        assertEquals(1, result.getJSONArray("features").getJSONObject(1).getJSONObject("geometry").getJSONArray("coordinates").length())
        assertEquals(4, result.getJSONArray("features").getJSONObject(1).getJSONObject("geometry").getJSONArray("coordinates").getJSONArray(0).length())
        assertTrue(result.getJSONArray("features").getJSONObject(1).getJSONObject("properties").getBoolean("tainted"))
    }

    @Test
    fun nestedMixedContent() {
        val json = loadFile("overpass_geometry_types/nested_mixed_content.json")
        var result = JSONObject(
            defaultConverter.convertOverpassJsonToGeoJson(json)
        )

        assertEquals("relation/1", result.getJSONArray("features").getJSONObject(0).get("id"))
        assertEquals("Polygon", result.getJSONArray("features").getJSONObject(0).getJSONObject("geometry").get("type"))
        assertEquals(1, result.getJSONArray("features").getJSONObject(0).getJSONObject("geometry").getJSONArray("coordinates").length())
        assertEquals(6, result.getJSONArray("features").getJSONObject(0).getJSONObject("geometry").getJSONArray("coordinates").getJSONArray(0).length())

        val xml = loadFile("overpass_geometry_types/nested_mixed_content.xml")
        result =
            JSONObject(defaultConverter.convertOsmXmlToGeoJson(xml))

        assertEquals("relation/1", result.getJSONArray("features").getJSONObject(0).get("id"))
        assertEquals("Polygon", result.getJSONArray("features").getJSONObject(0).getJSONObject("geometry").get("type"))
        assertEquals(1, result.getJSONArray("features").getJSONObject(0).getJSONObject("geometry").getJSONArray("coordinates").length())
        assertEquals(6, result.getJSONArray("features").getJSONObject(0).getJSONObject("geometry").getJSONArray("coordinates").getJSONArray(0).length())
    }
}