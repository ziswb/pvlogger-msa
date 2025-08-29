plugins {
    alias(libs.plugins.jvm)
    alias(libs.plugins.ktor) // Ktor Gradle plugin (marked)
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
    mainClass.set("io.ktor.server.netty.EngineMain") // Ktor Netty engine main class (marked)
    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment") // Ktor development flag (marked)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
    maven { url = uri("https://mvnrepository.com/artifact/") }
}

dependencies {
    // Messaging
    implementation(libs.activemq.core)
    implementation(libs.activemq.client)

    // Ktor
    implementation(libs.ktor.client.cio) // Ktor dependency (marked)
    implementation(libs.ktor.client.content.negotiation) // Ktor dependency (marked)
    implementation(libs.ktor.client.core) // Ktor dependency (marked)
    implementation(libs.ktor.client.logging) // Ktor dependency (marked)
    implementation(libs.ktor.serialization.kotlinx.json) // Ktor dependency (marked)
    implementation(libs.ktor.server.call.logging) // Ktor dependency (marked)
    implementation(libs.ktor.server.core) // Ktor dependency (marked)
    implementation(libs.ktor.server.netty) // Ktor dependency (marked)

    // Logging
    implementation(libs.logback.classic)
    implementation(libs.kotlin.logging)

    // Shared pvlogger-resources
    implementation(project(":pvlogger-shared"))

    // Dependency Injection (Kodein)
    implementation(libs.kodein.di)

    // Monitoring
    implementation(libs.micrometer.registry.prometheus)
    implementation(libs.prometheus.pushgateway)

    // Config (Lightbend Typesafe Config)
    implementation(libs.lightbend.config)

    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.ktor.server.tests)
}
