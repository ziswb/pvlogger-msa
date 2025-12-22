package ch.bziswiler.pvlogger.frontend.plugins

import ch.bziswiler.pvlogger.shared.rest.FrontendPaths.FRONTEND_READ_LIVE_DATA_VALUES_PATH
import ch.bziswiler.pvlogger.shared.rest.KpiValuesDto
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.pingPeriod
import io.ktor.server.websocket.sendSerialized
import io.ktor.server.websocket.timeout
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import java.time.Duration
import java.time.Duration.ofSeconds

private val logger = KotlinLogging.logger {}
private val Int.Seconds: Duration get() = ofSeconds(toLong())

fun Application.configureWebSockets() {
    // https://lankydan.dev/ktor-websockets
    install(WebSockets) {
        pingPeriod = 15.Seconds
        timeout = 15.Seconds
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

    val flow = MutableSharedFlow<KpiValuesDto>()
    val sharedFlow = flow.asSharedFlow()

    routing {
        webSocket("/$FRONTEND_READ_LIVE_DATA_VALUES_PATH") {
            val job =
                launch {
                    sharedFlow.collect { kpiDto ->
                        logger.debug { "WebSocket: Sending KPI values DTO '$kpiDto'...'" }
                        sendSerialized(kpiDto)
                        logger.info { "WebSocket: KPI values sent!" }
                    }
                }
            runCatching {
                incoming.consumeEach { frame ->
                    when (frame) {
                        is Frame.Close -> logger.debug { "WebSocket: Closing websocket" }.also { job.cancel() }
                        else -> logger.debug { "WebSocket: Received frame '$frame'" }
                    }
                }
            }.onFailure { exception ->
                logger.info(exception) { "WebSocket: Received exception" }
            }.also {
                job.cancel()
            }
        }
    }
}
