# Troubleshooting Guide

This document captures known issues, their root causes, and solutions encountered in the PVLogger MSA project.

## Ktor 3.x + Flyway Migration Issue

**Problem:** After upgrading from Ktor 2.x to 3.x (specifically 3.4.0), Flyway migrations fail to run in Docker containers. Flyway cannot find migration files even though they exist in `src/main/resources/db/migration/`.

**Symptoms:**
- Application starts but Flyway reports no migrations found
- Migration files are present in the source code but not accessible at runtime
- Issue only occurs when running from fat JAR (Docker containers)

**Root Cause:** Ktor 3.x updated the shadow plugin (to 9.1.0+), which changed how duplicate resources and service files are handled during fat JAR assembly via the `buildFatJar` task.

**Solution:** Update the `shadowJar` task configuration in all JVM service `build.gradle.kts` files:

```kotlin
tasks {
    shadowJar {
        mergeServiceFiles()
        duplicatesStrategy = DuplicatesStrategy.INCLUDE  // Add this line
    }
}
```

**Affected Services:**
- `pvlogger-modbus-data-accessor`
- `pvlogger-masterdata-server`
- `pvlogger-sunrise-sunset`

**Context:** The existing `mergeServiceFiles()` configuration was a workaround for Flyway 10.x bug ([issue #3811](https://github.com/flyway/flyway/issues/3811)). With Ktor 3.x, this alone is insufficient. Both `mergeServiceFiles()` and `duplicatesStrategy = DuplicatesStrategy.INCLUDE` are required.

**Fixed in:** Commit following dependency upgrade to Ktor 3.4.0, Exposed 1.0.0, Kotlin 2.2.20

---

## Contributing to This Guide

When encountering and resolving issues:
1. Document the problem with clear symptoms
2. Explain the root cause if known
3. Provide the complete solution with code examples
4. List affected components
5. Reference relevant external issues/documentation
