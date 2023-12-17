osmtogeojson-java
===================

Converts [OSM](https://www.openstreetmap.org/) [data](https://wiki.openstreetmap.org/wiki/OSM_XML) to [GeoJSON](https://geojson.org/). 

This is a Kotlin / Java / Android implementation of the Javascript/Node.js library [osmtogeojson](https://github.com/tyrasd/osmtogeojson).

All features from the original library are available, including command-line interface.

As the implementation relies heavily on `org.json`, the JAR library is unfortunately incompatible with Android. Thus, a version with minor adaptions for Android is supplied as well.


Usage
-----

### command line tool

Usage:
```
    $ java -jar osmtogeojson.jar file.osm > file.geojson
```
Supported command line options are shown with:
```
    $ java -jar osmtogeojson-3.0.0-beta.5.jar --help
```
### java/android library

Usage:
For plain Java projects add the .jar file to your project as a library. For Android projects, use the .aar file instead.
```java
import de.jonaswolf.osmtogeojson.OsmToGeoJson

String osmDataJson = /* ... */;
String geoJsonFromJson = new OsmToGeoJson().convertOverpassJsonToGeoJson(osmDataJson, null);

// or

String osmDataXml = /* ... */;
String geoJsonFromXml = new OsmToGeoJson().convertOsmXmlToGeoJson(osmDataXml, null);
```

API
---

### `new OsmToGeoJson()`
### `new OsmToGeoJson(verbose, flatProperties, uninterestingTags, uninterestingTagsValidator, polygonFeaturesValidator, deduplicator)`

Creates a new OSM data to GeoJSON converter. You can use some optional arguments to configure the converter.
* `verbose`: If true, the converter will print conversion details to stdout during execution. default: false
* `flatProperties`: If true, the resulting GeoJSON feature's properties will be a simple key-value list instead of a structured json object (with separate tags and metadata). default: false
* `uninterestingTags` (non-nullable): A [blacklist](https://github.com/jwtue/osmtogeojson-java/blob/3.0.0-beta.5/osmtogeojson-java/src/main/kotlin/de/jonaswolf/osmtogeojson/OsmToGeoJson.kt#L20-L28) of tag keys. Will be used to decide if a feature is *interesting* enough for its own GeoJSON feature.
* `uninterestingTagsValidator` (nullable): A callback function object.  Will be used to decide if a feature is *interesting* enough for its own GeoJSON feature. Callback function has preference over blacklist.
* `additionalPolygonFeatures` (nullable): A json object that is merged with the default [json object](https://github.com/jwtue/osmtogeojson-java/blob/3.0.0-beta.5/osmtogeojson-java/src/main/kotlin/de/jonaswolf/osmtogeojson/options/OsmPolygonFeatures.kt) and is used to determine if a closed way should be treated as a Polygon or LineString. [read more](https://wiki.openstreetmap.org/wiki/Overpass_turbo/Polygon_Features)
* `polygonFeaturesValidator` (nullable): A callback function object. Will be used to determine if a closed way should be treated as a Polygon or LineString. Callback function has preference over feature list.
* `deduplicator`: A deduplicator function object. Can be used to override [default deduplication rules](https://github.com/jwtue/osmtogeojson-java/blob/3.0.0-beta.5/osmtogeojson-java/src/main/kotlin/de/jonaswolf/osmtogeojson/options/DefaultDeduplicator.kt).

### `osmToGeoJson.convertOverpassJsonToGeoJson(data, featureCallback)`
* `data`: the OSM data in [OSM JSON](https://overpass-api.de/output_formats.html#json).
* `featureCallback`: Optional/nullable. A callback function that will be called for every feature that is created during conversion.

### `osmToGeoJson.convertOsmXmlToGeoJson(data, featureCallback)`
* `data`: the OSM data as a XML DOM.
* `featureCallback`: Optional/nullable. A callback function that will be called for every feature that is created during conversion.

More information
---
For more information on the produced GeoJSON and the way this library works, check out the documentation at [tyrasd/osmtogeojson](https://github.com/tyrasd/osmtogeojson).