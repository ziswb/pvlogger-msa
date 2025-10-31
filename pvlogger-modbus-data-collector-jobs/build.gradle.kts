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
    mainClass.set("ch.bziswiler.pvlogger.datacollector.jobs.ApplicationKt")
}

kotlin {
    jvmToolchain(21)
}

repositories {
    mavenCentral()
}

dependencies {
    // Digitalpetri Modbus TCP for Modbus communication
//    implementation(libs.digitalpetri.modbus.tcp)

    // Okio for in-memory encode/decode
    implementation(libs.okio)

    // Retrofit HTTP client
    implementation(libs.retrofit)

    // Kotlinx serialization
    implementation(libs.kotlinx.serialization.json)
    // Coroutines
    implementation(libs.kotlinx.coroutines)

    // Logging
    implementation(libs.logback.classic)
    implementation(libs.kotlin.logging)

    // Shared pvlogger-resources
    implementation(project(":pvlogger-shared"))

    // Shared modbus-resources
    implementation(project(":pvlogger-modbus-tcp-api"))

    // Dependency Injection (Kodein)
    implementation(libs.kodein.di)

    // Monitoring
    implementation(libs.micrometer.registry.prometheus)
    implementation(libs.prometheus.pushgateway)

    // Config (Lightbend Typesafe Config)
    implementation(libs.lightbend.config)
    implementation(libs.config4k)

    testImplementation(libs.kotlin.test.junit)
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
