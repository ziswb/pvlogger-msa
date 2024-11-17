plugins {
    alias(libs.plugins.jvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.serialization)
//  alias(libs.plugins.idea)
    id("idea")
}

group = "ch.bziswiler"
version = "0.0.1"

application {
    mainClass.set("ch.bziswiler.pvlogger.dataaccessor.ApplicationKt")
    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

idea {
    module {
        isDownloadJavadoc = true
        isDownloadSources = true
    }
}

dependencies {
    implementation(libs.activemq.core)
    implementation(libs.activemq.client)

    // Serialization
    implementation(libs.ktor.serialization.kotlinx.json)

    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.call.id)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.metrics.micrometer)
    implementation(libs.ktor.server.netty)

    implementation(project(":pvlogger-shared"))

    // Monitoring
    implementation(libs.micrometer.registry.prometheus)

    // Logging
    implementation(libs.logback.classic)
    implementation(libs.kotlin.logging)

    // Database
    implementation(libs.exposed.core)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.java.time)
    implementation(libs.exposed.jdbc)
    implementation(libs.postgresql)
    implementation(libs.hikaricp)
    // This way the application can be launched using a H2-configuration
    implementation(libs.h2)

    // Koin
    implementation(libs.koin.ktor)
    implementation(libs.koin.logger.slf4j)

    // Testing
    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.ktor.server.tests)
}
