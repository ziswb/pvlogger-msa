package ch.bziswiler.pvlogger.frontend.plugins

import ch.bziswiler.pvlogger.frontend.configuration.AppConfig
import ch.bziswiler.pvlogger.frontend.rest.DataAccessorRestProxy
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.time.delay
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import org.koin.ktor.ext.inject
import java.time.Duration
import java.time.Duration.ofSeconds
import java.util.concurrent.Executors
import kotlin.coroutines.CoroutineContext

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
    val config by inject<AppConfig>()
    val dataAccessorRestProxy = DataAccessorRestProxy(config.dataAccessorConfig)
    val notificationProducer = NotificationProducer(flow, dataAccessorRestProxy)

    routing {
        webSocket("/$FRONTEND_READ_LIVE_DATA_VALUES_PATH") {
            val delay = 60.Seconds
            val notificationProducerScheduler = NotificationProducerScheduler(notificationProducer, delay, delay)
            notificationProducerScheduler.start()
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
                notificationProducerScheduler.stop()
                job.cancel()
            }
        }
    }
}

class NotificationProducer(val flow: MutableSharedFlow<KpiValuesDto>, val dataAccessorRestProxy: DataAccessorRestProxy) {
    val logger = KotlinLogging.logger {}
    suspend fun produce() {
        logger.debug { "NotificationProducer: Producing..." }
        dataAccessorRestProxy.retrieveLatestKpis()?.let {
            logger.debug { "NotificationProducer: Emitting KPI values DTO '$it'..." }
            flow.emit(it)
        }
    }
}

class NotificationProducerScheduler(
    val service: NotificationProducer,
    val interval: Duration,
    val initialDelay: Duration?,
) : CoroutineScope {
    private val job = Job()
    private val singleThreadExecutor = Executors.newSingleThreadExecutor()
    val logger = KotlinLogging.logger {}

    override val coroutineContext: CoroutineContext
        get() = job + singleThreadExecutor.asCoroutineDispatcher()

    fun stop() {
        job.cancel()
        singleThreadExecutor.shutdown()
    }

    fun start() =
        launch {
            logger.debug { "Starting NotificationProducerScheduler with interval '$interval' and initial delay '$initialDelay'" }
            initialDelay?.let {
                logger.debug { "Waiting for initial delay '$initialDelay'" }
                delay(it)
            }
            while (isActive) {
                logger.debug { "NotificationProducerScheduler: Producing..." }
                service.produce()
                logger.debug { "NotificationProducerScheduler: Produced! Waiting for next interval '$interval'..." }
                delay(interval)
            }
            logger.debug { "NotificationProducerScheduler: Stopping..." }
    }
}
