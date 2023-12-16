package de.jonaswolf.osmtogeojson

import org.json.JSONObject
import org.junit.Test
import org.junit.Assert.assertEquals

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