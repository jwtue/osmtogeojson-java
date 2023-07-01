import de.jonaswolf.osmtogeojson.OsmToGeoJson
import de.jonaswolf.osmtogeojson.options.OverallUninterestingTag
import de.jonaswolf.osmtogeojson.options.PolygonFeatureValidator
import de.jonaswolf.osmtogeojson.options.UninterestingTag
import de.jonaswolf.osmtogeojson.options.UninterestingTagsValidator
import org.json.JSONObject
import kotlin.test.assertEquals
import kotlin.test.Test

class OptionsTest : AbstractTest() {

    override val defaultConverter = OsmToGeoJson()

    @Test
    fun flattenedProperties() {
        val (json, geoJson) = loadJsonFiles("options/flattened_properties")
        val flattenedConverter = OsmToGeoJson(flatProperties = true)
        val result = JSONObject(flattenedConverter.convertOverpassJsonToGeoJson(json.toString()))
        assertEquals(result.getJSONArray("features").getJSONObject(0).getJSONObject("properties").toString(INDENT_FACTOR), geoJson.toString(INDENT_FACTOR))
    }

    @Test
    fun uninterestingTags() {
        val json = loadFile("options/uninteresting_tags.json")
        val tagsConverter = OsmToGeoJson(
            uninterestingTags = listOf(OverallUninterestingTag("foo"))
        )
        val result = JSONObject(tagsConverter.convertOverpassJsonToGeoJson(json))
        assertEquals(2, result.getJSONArray("features").length())
        assertEquals(3, result.getJSONArray("features").getJSONObject(1).getJSONObject("properties").getNumber("id"))
    }

    @Test
    fun uninterestingTagsCallback() {
        val json = loadFile("options/uninteresting_tags_callback.json")
        val tagsConverter = OsmToGeoJson(
            uninterestingTagsValidator = object : UninterestingTagsValidator() {
                override fun validate(tags: JSONObject, ignoreTags: List<UninterestingTag>): Boolean {
                    return tags.opt("tag") != "1"
                }
            }
        )
        val result = JSONObject(tagsConverter.convertOverpassJsonToGeoJson(json))
        assertEquals(2, result.getJSONArray("features").length())
        assertEquals("LineString", result.getJSONArray("features").getJSONObject(0).getJSONObject("geometry").get("type"))
        assertEquals("Point", result.getJSONArray("features").getJSONObject(1).getJSONObject("geometry").get("type"))
        assertEquals(1, result.getJSONArray("features").getJSONObject(1).getJSONObject("properties").get("id"))
    }

    @Test
    fun polygonDetection() {
        // custom tagging detection rules
        val json = loadFile("options/polygon_detection.json")
        val polygonFeatures = JSONObject("""
            {
        "is_polygon_key": true,
        "is_polygon_key_value": {
          "included_values": {"included_value": true}
        },
        "is_polygon_key_excluded_value": {
          "excluded_values": {"excluded_value": true}
        }
      }
    }
        """.trimIndent())
        val polygonFeatureConverter = OsmToGeoJson(additionalPolygonFeatures = polygonFeatures)
        val result = JSONObject(polygonFeatureConverter.convertOverpassJsonToGeoJson(json))
        assertEquals(6, result.getJSONArray("features").length())
        assertEquals("Polygon", result.getJSONArray("features").getJSONObject(0).getJSONObject("geometry").get("type"))
        assertEquals("Polygon", result.getJSONArray("features").getJSONObject(1).getJSONObject("geometry").get("type"))
        assertEquals("Polygon", result.getJSONArray("features").getJSONObject(2).getJSONObject("geometry").get("type"))
        assertEquals("LineString", result.getJSONArray("features").getJSONObject(3).getJSONObject("geometry").get("type"))
        assertEquals("LineString", result.getJSONArray("features").getJSONObject(4).getJSONObject("geometry").get("type"))
        assertEquals("LineString", result.getJSONArray("features").getJSONObject(5).getJSONObject("geometry").get("type"))
    }

    @Test
    fun polygonDetectionCallback() {
        // custom tagging detection rules
        val json = loadFile("options/polygon_detection_callback.json")
        val polygonFeatureConverter = OsmToGeoJson(polygonFeaturesValidator = object : PolygonFeatureValidator() {
            override fun validate(tags: JSONObject): Boolean {
                return tags.optString("tag") == "1"
            }

        })
        val result = JSONObject(polygonFeatureConverter.convertOverpassJsonToGeoJson(json))
        assertEquals(2, result.getJSONArray("features").length())
        assertEquals("Polygon", result.getJSONArray("features").getJSONObject(0).getJSONObject("geometry").get("type"))
        assertEquals("LineString", result.getJSONArray("features").getJSONObject(1).getJSONObject("geometry").get("type"))
    }
}