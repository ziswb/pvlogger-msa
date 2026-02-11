plugins {
    alias(libs.plugins.jvm)
    id("idea")
}

group = "ch.bziswiler"
version = "0.0.1"

idea {
    module {
        isDownloadJavadoc = true
        isDownloadSources = true
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Logging
    implementation(libs.kotlin.logging)

    // Database
    implementation(libs.exposed.core)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.java.time)
    implementation(libs.exposed.jdbc)
    implementation(libs.postgresql)
    implementation(libs.hikaricp)

    // DB migrations
    implementation(libs.flywaydb)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}
