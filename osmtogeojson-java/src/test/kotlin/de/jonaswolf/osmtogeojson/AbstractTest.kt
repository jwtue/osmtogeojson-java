package de.jonaswolf.osmtogeojson

import org.json.JSONObject
import java.io.File
import java.nio.file.Paths
import kotlin.test.assertEquals

abstract class AbstractTest {

    abstract val defaultConverter: OsmToGeoJson

    fun loadFile(filenameWithExtension: String): String {
        val directory = Paths.get("src", "test", "resources")
        val file = File(directory.toFile(), filenameWithExtension)
        return file.readText()
    }

    fun loadJsonFiles(filename: String): Pair<JSONObject, JSONObject> {
        val json = loadFile(filename + ".json")
        val geoJson = loadFile(filename + ".geojson")
        return JSONObject(json) to JSONObject(
            geoJson
        )
    }

    fun loadXmlFiles(filename: String): Pair<String, JSONObject> {
        val xml = loadFile(filename + ".xml")
        val geoJson = loadFile(filename + ".geojson")
        return xml to JSONObject(geoJson)
    }

    fun compareJson(input: JSONObject, expected: JSONObject, converter: OsmToGeoJson = defaultConverter) {
        val result = converter.convertOverpassJsonToGeoJson(input.toString())
        assertEquals(expected.toString(INDENT_FACTOR), JSONObject(
            result
        ).toString(INDENT_FACTOR))
    }

    fun compareXml(input: String, expected: JSONObject, converter: OsmToGeoJson = defaultConverter) {
        val result = converter.convertOsmXmlToGeoJson(input)
        assertEquals(expected.toString(INDENT_FACTOR), JSONObject(
            result
        ).toString(INDENT_FACTOR))
    }

    fun checkEmptyJson(json: JSONObject, converter: OsmToGeoJson = defaultConverter) {
        val result =
            JSONObject(converter.convertOverpassJsonToGeoJson(json.toString()))
        assert(result.getJSONArray("features").isEmpty)
    }

    fun compareJson(input: Pair<JSONObject, JSONObject>, converter: OsmToGeoJson = defaultConverter) {
        compareJson(input.first, input.second, converter)
    }

    fun compareXml(input: Pair<String, JSONObject>, converter: OsmToGeoJson = defaultConverter) {
        compareXml(input.first, input.second, converter)
    }

    companion object {
        const val INDENT_FACTOR = 4
    }

    fun Any.isTruthy() : Boolean {
        if (this is Boolean) {
            return this
        }
        if (this is Number) {
            return this != 0
        }
        return when (this.toString().lowercase()) {
            "0", "false" -> false
            else -> true
        }
    }
}