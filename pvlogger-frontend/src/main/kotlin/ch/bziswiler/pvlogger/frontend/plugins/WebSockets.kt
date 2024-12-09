package ch.bziswiler.pvlogger.frontend.plugins

import ch.bziswiler.pvlogger.shared.job.tags.KpisCreatedMessageTag
import ch.bziswiler.pvlogger.shared.messaging.MessageHandler
import ch.bziswiler.pvlogger.shared.messaging.MessageReceiver
import ch.bziswiler.pvlogger.shared.messaging.PvLoggerKpiData
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
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import org.koin.java.KoinJavaComponent.inject
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneOffset
import javax.jms.Message
import javax.jms.ObjectMessage

fun Application.configureWebSockets() {
    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(30)
        maxFrameSize = Long.MAX_VALUE
        masking = false
        contentConverter = KotlinxWebsocketSerializationConverter(Json)
    }
    // https://lankydan.dev/ktor-websockets
    routing {
        webSocket("/$FRONTEND_READ_LIVE_DATA_VALUES_PATH") {
            val kpiMsgReceiver by inject<MessageReceiver>(MessageReceiver::class.java)
            kpiMsgReceiver.addMessageHandler(KpiMessageHandler() { data: KpiValuesDto ->
                launch { sendSerialized(data) }
            })
        }
    }
}

internal class KpiMessageHandler(private val sendData: (data: KpiValuesDto) -> Unit) : MessageHandler {

    override val messageSelector = KpisCreatedMessageTag().toSelector()

    private val logger = KotlinLogging.logger {}

    override fun onMessage(message: Message?) = when (message) {
        is ObjectMessage -> handleObjectMessage(message)
        else -> logger.info { "message is not an 'object' message! -> $message" }
    }

    private fun handleObjectMessage(message: ObjectMessage) = when (val obj = message.`object`) {
        is PvLoggerKpiData -> sendData(obj.toKpiValuesDto()).let { logger.info { "KPIs stored: '$obj'" } }
        else -> logger.info { "message.object is not 'pv logger kpi data'! -> '$obj'" }
    }
}

private fun PvLoggerKpiData.toKpiValuesDto(): KpiValuesDto = KpiValuesDto(
    LocalDateTime.now().toEpochSecond(ZoneOffset.UTC),
    this.producing,
    gridPower = this.exporting + this.selfConsuming,
    this.selfConsuming,
    this.importing,
    this.exporting,
    this.produced,
    this.consuming,
    this.selfConsumed,
    this.exported,
    this.imported,
    this.consumed
)