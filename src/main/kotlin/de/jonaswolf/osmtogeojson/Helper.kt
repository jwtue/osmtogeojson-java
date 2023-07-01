package de.jonaswolf.osmtogeojson

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import kotlin.math.max
import kotlin.math.min

fun JSONArray.toListOfJSONObjects(): List<JSONObject?> {
    return (0 until length()).map {
        try {
            optJSONObject(it)
        } catch (_: JSONException) {
            null
        }
    }
}
fun JSONArray.toListOfJSONArrays(): List<JSONArray?> {
    return (0 until length()).map {
        try {
            optJSONArray(it)
        } catch (_: JSONException) {
            null
        }
    }
}
val JSONArray.indices : IntRange
    get()  = 0 until length()

fun JSONArray.firstAsJSONObject() : JSONObject? {
    return optJSONObject(indices.first)
}
fun JSONArray.lastAsJSONObject() : JSONObject? {
    return optJSONObject(indices.last)
}

fun JSONObject.toStringMap(): Map<String, String> {
    val output = mutableMapOf<String, String>()
    this.keys().forEach {
        output[it] = getString(it)
    }
    return output
}

fun JSONArray.slice(start: Int? = null, end: Int? = null) : JSONArray {
    var realStart = start ?: 0
    var realEnd = end ?: length()
    if (realStart < 0) realStart += length()
    if (realEnd < 0) realEnd += length()

    realStart = max(0, realStart)
    realEnd = min(length(), realEnd)

    val output = JSONArray()
    for (i in realStart until realEnd) {
        output.put(this[i])
    }
    return output
}
fun JSONArray.reverse() : JSONArray {
    val output = JSONArray()
    for (i in (length()-1) downTo 0) {
        output.put(this[i])
    }
    return output
}

fun JSONObject.merge(vararg objects: JSONObject) : JSONObject {
    for (obj in objects) {
        for (k in obj.keys()) {
            val subA: JSONObject? = optJSONObject(k)
            val subB: JSONObject? = obj.optJSONObject(k)
            if (subA != null && subB != null) {
                put(k, subA.merge(subB))
            } else if (obj.opt(k) != null) {
                put(k, obj.get(k))
            }
        }
    }
    return this
}