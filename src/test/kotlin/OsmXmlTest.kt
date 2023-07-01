import kotlin.test.Test
import de.jonaswolf.osmtogeojson.OsmToGeoJson

class OsmXmlTest : AbstractTest() {

    override val defaultConverter = OsmToGeoJson()

    @Test
    fun blankOsm() {
        compareXml(loadXmlFiles("osm_xml/blank_osm"))
    }

    @Test
    fun node() {
        compareXml(loadXmlFiles("osm_xml/node"))
    }

    @Test
    fun way() {
        compareXml(loadXmlFiles("osm_xml/way"))
    }

    @Test
    fun relation() {
        compareXml(loadXmlFiles("osm_xml/relation"))
    }
}