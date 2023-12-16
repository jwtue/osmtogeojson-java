package de.jonaswolf.osmtogeojson

import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object Main {
    @JvmStatic
    fun main(args: Array<String>) {
        val flags = mutableListOf<String>()
        var fileformat: String? = null

        var lastarg: String? = null
        for (arg in args) {
            if (lastarg == "-f") {
                fileformat = arg
                lastarg = null
                continue
            }
            if (arg.startsWith("--")) {
                when (arg) {
                    "-e", "-n", "-v", "-m", "--ndjson" -> flags.add(arg)
                    "--version" -> {
                        printVersion()
                        return
                    }

                    "--help" -> {
                        printHelp()
                        return
                    }

                    else -> flags.add(arg)
                }
            } else if (arg.startsWith("-")) {
                flags.addAll(arg.substring(1).toCharArray().map { "-$it" })
            }
            when (arg) {
                "-e", "-n", "-v", "-m", "--ndjson" -> flags.add(arg)
                "--version" -> {
                    printVersion()
                    return
                }

                "--help" -> {
                    printHelp()
                    return
                }
            }

            lastarg = arg
        }

        val wrongArgs = flags.filter {
            when (it) {
                "-e", "-n", "-v", "-m", "--ndjson" -> false
                else -> true
            }
        }
        if (wrongArgs.isNotEmpty()) {
            System.err.println("ERROR: Unknown argument ${wrongArgs.first()}")
            return
        }

        if (lastarg == null) {
            printHelp()
            return
        }
        val file = lastarg
        if (fileformat == "osm") {
            fileformat = "xml"
        }
        if (fileformat != "xml" && fileformat != "json") {
            if (file.lowercase().endsWith(".osm") || file.lowercase().endsWith(".xml")) {
                fileformat = "xml"
            } else if (file.lowercase().endsWith(".json")) {
                fileformat = "json"
            } else {
                System.err.println("ERROR: File format could not be detected. Please provide a valid file format.")
                return
            }
        }

        val data: String = try {
            File(file).readText()
        } catch (e: Exception) {
            System.err.println("ERROR: Could not open file \"$file\".")
            return
        }

        val converter = OsmToGeoJson(
            verbose = flags.contains("-v"),
            flatProperties = !flags.contains("-e"),
        )
        val callback: ((String) -> Unit)? = if (flags.contains("--ndjson"))
            fun(feature: String) {
                var result = JSONObject(feature)
                if (flags.contains("-n")) {
                    result = geojsonNumeric(result, flags.contains("-e"))
                }
                println(result.toString())
                println()
            }
        else null
        var result = JSONObject(
            if (fileformat == "json") converter.convertOverpassJsonToGeoJson(
                data,
                callback
            ) else converter.convertOsmXmlToGeoJson(data, callback)
        )
        if (!flags.contains("--ndjson")) {
            if (flags.contains("-n")) {
                result = geojsonNumeric(result, flags.contains("-e"))
            }
            if (flags.contains("-m")) {
                println(result.toString())
            } else {
                println(result.toString(4))
            }
        }
    }

    private fun geojsonNumeric(geojson: JSONObject, recursive: Boolean): JSONObject {
        val features = if (geojson.opt("type") == "FeatureCollection")
            geojson.getJSONArray("features")
        else if (geojson.opt("type") == "Feature")
            JSONArray(geojson)
        else
            JSONArray(
                JSONObject(
                    mapOf(
                        "type" to "Feature",
                        "properties" to JSONObject(),
                        "geometry" to geojson
                    )
                )
            )

        fun isNumeric(n: Number?): Boolean {
            return n != null && !n.toDouble().isNaN() && n.toDouble().isFinite()
        }

        fun toNumeric(obj: JSONObject?) {
            obj?.apply {
                for (key in obj.keys()) {
                    if (isNumeric(obj.optNumber(key))) {
                        obj.put(key, obj.getNumber(key))
                    } else if (recursive && obj.optJSONObject(key) != null) {
                        toNumeric(obj.getJSONObject(key))
                    }
                }
            }
        }

        features.forEachIndexed { _, feature ->
            if (feature is JSONObject) {
                toNumeric(feature.optJSONObject("properties"))
            }
        }

        return geojson
    }

    private fun printHelp() {
        println("Usage: osmtogeojson [-f format] [-e] [-v] FILE")
        println()
        println("Options:")
        println("  -f         file format. if not given, will be detected from filename. supported values: osm, json                            [string]")
        println("  -e         enhanced properties. if set, the resulting GeoJSON feature's properties will contain more structured information  [boolean]")
        println("  -n         numeric properties. if set, the resulting GeoJSON feature's properties will be numbers if possible                [boolean]")
        println("  -v         verbose mode. output diagnostic information during processing                                                     [boolean]")
        println("  -m         minify output json (no identation and linebreaks)                                                                 [boolean]")
        println("  --ndjson   output newline delimited geojson instead of a single featurecollection (implies -m enabled)                       [boolean]")
        println("  --version  display software version                                                                                          [boolean]")
        println("  --help     print this help message                                                                                           [boolean]")
    }

    private fun printVersion() {
        println("3.0.0-beta.5")
    }
}