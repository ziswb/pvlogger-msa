package ch.bziswiler.pvlogger.shared.ktor

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped

private val logger = KotlinLogging.logger {}

/**
 * Closes [resource] when the application has stopped (security finding 29).
 *
 * ApplicationStopped, not ApplicationStopping: by then the engine has stopped
 * taking requests and in-flight ones are done, so nothing still uses the
 * resource. A failing close is logged and does not block the rest of shutdown.
 */
fun Application.closeOnShutdown(resource: AutoCloseable) {
    monitor.subscribe(ApplicationStopped) {
        try {
            resource.close()
            logger.info { "Closed ${resource::class.simpleName} on shutdown" }
        } catch (e: Exception) {
            logger.error(e) { "Closing ${resource::class.simpleName} on shutdown failed" }
        }
    }
}
