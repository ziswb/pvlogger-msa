plugins {
    alias(libs.plugins.jvm)
    alias(libs.plugins.ktor)
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
    // Logging
    implementation(libs.logback.classic)
    implementation(libs.kotlin.logging)

    // Shared pvlogger-resources
    implementation(project(":pvlogger-shared"))
    implementation(project(":pvlogger-modbus-tcp-api"))

    // Dependency Injection
    implementation("org.kodein.di:kodein-di:7.26.1")

    // Kotlin
    implementation("org.jetbrains.kotlinx:kotlinx-io-core:0.7.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

    // Configuration
    implementation("com.sksamuel.hoplite:hoplite-core:2.9.0")
    implementation("com.sksamuel.hoplite:hoplite-yaml:2.9.0")

//    implementation("io.github.sksamuel.hoplite:hoplite-core:2.8.4")
//    implementation("io.github.sksamuel.hoplite:hoplite-yaml:2.8.4")

    // Testing
    testImplementation(libs.kotlin.test.junit)
    testImplementation("io.github.sksamuel.hoplite:hoplite-testing:2.8.4")
}
