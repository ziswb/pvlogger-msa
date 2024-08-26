import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    alias(libs.plugins.jvm)
    alias(libs.plugins.ktor)
    id("idea")
//    alias(libs.plugins.idea)
}

group = "ch.bziswiler"
version = "0.0.1"

idea {
    module {
        isDownloadJavadoc = true
        isDownloadSources = true
    }
}

application {
    mainClass.set("io.ktor.server.netty.EngineMain")
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

dependencies {
    // Messaging
    implementation(libs.activemq.core)
    implementation(libs.activemq.client)

    // persistence accessor
    implementation(libs.exposed.core)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.jdbc)

    // In-memory DB for testing
    implementation(libs.h2)

    // Dependency injection
    implementation(libs.koin.ktor)
    implementation(libs.koin.logger.slf4j)

    // Logging
    implementation(libs.kotlin.logging)
    implementation(libs.ktor.client.logging)
    implementation(libs.logback.classic)

    // Ktor
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.metrics.micrometer)
    implementation(libs.ktor.server.netty)

    // Needed in order to recognize the subprojects serializable annotations
    implementation(libs.ktor.serialization.jackson)

    // Shared pvlogger-resources
    implementation(project(":pvlogger-shared"))

    // Misc
    implementation(libs.micrometer.registry.prometheus)
    implementation(libs.postgresql)
    implementation(libs.quartz)

    // Testing
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.koin.test)
    testImplementation(libs.koin.test.junit5)
    testImplementation(libs.kotlin.test.junit)
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        exceptionFormat = TestExceptionFormat.FULL
        events("passed", "skipped", "failed")
    }
}
