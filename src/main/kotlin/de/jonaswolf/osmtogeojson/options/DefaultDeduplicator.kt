package de.jonaswolf.osmtogeojson.options

import de.jonaswolf.osmtogeojson.merge
import org.json.JSONObject

class DefaultDeduplicator : Deduplicator {

    override fun deduplicate(objA: JSONObject, objB: JSONObject): JSONObject {
        val versionA = objA.optDouble("version", 0.0)
        val versionB = objB.optDouble("version", 0.0)
        if (versionA > versionB) {
            return objA
        } else if (versionB > versionA) {
            return objB
        }
        return objA.merge(objB)
    }
}