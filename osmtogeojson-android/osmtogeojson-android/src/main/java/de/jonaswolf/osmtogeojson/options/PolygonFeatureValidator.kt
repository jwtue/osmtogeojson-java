package de.jonaswolf.osmtogeojson.options

import org.json.JSONObject

abstract class PolygonFeatureValidator {
    abstract fun validate(tags: JSONObject) : Boolean
}