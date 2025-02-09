package ch.bziswiler.pvlogger.frontend.plugins

import ch.bziswiler.pvlogger.shared.job.tags.KpisCreatedMessageTag
import ch.bziswiler.pvlogger.shared.messaging.MessageHandler
import ch.bziswiler.pvlogger.shared.messaging.MqReceiverComposer
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
import io.ktor.websocket.Frame
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import org.koin.java.KoinJavaComponent.inject
import java.time.Duration
import java.time.Duration.ofSeconds
import javax.jms.Message
import javax.jms.ObjectMessage

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
    val composer by inject<MqReceiverComposer>(MqReceiverComposer::class.java)

    composer.addMessageHandler(KpiMessageHandler(flow))
    composer.compose()

    routing {
        webSocket("/$FRONTEND_READ_LIVE_DATA_VALUES_PATH") {
            val job = launch {
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

internal class KpiMessageHandler(private val sharedFlow: MutableSharedFlow<KpiValuesDto>) : MessageHandler {

    override val messageSelector = KpisCreatedMessageTag().toSelector()

    override fun onMessage(message: Message?) = when (message) {
        is ObjectMessage -> handleObjectMessage(message)
        else -> logger.info { "message is not an 'object' message! -> $message" }
    }

    private fun handleObjectMessage(message: ObjectMessage) = when (val obj = message.`object`) {
        is PvLoggerKpiData -> runBlocking { logger.info { "New KPIs received: '$obj'" }.let { sharedFlow.emit(obj.toKpiValuesDto()) } }
        else -> logger.info { "message.object is not 'pv logger kpi data'! -> '$obj'" }
    }
}


private fun PvLoggerKpiData.toKpiValuesDto(): KpiValuesDto = KpiValuesDto(
    createdAt = this.dateCreated,
//val solarPower: Double = producing
    solarPower = this.producing.toKW().kw,
// gridPower = gridToHousePower - solarToGridPower
    gridPower = (this.importing - this.exporting).toKW().kw,
//val solarToGridPower: Double = exporting
    solarToGridPower = this.exporting.toKW().kw,
//val solarToHousePower: Double = selfConsuming
    solarToHousePower = this.selfConsuming.toKW().kw,
//val gridToHousePower: Double = importing
    gridToHousePower = this.importing.toKW().kw,
//val ownConsumption: Double = consuming
    ownConsumption = this.consuming.toKW().kw,
//val dailyEnergyYield: Double = produced
    dailyEnergyYield = this.produced.toKWh().kWh,
//val dailyEnergySelfCovered: Double = selfConsumed
    dailyEnergySelfCovered = this.selfConsumed.toKWh().kWh,
//val dailyEnergyExported: Double = exported
    dailyEnergyExported = this.exported.toKWh().kWh,
//val dailyEnergyImported: Double = imported
    dailyEnergyImported = this.imported.toKWh().kWh,
//val dailyEnergyConsumed: Double = consumed
    dailyEnergyConsumed = this.consumed.toKWh().kWh
)
