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
}

dependencies {
    // Messaging
    implementation(libs.activemq.core)
    implementation(libs.activemq.client)

    // Digitalpetri Modbus TCP for Modbus communication
//    implementation(libs.digitalpetri.modbus.tcp)

    // Ktor IO channels (non-network), used for in-memory encode/decode
    implementation(libs.ktor.io)

    // Retrofit HTTP client (replacing Ktor HTTP client)
    implementation(libs.retrofit)

    // Kotlinx serialization (pure Kotlin)
    implementation(libs.kotlinx.serialization.json)

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
