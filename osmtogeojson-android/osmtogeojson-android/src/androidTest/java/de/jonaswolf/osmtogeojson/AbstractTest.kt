package de.jonaswolf.osmtogeojson

import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.ComparisonFailure
import java.io.File
import java.nio.file.Paths

abstract class AbstractTest {

    abstract val defaultConverter: OsmToGeoJson

    fun loadFile(filenameWithExtension: String): String {
        val assets = InstrumentationRegistry.getInstrumentation().context.assets
        val inputStream = assets.open(filenameWithExtension)
        val sb = StringBuilder()
        var ch: Int
        while (inputStream.read().also { ch = it } != -1) {
            sb.append(ch.toChar())
        }
        return sb.toString()
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

    fun compareJson(
        input: JSONObject,
        expected: JSONObject,
        converter: OsmToGeoJson = defaultConverter
    ) {
        val result = converter.convertOverpassJsonToGeoJson(input.toString())
        assertEqualsJson(expected, JSONObject(result))
    }

    fun compareXml(
        input: String,
        expected: JSONObject,
        converter: OsmToGeoJson = defaultConverter
    ) {
        val result = converter.convertOsmXmlToGeoJson(input)
        assertEqualsJson(expected, JSONObject(result))
    }

    fun checkEmptyJson(json: JSONObject, converter: OsmToGeoJson = defaultConverter) {
        val result =
            JSONObject(converter.convertOverpassJsonToGeoJson(json.toString()))
        assert(result.getJSONArray("features").isEmpty)
    }

    fun compareJson(
        input: Pair<JSONObject, JSONObject>,
        converter: OsmToGeoJson = defaultConverter
    ) {
        compareJson(input.first, input.second, converter)
    }

    fun compareXml(input: Pair<String, JSONObject>, converter: OsmToGeoJson = defaultConverter) {
        compareXml(input.first, input.second, converter)
    }

    companion object {
        const val INDENT_FACTOR = 4
    }

    fun Any.isTruthy(): Boolean {
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

    protected fun assertEqualsJson(expected: JSONObject, actual: JSONObject) {
        for (key in expected.keys()) {
            if (!actual.has(key))
                throw AssertionError("JSONObject: Expected key $key is missing.")
            val exp = expected.get(key)
            val act = actual.get(key)
            if (exp is Number && act is Number) {
                assertEquals(exp.toDouble(), act.toDouble(), 0.001)
            } else if (exp::class != act::class) {
                throw ComparisonFailure(
                    "JSONObject: Different types of value for key $key",
                    exp::class.simpleName,
                    act::class.simpleName
                )
            } else if (exp is JSONObject && act is JSONObject) {
                assertEqualsJson(exp, act)
            } else if (exp is JSONArray && act is JSONArray) {
                assertEqualsJson(exp, act)
            } else {
                assertEquals(exp, act)
            }
        }
        for (key in actual.keys()) {
            if (!expected.has(key)) {
                throw AssertionError("JSONObject: Unexpected key $key detected.")
            }
        }
    }

    private fun assertEqualsJson(expected: JSONArray, actual: JSONArray) {
        if (expected.length() != actual.length()) {
            throw ComparisonFailure(
                "JSONArray: Lengths differ",
                expected.length().toString(),
                actual.length().toString()
            )
        }
        for (i in expected.indices) {
            val exp = expected.get(i)
            val act = actual.get(i)
            if (exp is Number && act is Number) {
                assertEquals(exp.toDouble(), act.toDouble(), 0.001)
            } else if (exp::class != act::class) {
                throw ComparisonFailure(
                    "JSONObject: Different types of value for index $i",
                    exp::class.simpleName,
                    act::class.simpleName
                )
            } else if (exp is JSONObject && act is JSONObject) {
                assertEqualsJson(exp, act)
            } else if (exp is JSONArray && act is JSONArray) {
                assertEqualsJson(exp, act)
            } else {
                assertEquals(exp, act)
            }
        }
    }
}