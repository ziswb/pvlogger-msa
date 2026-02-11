plugins {
    alias(libs.plugins.jvm)
    alias(libs.plugins.serialization)
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

    // Ktor
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.logging)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}
