import kotlin.test.Test
import de.jonaswolf.osmtogeojson.OsmToGeoJson
import org.json.JSONObject
import kotlin.test.assertEquals

class OtherTest : AbstractTest() {

    override val defaultConverter = OsmToGeoJson()

    @Test
    fun sideeffects() {
        val json = JSONObject(loadFile("other/sideeffects.json"))
        val jsonBefore = json.toString(INDENT_FACTOR)
        defaultConverter.convertOverpassJsonToGeoJson(json.toString())
        val jsonAfter = json.toString(INDENT_FACTOR)
        assertEquals(jsonBefore, jsonAfter)
    }
}