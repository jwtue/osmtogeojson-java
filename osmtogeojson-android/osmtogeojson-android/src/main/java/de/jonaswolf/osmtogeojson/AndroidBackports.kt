package de.jonaswolf.osmtogeojson

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.io.StringWriter
import java.io.Writer
import java.math.BigDecimal
import java.math.BigInteger
import java.util.HashMap
import kotlin.reflect.KClass


fun JSONArray.first() : Any? {
    return this.get(0)
}

val JSONArray.isEmpty : Boolean
    get() = this.length() == 0

fun JSONArray.forEach(action: (Any?) -> Unit) {
    for (i in this.indices) {
        action(this.get(i))
    }
}

fun JSONArray.forEachIndexed(action: (Int, Any?) -> Unit) {
    for (i in this.indices) {
        action(i, this.get(i))
    }
}

fun JSONObject.optJSONObject(key: String, defaultValue: JSONObject) : JSONObject {
    val obj = this.opt(key)
    return if (obj is JSONObject) obj else defaultValue
}

fun JSONArray.putAll(list: List<Any>) {
    for (l in list) {
        this.put(if (l is Map<*, *>) {
            JSONObject(l)
        } else l)
    }
}
fun JSONArray.putAll(array: JSONArray) {
    this.putAll(array.toList().filterNotNull())
}
operator fun JSONArray.iterator() : Iterator<Any?> {
    return JSONArrayIterator(this)
}

class JSONArrayIterator(private val array: JSONArray) : Iterator<Any?> {
    var index = 0;

    override fun hasNext(): Boolean {
        return index < array.length()
    }

    override fun next(): Any? {
        return array.get(index++)
    }

}

/**
 * Get an optional {@link Number} value associated with a key, or the default if there
 * is no such key or if the value is not a number. If the value is a string,
 * an attempt will be made to evaluate it as a number. This method
 * would be used in cases where type coercion of the number value is unwanted.
 *
 * @param key
 *            A key string.
 * @param defaultValue
 *            The default.
 * @return An object which is the value.
 */
fun JSONObject.optNumber(key: String, defaultValue : Number? = null) : Number? {
    val value : Any? = this.opt(key)
    if (value == null || JSONObject.NULL.equals(value)) {
        return defaultValue
    }
    if (value is Number){
        return value
    }

    try {
        return NumberConversionUtil.stringToNumber(value.toString());
    } catch (e: Exception) {
        return defaultValue
    }
}


/**
 * Get the Number value associated with a key.
 *
 * @param key
 * A key string.
 * @return The numeric value.
 * @throws org.json.JSONException
 * if the key is not found or if the value is not a Number
 * object and cannot be converted to a number.
 */
@Throws(JSONException::class)
fun JSONObject.getNumber(key: String): Number {
    val `object`: Any = this.get(key)
    return try {
        if (`object` is Number) {
            `object`
        } else NumberConversionUtil.stringToNumber(`object`.toString())
    } catch (e: java.lang.Exception) {
        throw wrongValueFormatException(key, "number", `object`, e)
    }
}

/**
 * Returns a java.util.Map containing all of the entries in this object.
 * If an entry in the object is a JSONArray or JSONObject it will also
 * be converted.
 *
 *
 * Warning: This method assumes that the data structure is acyclical.
 *
 * @return a java.util.Map containing the entries of this object
 */
fun JSONObject.toMap(): Map<String, Any?> {
    val results: MutableMap<String, Any?> = HashMap()
    for (key in this.keys()) {
        val value1 : Any? = this.get(key)
        val value = if (value1 == null || JSONObject.NULL.equals(value1)) {
            null
        } else if (value1 is JSONObject) {
            value1.toMap()
        } else if (value1 is JSONArray) {
            value1.toList()
        } else {
            value1
        }
        results[key] = value
    }
    return results
}

/**
 * Returns a java.util.List containing all of the elements in this array.
 * If an element in the array is a JSONArray or JSONObject it will also
 * be converted to a List and a Map respectively.
 *
 *
 * Warning: This method assumes that the data structure is acyclical.
 *
 * @return a java.util.List containing the elements of this array
 */
