package de.jonaswolf.osmtogeojson.options

import org.json.JSONObject

fun interface Deduplicator {
    fun deduplicate(objA: JSONObject, objB: JSONObject): JSONObject
}