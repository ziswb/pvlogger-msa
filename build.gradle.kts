plugins {
    alias(libs.plugins.jvm)
    alias(libs.plugins.ktor) apply false
    idea
}

tasks.register<Exec>("makeGoBuild") {
    group = "build"
    description = "Builds all Go modules recursively via the top-level Makefile."
    workingDir = rootProject.projectDir
    // Ensure the task always runs even if Gradle thinks nothing changed
    outputs.upToDateWhen { false }

    commandLine("make", "build")
}

tasks.register<Exec>("makeGoClean") {
    group = "build"
    description = "Cleans Go artifacts by calling the top-level Makefile."
    workingDir = rootProject.projectDir
    // Ensure Gradle doesn't try to skip this
    outputs.upToDateWhen { false }

    commandLine("make", "clean")
}

tasks.register<Exec>("makeGoLint") {
    group = "build"
    description = "Lints the Go code by calling the top-level Makefile."
    workingDir = rootProject.projectDir
    // Ensure Gradle doesn't try to skip this
    outputs.upToDateWhen { false }

    commandLine("make", "lint")
}

// Hook into the standard lifecycle
tasks.named("build") { dependsOn("makeGoBuild") }
tasks.named("clean") { dependsOn("makeGoClean") }
