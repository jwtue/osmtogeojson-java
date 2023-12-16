package de.jonaswolf.osmtogeojson.options

import org.json.JSONObject

abstract class UninterestingTagsValidator {
    abstract fun validate(tags: JSONObject, ignoreTags: List<UninterestingTag>) : Boolean
}