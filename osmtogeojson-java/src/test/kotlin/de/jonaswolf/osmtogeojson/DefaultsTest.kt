package de.jonaswolf.osmtogeojson

import org.json.JSONObject
import kotlin.test.Test
import kotlin.test.assertEquals

class DefaultsTest : AbstractTest() {

    override val defaultConverter = OsmToGeoJson()

    @Test
    fun interestingObjects() {
        val json =
            JSONObject(loadFile("defaults/interesting_objects.json"))
        val result = JSONObject(
            defaultConverter.convertOverpassJsonToGeoJson(json.toString())
        )
        assertEquals(result.getJSONArray("features").length(), 2)
        assertEquals(
            result.getJSONArray("features").getJSONObject(0).getJSONObject("geometry").get("type").toString(),
            "LineString"
        )
        assertEquals(
            result.getJSONArray("features").getJSONObject(1).getJSONObject("geometry").get("type").toString(), "Point"
        )
        assertEquals(
            result.getJSONArray("features").getJSONObject(1).getJSONObject("properties").get("id").toString(), "2"
        )
    }

    @Test
    fun interestingObjectsRelationMembers() {
        val xml = loadFile("defaults/interesting_objects_relation_members.xml")
        val result =
            JSONObject(defaultConverter.convertOsmXmlToGeoJson(xml))
        assertEquals(8, result.optJSONArray("features")?.length())
    }

    @Test
    // see: https://wiki.openstreetmap.org/wiki/Overpass_turbo/Polygon_Features
    fun polygonDetection() {
        var json =
            JSONObject(loadFile("defaults/polygon_detection_area_yes.json"))
        var result = JSONObject(
            defaultConverter.convertOverpassJsonToGeoJson(json.toString())
        )
        assertEquals(2, result.getJSONArray("features").length())
        assertEquals(
            result.getJSONArray("features").getJSONObject(0).getJSONObject("geometry").get("type").toString(),
            "Polygon"
        )
        assertEquals(
            result.getJSONArray("features").getJSONObject(1).getJSONObject("geometry").get("type").toString(),
            "LineString"
        )

        json =
            JSONObject(loadFile("defaults/polygon_detection_area_no.json"))
        result = JSONObject(
            defaultConverter.convertOverpassJsonToGeoJson(json.toString())
        )
        assertEquals(result.getJSONArray("features").length(), 1)
        assertEquals(
            result.getJSONArray("features").getJSONObject(0).getJSONObject("geometry").get("type").toString(),
            "LineString"
        )
    }
}