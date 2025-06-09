plugins {
    alias(libs.plugins.jvm)
    idea
}

group = "ch.bziswiler.pvlogger.mudbus.base"
version = "0.0.1"

idea {
    module {
        isDownloadJavadoc = true
        isDownloadSources = true
    }
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
    implementation(libs.kotlin.logging)
    implementation("org.jetbrains.kotlinx:kotlinx-io-core:0.7.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    // Shared pvlogger-resources
    implementation(project(":pvlogger-modbus-tcp-api"))

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}