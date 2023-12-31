package de.jonaswolf.osmtogeojson

import org.json.JSONArray
import org.json.JSONObject

class Relsmap : Map<String, MutableMap<Any, MutableList<RelsmapEntry>>> {
    val node = mutableMapOf<Any, MutableList<RelsmapEntry>>()
    val way = mutableMapOf<Any, MutableList<RelsmapEntry>>()
    val relation = mutableMapOf<Any, MutableList<RelsmapEntry>>()

    override val entries: Set<Map.Entry<String, MutableMap<Any, MutableList<RelsmapEntry>>>>
        get() = setOf(("node" to node).toEntry(), ("way" to way).toEntry(), ("relation" to relation).toEntry())
    override val keys: Set<String> = setOf("node", "way", "relation")
    override val size: Int = 3
    override val values: Collection<MutableMap<Any, MutableList<RelsmapEntry>>>
        get() = listOf(node, way, relation)

    val json : JSONObject
        get() {
            val out = JSONObject()
            val nodes = JSONObject().apply {
                node.forEach { this.put(it.key.toString(), JSONArray().apply { putAll(it.value.map { it.json }) }) }
            }
            val ways = JSONObject().apply {
                way.forEach { this.put(it.key.toString(), JSONArray().apply { putAll(it.value.map { it.json }) }) }
            }
            val rels = JSONObject().apply {
                relation.forEach { this.put(it.key.toString(), JSONArray().apply { putAll(it.value.map { it.json }) }) }
            }
            out.put("nodes", nodes)
            out.put("ways", ways)
            out.put("relations", rels)
            return out
        }

    override fun isEmpty(): Boolean {
        return false
    }

    override fun get(key: String): MutableMap<Any, MutableList<RelsmapEntry>>? {
        return when (key) {
            "node" -> node
            "way" -> way
            "relation" -> relation
            else -> null
        }
    }

    override fun containsValue(value: MutableMap<Any, MutableList<RelsmapEntry>>): Boolean {
        return values.contains(value)
    }

    override fun containsKey(key: String): Boolean {
        return keys.contains(key)
    }

    companion object {
        private fun <K, V> Pair<K, V>.toEntry() = object : Map.Entry<K, V> {
            override val key: K = first
            override val value: V = second
        }
    }
}

class RelsmapEntry(val role: Any?, val rel: Any, val reltags: JSONObject) {
    val json : JSONObject
        get() = JSONObject(
            mapOf(
                "role" to role,
                "rel" to rel,
                "reltags" to reltags
            )
        )
}