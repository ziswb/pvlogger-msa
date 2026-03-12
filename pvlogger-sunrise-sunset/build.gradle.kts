import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    alias(libs.plugins.jvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.serialization)
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
    // Ktor
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.client.logging)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.metrics.micrometer)
    implementation(libs.ktor.server.netty)

    // Logging
    implementation(libs.logback.classic)
    implementation(libs.kotlin.logging)

    // Shared pvlogger-resources
    implementation(project(":pvlogger-shared-database"))

    // Koin
    implementation(libs.koin.ktor)
    implementation(libs.koin.logger.slf4j)

    // Monitoring
    implementation(libs.micrometer.registry.prometheus)

    // Database
    implementation(libs.exposed.core)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.java.time)
    implementation(libs.exposed.jdbc)
    implementation(libs.postgresql)
    implementation(libs.hikaricp)

    // DB migrations
    implementation(libs.flywaydb)

    // Testing
    testImplementation(libs.kotlin.test)
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        exceptionFormat = TestExceptionFormat.FULL
        events("passed", "skipped", "failed")
    }
}

// Necessary due to a bug in flyway 10.x. See: https://github.com/flyway/flyway/issues/3811
// Symptom was: no migrations were found when application was run from (shadow) jar
tasks.withType<ShadowJar>() {
    mergeServiceFiles()
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}
