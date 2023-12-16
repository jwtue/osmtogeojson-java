package de.jonaswolf.osmtogeojson.options

class ValueDependantUninterestingTag(val key: String, val value: Any) : UninterestingTag() {
    override fun matches(key: String, value: Any?): Boolean {
        return key == this.key && value == this.value
    }
}