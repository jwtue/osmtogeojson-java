package de.jonaswolf.osmtogeojson.options

class OverallUninterestingTag(val key: String) : UninterestingTag() {
    override fun matches(key: String, value: Any): Boolean {
        return key == this.key
    }
}