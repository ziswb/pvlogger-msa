plugins {
    alias(libs.plugins.jvm)
    idea
}

group = "ch.bziswiler"
version = "0.0.1"

idea {
    module {
        isDownloadJavadoc = true
        isDownloadSources = true
    }
}

kotlin {
    jvmToolchain(21)
}

repositories {
    mavenCentral()
}

dependencies {
    // Kotlinx serialization
    implementation(libs.kotlinx.serialization.json)

    // Okio for in-memory encode/decode
    implementation(libs.okio)
}

tasks.test {
    useJUnitPlatform()
}
