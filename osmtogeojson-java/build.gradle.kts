plugins {
    kotlin("jvm") version "1.9.20"
    application
    `java-library`
    base
}

group = "de.jonaswolf"
version = "3.0.0-beta.5"

repositories {
    mavenCentral()
}

dependencies {
    // https://mvnrepository.com/artifact/org.json/json
    implementation("org.json:json:20231013")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "de.jonaswolf.osmtogeojson.Main"
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    from(sourceSets["main"].allSource)
}