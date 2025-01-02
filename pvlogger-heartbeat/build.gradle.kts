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

// Necessary due to a bug in flyway 10.x. See: https://github.com/flyway/flyway/issues/3811
// Symptom was: no migrations were found when application was run from (shadow) jar
tasks {
    shadowJar {
        mergeServiceFiles()
    }
}

dependencies {
    // Messaging
    implementation(libs.activemq.core)
    implementation(libs.activemq.client)

    // persistence accessor
    implementation(libs.exposed.core)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.jdbc)

    // DB migrations
    implementation(libs.flywaydb)

    // In-memory DB for testing
    implementation(libs.h2)

    // Dependency injection
    implementation(libs.koin.ktor)
    implementation(libs.koin.logger.slf4j)

    // Logging
    implementation(libs.kotlin.logging)
    implementation(libs.ktor.client.logging)
    implementation(libs.logback.classic)

    // Serialization
    implementation(libs.ktor.serialization.kotlinx.json)

    // Ktor
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.metrics.micrometer)
    implementation(libs.ktor.server.netty)

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
