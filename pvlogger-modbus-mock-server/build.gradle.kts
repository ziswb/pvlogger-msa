import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.tasks.testing.logging.TestExceptionFormat

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

    // Logging
    implementation(libs.logback.classic)
    implementation(libs.kotlin.logging)

    implementation(project(":pvlogger-shared-ktor"))
    implementation(project(":pvlogger-shared-model"))
    implementation(project(":pvlogger-shared-rest"))

    // Cache -> https://github.com/ReactiveCircus/cache4k
    implementation(libs.reactivecircus.cache)

    // Testing
    testImplementation(platform(libs.junit.bom))
    testRuntimeOnly(libs.junit.platform.launcher)
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

// Shadow 9.x (via Ktor 3.x) drops duplicate resources/service files during
// buildFatJar without these — see TROUBLESHOOTING.md
tasks.withType<ShadowJar>() {
    mergeServiceFiles()
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}