fun JSONArray.toList(): List<Any?> {
    val results = mutableListOf<Any?>()
    for (i in this.indices) {
        val element : Any? = this.get(i)
        if (element == null || JSONObject.NULL == element) {
            results.add(null)
        } else if (element is JSONArray) {
            results.add(element.toList())
        } else if (element is JSONObject) {
            results.add(element.toMap())
        } else {
            results.add(element)
        }
    }
    return results
}

/**
 * Create a new JSONException in a common format for incorrect conversions.
 * @param key name of the key
 * @param valueType the type of value being coerced to
 * @param cause optional cause of the coercion failure
 * @return JSONException that can be thrown.
 */
private fun wrongValueFormatException(
    key: String,
    valueType: String,
    value: Any?,
    cause: Throwable
): JSONException {
    if (value == null) {
        return JSONException(
            "JSONObject[" + quote(key) + "] is not a " + valueType + " (null).", cause
        )
    }
    // don't try to toString collections or known object types that could be large.
    return if (value is Map<*, *> || value is Iterable<*> || value is JSONObject) {
        JSONException(
            "JSONObject[" + quote(key) + "] is not a " + valueType + " (" + value.javaClass + ").",
            cause
        )
    } else JSONException(
        "JSONObject[" + quote(key) + "] is not a " + valueType + " (" + value.javaClass + " : " + value + ").",
        cause
    )
}

/**
 * Produce a string in double quotes with backslash sequences in all the
 * right places. A backslash will be inserted within &lt;/, producing
 * &lt;\/, allowing JSON text to be delivered in HTML. In JSON text, a
 * string cannot contain a control character or an unescaped quote or
 * backslash.
 *
 * @param string
 * A String
 * @return A String correctly formatted for insertion in a JSON text.
 */
@Throws(IOException::class)
private fun quote(string: String?, ww: Writer? = null): Writer {
    val w = ww ?: StringWriter()
    if (string == null || string.isEmpty()) {
        w.write("\"\"")
        return w
    }
    var b: Char
    var c = 0.toChar()
    var hhhh: String
    var i: Int
    val len = string.length
    w.write('"'.code)
    i = 0
    while (i < len) {
        b = c
        c = string[i]
        when (c) {
            '\\', '"' -> {
                w.write('\\'.code)
                w.write(c.code)
            }

            '/' -> {
                if (b == '<') {
                    w.write('\\'.code)
                }
                w.write(c.code)
            }

            '\b' -> w.write("\\b")
            '\t' -> w.write("\\t")
            '\n' -> w.write("\\n")
            '\r' -> w.write("\\r")
            else -> if ((c < ' ' || c >= '\u0080' && c < '\u00a0' || c >= '\u2000') && c < '\u2100') {
                w.write("\\u")
                hhhh = Integer.toHexString(c.code)
                w.write("0000", 0, 4 - hhhh.length)
                w.write(hhhh)
            } else {
                w.write(c.code)
            }
        }
        i += 1
    }
    w.write('"'.code)
    return w
}


object NumberConversionUtil {

