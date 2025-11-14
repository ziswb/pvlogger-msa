plugins {
    alias(libs.plugins.jvm)
    alias(libs.plugins.serialization)
    application
    idea
}

group = "ch.bziswiler"

idea {
    module {
        isDownloadJavadoc = true
        isDownloadSources = true
    }
}

application {
    mainClass.set("ch.bziswiler.pvlogger.datacollector.ApplicationKt")
}

kotlin {
    jvmToolchain(21)
}

repositories {
    mavenCentral()
}

dependencies {
    // Retrofit HTTP client
    implementation(libs.retrofit)

    // Okio for in-memory encode/decode
    implementation(libs.okio)

    // Coroutines
    implementation(libs.kotlinx.coroutines)

    // Kotlinx serialization (pure Kotlin)
    implementation(libs.kotlinx.serialization.json)

    // Logging
    implementation(libs.logback.classic)
    implementation(libs.kotlin.logging)

    // Shared pvlogger-resources
    implementation(project(":pvlogger-shared"))

    // Testing
    testImplementation(libs.kotlin.test)
}

tasks.test {
    useJUnitPlatform()
}
