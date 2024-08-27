plugins {
    alias(libs.plugins.jvm)
    alias(libs.plugins.ktor)
    idea
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

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.activemq.core)
    implementation(libs.activemq.client)

    implementation(libs.koin.ktor)
    implementation(libs.koin.logger.slf4j)

    implementation(libs.kotlin.logging)

    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.logging)

    implementation(libs.ktor.serialization.kotlinx.json)

    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.metrics.micrometer)
    implementation(libs.ktor.server.netty)

    implementation(libs.logback.classic)
    implementation(libs.micrometer.registry.prometheus)

    implementation(project(":pvlogger-shared"))

    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.ktor.server.tests)
}
