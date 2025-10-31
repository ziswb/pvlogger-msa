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
    // Modbus API
    implementation(project(":pvlogger-modbus-tcp-api"))

    // Okio for in-memory encode/decode
    implementation(libs.okio)

    // Kotlinx serialization
    implementation(libs.kotlinx.serialization.json)
    // Coroutines
    implementation(libs.kotlinx.coroutines)

    // Logging
    implementation(libs.logback.classic)
    implementation(libs.kotlin.logging)

    // Config (Lightbend Typesafe Config)
    implementation(libs.lightbend.config)
    implementation(libs.config4k)

    testImplementation(libs.kotlin.test.junit)
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
