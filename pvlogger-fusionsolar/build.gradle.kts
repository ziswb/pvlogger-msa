plugins {
    alias(libs.plugins.jvm)
    alias(libs.plugins.serialization)
    alias(libs.plugins.openapi)
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
    mainClass.set("ch.bziswiler.pvlogger.fusionsolar.ApplicationKt")
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

    // Coroutines
    implementation(libs.kotlinx.coroutines)

    // Kotlinx serialization (pure Kotlin)
    implementation(libs.kotlinx.serialization.json)

    // Logging
    implementation(libs.logback.classic)
    implementation(libs.kotlin.logging)

    // Shared pvlogger-resources
    implementation(project(":pvlogger-shared"))

    // Database
    implementation(libs.exposed.core)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.java.time)
    implementation(libs.exposed.jdbc)
    implementation(libs.postgresql)
    implementation(libs.hikaricp)

    // DB migrations
    implementation(libs.flywaydb)

    // In-memory DB for testing
    implementation(libs.h2)

    // Kotlin DateTime
    implementation(libs.kotlinx.datetime)

    // Testing
    testImplementation(libs.kotlin.test)
}
sourceSets.getByName("main") {
    java.srcDir("src/gen/kotlin")
}

tasks.test {
    useJUnitPlatform()
}
