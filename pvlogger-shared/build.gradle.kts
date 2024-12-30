plugins {
    alias(libs.plugins.jvm)
    alias(libs.plugins.serialization)
//  alias(libs.plugins.idea)
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
    implementation(libs.kotlinx.serialization.json)

    // Logging
    implementation(libs.kotlin.logging)

    // Ktor
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.logging)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)

    // Messaging
    implementation(libs.activemq.core)

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
