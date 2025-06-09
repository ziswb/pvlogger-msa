plugins {
    alias(libs.plugins.jvm)
    alias(libs.plugins.serialization)
    id("idea")
}

group = "ch.bziswiler.pvlogger.mudbus.tcp.api"
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
    implementation(project(":pvlogger-shared"))
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}