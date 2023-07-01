import de.jonaswolf.osmtogeojson.OsmToGeoJson
import de.jonaswolf.osmtogeojson.options.Deduplicator
import org.json.JSONObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DuplicateElementsTest : AbstractTest() {
    override val defaultConverter = OsmToGeoJson()

    @Test
    fun node() {
        val json = loadFile("duplicate_elements/node.json")
        val result = JSONObject(defaultConverter.convertOverpassJsonToGeoJson(json))

        assertEquals(1, result.getJSONArray("features").length())
        assertEquals("node/1", result.getJSONArray("features").getJSONObject(0).get("id"))
        assertEquals("bar", result.getJSONArray("features").getJSONObject(0).getJSONObject("properties").getJSONObject("tags").get("foo"))
        assertEquals("fasd", result.getJSONArray("features").getJSONObject(0).getJSONObject("properties").getJSONObject("tags").get("asd"))
        assertTrue(result.getJSONArray("features").getJSONObject(0).getJSONObject("properties").getJSONObject("tags").opt("dupe").isTruthy())
    }

    @Test
    fun nodeDifferentVersion() {
        val json = loadFile("duplicate_elements/node_different_version.json")
        val result = JSONObject(defaultConverter.convertOverpassJsonToGeoJson(json))

        assertEquals(1, result.getJSONArray("features").length())
        assertEquals("node/1", result.getJSONArray("features").getJSONObject(0).get("id"))
        assertNull(result.getJSONArray("features").getJSONObject(0).getJSONObject("properties").getJSONObject("tags").opt("asd"))
        assertEquals("bar", result.getJSONArray("features").getJSONObject(0).getJSONObject("properties").getJSONObject("tags").get("foo"))
        assertEquals("x", result.getJSONArray("features").getJSONObject(0).getJSONObject("properties").getJSONObject("tags").get("dupe"))
    }

    @Test
    fun way() {
        val json = loadFile("duplicate_elements/way.json")
        val result = JSONObject(defaultConverter.convertOverpassJsonToGeoJson(json))

        assertEquals(1, result.getJSONArray("features").length())
        assertEquals("way/1", result.getJSONArray("features").getJSONObject(0).get("id"))
        assertEquals("bar", result.getJSONArray("features").getJSONObject(0).getJSONObject("properties").getJSONObject("tags").get("foo"))
        assertEquals(2, result.getJSONArray("features").getJSONObject(0).getJSONObject("geometry").getJSONArray("coordinates").length())
    }

    @Test
    fun wayDifferentVersion() {
        val json = loadFile("duplicate_elements/way_different_version.json")
        val result = JSONObject(defaultConverter.convertOverpassJsonToGeoJson(json))

        assertEquals(1, result.getJSONArray("features").length())
        assertEquals("way/1", result.getJSONArray("features").getJSONObject(0).get("id"))
        assertEquals(2, result.getJSONArray("features").getJSONObject(0).getJSONObject("properties").getJSONObject("meta").get("version"))
        assertNull(result.getJSONArray("features").getJSONObject(0).getJSONObject("properties").getJSONObject("tags").opt("foo"))
        assertEquals("y", result.getJSONArray("features").getJSONObject(0).getJSONObject("properties").getJSONObject("tags").opt("dupe"))
        assertEquals("fasd", result.getJSONArray("features").getJSONObject(0).getJSONObject("properties").getJSONObject("tags").opt("asd"))
        assertEquals(3, result.getJSONArray("features").getJSONObject(0).getJSONObject("geometry").getJSONArray("coordinates").length())
    }

    @Test
    fun relation() {
        val json = loadFile("duplicate_elements/relation.json")
        val result = JSONObject(defaultConverter.convertOverpassJsonToGeoJson(json))

        assertEquals(2, result.getJSONArray("features").length())
        assertEquals("relation/1", result.getJSONArray("features").getJSONObject(0).get("id"))
        assertEquals("way/2", result.getJSONArray("features").getJSONObject(1).get("id"))
        assertEquals(2, result.getJSONArray("features").getJSONObject(0).getJSONObject("properties").getJSONObject("meta").get("version"))
        assertEquals("2", result.getJSONArray("features").getJSONObject(0).getJSONObject("properties").getJSONObject("tags").get("foo"))
    }

    @Test
    fun relationAdditionalSkeletonWays() {
        val json = loadFile("duplicate_elements/relation_additional_skeleton_ways.json")
        val result = JSONObject(defaultConverter.convertOverpassJsonToGeoJson(json))

        assertEquals(2, result.getJSONArray("features").length())
        assertEquals("relation/1", result.getJSONArray("features").getJSONObject(0).get("id"))
        assertEquals("way/1", result.getJSONArray("features").getJSONObject(1).get("id"))
        assertEquals(1, result.getJSONArray("features").getJSONObject(0).getJSONObject("geometry").getJSONArray("coordinates").length())
    }

    @Test
    fun customDeduplicator() {
        val json = loadFile("duplicate_elements/custom_deduplicator.json")
        val deduplicatorConverter = OsmToGeoJson(
            deduplicator = Deduplicator { objA, objB ->
                if (objA.getNumber("version").toDouble() < objB.getNumber("version").toDouble()) objA else objB
            }
        )
        val result = JSONObject(deduplicatorConverter.convertOverpassJsonToGeoJson(json))

        assertEquals(1, result.getJSONArray("features").length())
        assertEquals("node/1", result.getJSONArray("features").getJSONObject(0).get("id"))
        assertEquals("fasd", result.getJSONArray("features").getJSONObject(0).getJSONObject("properties").getJSONObject("tags").get("asd"))
        assertNull(result.getJSONArray("features").getJSONObject(0).getJSONObject("properties").getJSONObject("tags").opt("foo"))
        assertEquals("y", result.getJSONArray("features").getJSONObject(0).getJSONObject("properties").getJSONObject("tags").get("dupe"))
    }
}