import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    alias(libs.plugins.jvm)
    alias(libs.plugins.ktor)
    idea
//    alias(libs.plugins.idea)
    id("org.owasp.dependencycheck") version "11.1.1"
}

buildscript {
    dependencies {
        classpath("org.owasp.dependencycheck:org.owasp.dependencycheck.gradle.plugin:11.1.1")
    }
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
    mainClass.set("ch.bziswiler.pvlogger.modbus.mockserver.ApplicationKt")
    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    mavenCentral()
}

dependencies {

    // Ktor
    implementation(libs.ktor.network)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.logging)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)

    // Logging
    implementation(libs.logback.classic)
    implementation(libs.kotlin.logging)

    implementation(project(":pvlogger-shared-model"))

    // Cache -> https://github.com/ReactiveCircus/cache4k
    implementation(libs.reactivecircus.cache)

    // Testing
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlin.test)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        exceptionFormat = TestExceptionFormat.FULL
        events("passed", "skipped", "failed")
    }
}
