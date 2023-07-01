import de.jonaswolf.osmtogeojson.OsmToGeoJson
import org.json.JSONObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.expect

class FeatureCallbackTest : AbstractTest() {
    override val defaultConverter = OsmToGeoJson()

    @Test
    fun node() {
        val (json, geojson) = loadJsonFiles("feature_callback/node")
        defaultConverter.convertOverpassJsonToGeoJson(json.toString()) {
            assertEquals(geojson.toString(INDENT_FACTOR), JSONObject(it).toString(INDENT_FACTOR))
        }
    }

    @Test
    fun wayLine() {
        val (json, geojson) = loadJsonFiles("feature_callback/way_line")
        defaultConverter.convertOverpassJsonToGeoJson(json.toString()) {
            assertEquals(geojson.toString(INDENT_FACTOR), JSONObject(it).toString(INDENT_FACTOR))
        }
    }

    @Test
    fun wayPolygon() {
        val (json, geojson) = loadJsonFiles("feature_callback/way_polygon")
        defaultConverter.convertOverpassJsonToGeoJson(json.toString()) {
            assertEquals(geojson.toString(INDENT_FACTOR), JSONObject(it).toString(INDENT_FACTOR))
        }
    }

    @Test
    fun relationSimpleMultipolygon() {
        val (json, geojson) = loadJsonFiles("feature_callback/relation_simple_multipolygon")
        defaultConverter.convertOverpassJsonToGeoJson(json.toString()) {
            assertEquals(geojson.toString(INDENT_FACTOR), JSONObject(it).toString(INDENT_FACTOR))
        }
    }

    @Test
    fun relationMultipolygon() {
        val (json, geojson) = loadJsonFiles("feature_callback/relation_multipolygon")
        defaultConverter.convertOverpassJsonToGeoJson(json.toString()) {
            assertEquals(geojson.toString(INDENT_FACTOR), JSONObject(it).toString(INDENT_FACTOR))
        }
    }
}