    /**
     * Converts a string to a number using the narrowest possible type. Possible
     * returns for this function are BigDecimal, Double, BigInteger, Long, and Integer.
     * When a Double is returned, it should always be a valid Double and not NaN or +-infinity.
     *
     * @param input value to convert
     * @return Number representation of the value.
     * @throws NumberFormatException thrown if the value is not a valid number. A public
     * caller should catch this and wrap it in a [JSONException] if applicable.
     */
    @Throws(NumberFormatException::class)
    fun stringToNumber(input: String): Number {
        var `val` = input
        if (`val`.startsWith(".")) {
            `val` = "0$`val`"
        }
        if (`val`.startsWith("-.")) {
            `val` = "-0." + `val`.substring(2)
        }
        var initial = `val`[0]
        if (initial >= '0' && initial <= '9' || initial == '-') {
            // decimal representation
            if (isDecimalNotation(`val`)) {
                // Use a BigDecimal all the time so we keep the original
                // representation. BigDecimal doesn't support -0.0, ensure we
                // keep that by forcing a decimal.
                return try {
                    val bd = BigDecimal(`val`)
                    if (initial == '-' && BigDecimal.ZERO.compareTo(bd) == 0) {
                        java.lang.Double.valueOf(-0.0)
                    } else bd
                } catch (retryAsDouble: NumberFormatException) {
                    // this is to support "Hex Floats" like this: 0x1.0P-1074
                    try {
                        val d = java.lang.Double.valueOf(`val`)
                        if (d.isNaN() || d.isInfinite()) {
                            throw NumberFormatException("val [$input] is not a valid number.")
                        }
                        d
                    } catch (ignore: NumberFormatException) {
                        throw NumberFormatException("val [$input] is not a valid number.")
                    }
                }
            }
            `val` = removeLeadingZerosOfNumber(input)
            initial = `val`[0]
            if (initial == '0' && `val`.length > 1) {
                val at1 = `val`[1]
                if (at1 >= '0' && at1 <= '9') {
                    throw NumberFormatException("val [$input] is not a valid number.")
                }
            } else if (initial == '-' && `val`.length > 2) {
                val at1 = `val`[1]
                val at2 = `val`[2]
                if (at1 == '0' && (at2 >= '0') && (at2 <= '9')) {
                    throw NumberFormatException("val [$input] is not a valid number.")
                }
            }
            // integer representation.
            // This will narrow any values to the smallest reasonable Object representation
            // (Integer, Long, or BigInteger)

            // BigInteger down conversion: We use a similar bitLength compare as
            // BigInteger#intValueExact uses. Increases GC, but objects hold
            // only what they need. i.e. Less runtime overhead if the value is
            // long lived.
            val bi = BigInteger(`val`)
            if (bi.bitLength() <= 31) {
                return Integer.valueOf(bi.toInt())
            }
            return if (bi.bitLength() <= 63) {
                java.lang.Long.valueOf(bi.toLong())
            } else bi
        }
        throw NumberFormatException("val [$input] is not a valid number.")
    }

    /**
     * Checks if the value could be considered a number in decimal number system.
     * @param value
     * @return
     */
    fun potentialNumber(value: String?): Boolean {
        return if (value == null || value.isEmpty()) {
            false
        } else potentialPositiveNumberStartingAtIndex(value, if (value[0] == '-') 1 else 0)
    }

    /**
     * Tests if the value should be tried as a decimal. It makes no test if there are actual digits.
     *
     * @param val value to test
     * @return true if the string is "-0" or if it contains '.', 'e', or 'E', false otherwise.
     */
    private fun isDecimalNotation(`val`: String): Boolean {
        return (`val`.indexOf('.') > -1) || (`val`.indexOf('e') > -1) || (`val`.indexOf('E') > -1) || ("-0" == `val`)
    }

    private fun potentialPositiveNumberStartingAtIndex(value: String, index: Int): Boolean {
        return if (index >= value.length) {
            false
        } else digitAtIndex(value, if (value[index] == '.') index + 1 else index)
    }

    private fun digitAtIndex(value: String, index: Int): Boolean {
        return if (index >= value.length) {
            false
        } else value[index] >= '0' && value[index] <= '9'
    }

    /**
     * For a prospective number, remove the leading zeros
     * @param value prospective number
     * @return number without leading zeros
     */
    private fun removeLeadingZerosOfNumber(value: String): String {
        if (value == "-") {
            return value
        }
        val negativeFirstChar = value[0] == '-'
        var counter = if (negativeFirstChar) 1 else 0
        while (counter < value.length) {
            if (value[counter] != '0') {
                return if (negativeFirstChar) {
                    "-" + value.substring(counter)
                } else value.substring(counter)
            }
            ++counter
        }
        return if (negativeFirstChar) {
            "-0"
        } else "0"
    }
}