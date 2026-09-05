package ch.bziswiler.pvlogger.frontend.plugins

import ch.bziswiler.pvlogger.frontend.configuration.AppConfig
import ch.bziswiler.pvlogger.frontend.rest.DataAccessorRestProxy
import ch.bziswiler.pvlogger.frontend.rest.FrontendPaths.FRONTEND_READ_LIVE_DATA_VALUES_PATH
import ch.bziswiler.pvlogger.frontend.service.LiveKpiPublisher
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.pingPeriod
import io.ktor.server.websocket.sendSerialized
import io.ktor.server.websocket.timeout
import io.ktor.server.websocket.webSocket
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.koin.ktor.ext.inject
import kotlin.time.Duration.Companion.seconds

private val logger = KotlinLogging.logger {}

fun Application.configureWebSockets() {
    install(WebSockets) {
        pingPeriod = 15.seconds
        timeout = 15.seconds
        maxFrameSize = Long.MAX_VALUE
        masking = false
        contentConverter = KotlinxWebsocketSerializationConverter(
            Json {
                encodeDefaults = true
                ignoreUnknownKeys = true
                isLenient = true
                prettyPrint = true
            }
        )
    }

    val config by inject<AppConfig>()
    // Scoped to the application, not to a session: the polling loop must
    // outlive the client that happened to start it.
    val publisher = LiveKpiPublisher(DataAccessorRestProxy(config.dataAccessorConfig), scope = this)

    routing {
        webSocket("/$FRONTEND_READ_LIVE_DATA_VALUES_PATH") {
            logger.info { "WebSocket: session opened" }
            val sender = launch {
                publisher.latestKpis
                    .conflate()
                    .collect { kpiDto ->
                        logger.debug { "WebSocket: sending KPI values DTO '$kpiDto'..." }
                        sendSerialized(kpiDto)
                        logger.info { "WebSocket: KPI values sent!" }
                    }
            }
            try {
                // Nothing is expected from the client; draining `incoming` is
                // how this session learns that the client went away.
                incoming.consumeEach { frame -> logger.debug { "WebSocket: received frame '$frame'" } }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                logger.error(e) { "WebSocket: session failed" }
            } finally {
                sender.cancel()
                logger.info { "WebSocket: session closed" }
            }
        }
    }
}
