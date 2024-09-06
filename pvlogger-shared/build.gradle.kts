plugins {
    alias(libs.plugins.jvm)
    alias(libs.plugins.serialization)
}

group = "ch.bziswiler.pvlogger"
version = "0.0.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.kotlinx.serialization.json)

    // Messaging
    implementation(libs.activemq.core)

    // This dependency is exported to consumers, that is to say found on their compile classpath.
    // api(libs.commons.math3)
    // This dependency is used internally, and not exposed to consumers on their own compile classpath.
    // implementation(libs.guava)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}
