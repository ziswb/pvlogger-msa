package ch.bziswiler.pvlogger.frontend.service

import ch.bziswiler.pvlogger.shared.rest.KpiValuesDto
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.shareIn
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private val logger = KotlinLogging.logger {}

/**
 * Polls the data accessor for the latest KPI row and broadcasts it to every
 * connected `/livedata` WebSocket session.
 *
 * The polling loop is owned by [scope] (the application, not a client session)
 * and is reference-counted by `SharingStarted.WhileSubscribed`:
 *
 * - the first subscriber starts it and gets a value immediately,
 * - a later subscriber gets the cached last value at once (`replay = 1`) and
 *   then shares the same 60 s tick as everybody else,
 * - the last subscriber leaving stops it again, so an idle dashboard causes no
 *   traffic between the frontend and the data accessor.
 */
class LiveKpiPublisher(
    private val source: LatestKpiSource,
    scope: CoroutineScope,
    private val pollInterval: Duration = DEFAULT_POLL_INTERVAL,
    private val retryDelay: Duration = DEFAULT_RETRY_DELAY,
    stopTimeout: Duration = DEFAULT_STOP_TIMEOUT,
    replayExpiration: Duration = DEFAULT_POLL_INTERVAL,
) {
    /**
     * The latest KPI row, refreshed every [pollInterval] while at least one
     * session collects it. Collect it conflated: a session that cannot keep up
     * must not stall the shared upstream and thereby starve the others.
     */
    val latestKpis: SharedFlow<KpiValuesDto> =
        poll().shareIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(
                stopTimeoutMillis = stopTimeout.inWholeMilliseconds,
                replayExpirationMillis = replayExpiration.inWholeMilliseconds,
            ),
            replay = 1,
        )

    private fun poll(): Flow<KpiValuesDto> =
        flow {
            while (true) {
                logger.debug { "LiveKpiPublisher: polling latest KPIs..." }
                source.retrieveLatestKpis()?.let { emit(it) }
                delay(pollInterval)
            }
        }.retryWhen { cause, attempt ->
            // Never let a hiccup of the data accessor end the shared flow: an
            // ended flow would leave every connected session silent for good.
            logger.warn(cause) { "LiveKpiPublisher: poll failed (attempt $attempt), retrying in $retryDelay" }
            delay(retryDelay)
            true
        }

    companion object {
        /** Matches the per-minute cadence the fast collector writes at (ADR-0016). */
        val DEFAULT_POLL_INTERVAL = 60.seconds

        /** Grace period before an idle publisher stops, so a page reload does not restart it. */
        val DEFAULT_STOP_TIMEOUT = 5.seconds

        val DEFAULT_RETRY_DELAY = 10.seconds
    }
}
