plugins {
    alias(libs.plugins.jvm)
    application
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
    mainClass.set("ch.bziswiler.pvlogger.datacollector.ApplicationKt")
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

    // Ktor client/network used by this module
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.logging)
    implementation(libs.ktor.serialization.kotlinx.json)
    // Remove Ktor server dependencies (no server startup)
    // implementation(libs.ktor.server.call.logging)
    // implementation(libs.ktor.server.core)
    // implementation(libs.ktor.server.netty)

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
}
