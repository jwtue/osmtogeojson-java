package de.jonaswolf.osmtogeojson.parser

import org.json.JSONObject

abstract class AbstractParser {
    val nodes = mutableListOf<JSONObject>()
    val ways = mutableListOf<JSONObject>()
    val rels = mutableListOf<JSONObject>()

}