package de.jonaswolf.osmtogeojson.options

abstract class UninterestingTag {
    abstract fun matches(key: String, value: Any): Boolean
}