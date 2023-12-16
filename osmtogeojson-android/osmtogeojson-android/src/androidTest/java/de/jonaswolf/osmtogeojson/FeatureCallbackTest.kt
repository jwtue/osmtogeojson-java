package de.jonaswolf.osmtogeojson

import org.json.JSONObject
import org.junit.Test
import org.junit.Assert.assertEquals

class FeatureCallbackTest : AbstractTest() {
    override val defaultConverter = OsmToGeoJson()

    @Test
    fun node() {
        val (json, geojson) = loadJsonFiles("feature_callback/node")
        defaultConverter.convertOverpassJsonToGeoJson(json.toString()) {
            assertEquals(geojson.toString(INDENT_FACTOR), JSONObject(
                it
            ).toString(INDENT_FACTOR))
        }
    }

    @Test
    fun wayLine() {
        val (json, geojson) = loadJsonFiles("feature_callback/way_line")
        defaultConverter.convertOverpassJsonToGeoJson(json.toString()) {
            assertEquals(geojson.toString(INDENT_FACTOR), JSONObject(
                it
            ).toString(INDENT_FACTOR))
        }
    }

    @Test
    fun wayPolygon() {
        val (json, geojson) = loadJsonFiles("feature_callback/way_polygon")
        defaultConverter.convertOverpassJsonToGeoJson(json.toString()) {
            assertEqualsJson(geojson, JSONObject(
                it
            ))
        }
    }

    @Test
    fun relationSimpleMultipolygon() {
        val (json, geojson) = loadJsonFiles("feature_callback/relation_simple_multipolygon")
        defaultConverter.convertOverpassJsonToGeoJson(json.toString()) {
            assertEqualsJson(geojson, JSONObject(
                it
            ))
        }
    }

    @Test
    fun relationMultipolygon() {
        val (json, geojson) = loadJsonFiles("feature_callback/relation_multipolygon")
        defaultConverter.convertOverpassJsonToGeoJson(json.toString()) {
            assertEqualsJson(geojson, JSONObject(
                it
            ))
        }
    }
}