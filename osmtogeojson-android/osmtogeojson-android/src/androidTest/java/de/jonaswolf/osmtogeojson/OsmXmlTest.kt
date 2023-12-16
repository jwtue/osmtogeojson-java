package de.jonaswolf.osmtogeojson

import org.junit.Test

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