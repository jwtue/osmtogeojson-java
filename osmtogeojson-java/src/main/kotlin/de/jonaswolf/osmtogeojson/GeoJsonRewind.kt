package de.jonaswolf.osmtogeojson

import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.abs

// source: https://github.com/mapbox/geojson-rewind/blob/main/index.js
object GeoJsonRewind {
    fun rewind(geoJson: JSONObject, outer: Boolean = false) : JSONObject {
        var gj = geoJson
        val type = gj.optString("type")

        when (type) {
            "FeatureCollection" -> {
                val features = gj.getJSONArray("features")
                for (i in 0 until features.length()) {
                    features.put(i, rewind(features.getJSONObject(i), outer))
                }
                gj.put("features", features)
            }
            "GeometryCollection" -> {
                val geometries = gj.getJSONArray("geometries")
                for (i in 0 until geometries.length()) {
                    geometries.put(i, rewind(geometries.getJSONObject(i), outer))
                }
                gj.put("geometries", geometries)
            }
            "Feature" -> {
                gj.put("geometry", rewind(gj.getJSONObject("geometry"), outer))
            }
            "Polygon" -> {
                gj.put("coordinates", rewindRings(gj.getJSONArray("coordinates"), outer))
            }
            "MultiPolygon" -> {
                val coordinates = gj.getJSONArray("coordinates")
                for (i in 0 until coordinates.length()) {
                    coordinates.put(i, rewindRings(coordinates.getJSONArray(i), outer))
                }
                gj.put("coordinates", coordinates)
            }
        }
        return gj
    }

    private fun rewindRings(rings: JSONArray, outer: Boolean) : JSONArray {
        if (rings.length() == 0) return rings

        rings.put(0, rewindRing(rings.getJSONArray(0), outer))
        for (i in 1 until rings.length()) {
            rings.put(i, rewindRing(rings.getJSONArray(i), !outer))
        }
        return rings
    }

    private fun rewindRing(ring: JSONArray, dir: Boolean) : JSONArray {
        var area = 0.0
        var err = 0.0

        var i = 0
        val len = ring.length()
        var j = len -1
        while (i < len) {
            val k = (ring.getJSONArray(i).getDouble(0) - ring.getJSONArray(j).getDouble(0)) * (ring.getJSONArray(j).getDouble(1) + ring.getJSONArray(i).getDouble(1))
            val m = area + k
            err += if (abs(area) >= abs(k)) area - m + k else k - m + area
            area = m

            j = i++
        }

        return if (area + err >= 0 != dir) {
            val output = JSONArray()
            for (index in (0 until ring.length()).reversed()) {
                output.put(ring.get(index))
            }
            output
        } else ring
    }
}