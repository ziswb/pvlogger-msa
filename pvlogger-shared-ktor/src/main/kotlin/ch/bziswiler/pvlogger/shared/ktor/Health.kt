package ch.bziswiler.pvlogger.shared.ktor

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

const val HEALTH_LIVE_PATH = "/health/live"
const val HEALTH_READY_PATH = "/health/ready"

private val logger = KotlinLogging.logger {}

/** A dependency the service needs to serve requests: returns when it is usable, throws otherwise. */
fun interface ReadinessCheck {
    suspend fun check()
}

/**
 * The probe endpoints (backlog 60). [HEALTH_LIVE_PATH] answers while the
 * process can serve HTTP at all; a failing liveness probe restarts the pod, so
 * it checks nothing else. [HEALTH_READY_PATH] runs [checks], each bounded by
 * [timeout]; a failure takes the pod out of its Service until the next probe
 * passes. The 503 body names only the failed checks; the reason goes to the
 * log. Neither path is exposed by the ingresses (ADR-0020).
 */
fun Application.configureHealth(
    checks: Map<String, ReadinessCheck> = emptyMap(),
    timeout: Duration = 3.seconds,
) {
    routing {
        get(HEALTH_LIVE_PATH) { call.respondText("ok") }
        get(HEALTH_READY_PATH) {
            val failed =
                checks.mapNotNull { (name, check) ->
                    try {
                        withTimeout(timeout) { check.check() }
                        null
                    } catch (e: TimeoutCancellationException) {
                        logger.warn { "Readiness check '$name' timed out after $timeout" }
                        name
                    } catch (e: CancellationException) {
                        // The request itself was cancelled: not the check's failure.
                        throw e
                    } catch (e: Exception) {
                        logger.warn(e) { "Readiness check '$name' failed" }
                        name
                    }
                }
            if (failed.isEmpty()) {
                call.respondText("ok")
            } else {
                call.respondText("not ready: ${failed.joinToString()}", status = HttpStatusCode.ServiceUnavailable)
            }
        }
    }
}
