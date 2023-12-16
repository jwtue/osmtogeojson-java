osmtogeojson-java
===================

Converts [OSM](https://www.openstreetmap.org/) [data](https://wiki.openstreetmap.org/wiki/OSM_XML) to [GeoJSON](https://geojson.org/). 

This is a Kotlin / Java / Android implementation of the Javascript/Node.js library [osmtogeojson](https://github.com/tyrasd/osmtogeojson).

All features from the original library are available, including command-line interface.

As the implementation relies heavily on `org.json`, the JAR library is unfortunately incompatible with Android. Thus, a version with minor adaptions for Android is supplied as well.