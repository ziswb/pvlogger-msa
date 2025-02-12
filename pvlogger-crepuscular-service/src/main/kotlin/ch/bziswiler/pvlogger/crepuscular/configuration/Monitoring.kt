package ch.bziswiler.pvlogger.crepuscular.configuration

import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.metrics.micrometer.MicrometerMetrics
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.plugins.doublereceive.DoubleReceive
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics
import io.micrometer.core.instrument.binder.system.FileDescriptorMetrics
import io.micrometer.core.instrument.binder.system.ProcessorMetrics
import io.micrometer.core.instrument.binder.system.UptimeMetrics
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import kotlinx.coroutines.runBlocking
import org.slf4j.event.Level

private const val METRICS_PATH = "/metrics"

fun Application.configureMonitoring() {
    install(DoubleReceive)
    install(CallLogging) {
        level = Level.TRACE
        format { call ->
            runBlocking {
                "Body: ${call.receiveText()}"
            }
        }
    }
    val appMicrometerRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    install(MicrometerMetrics) {
        registry = appMicrometerRegistry
        meterBinders = listOf(
            ClassLoaderMetrics(),
            FileDescriptorMetrics(),
            JvmGcMetrics(),
            JvmMemoryMetrics(),
            JvmThreadMetrics(),
            ProcessorMetrics(),
            UptimeMetrics(),
        )
    }
    routing {
        get(METRICS_PATH) {
            call.respond(appMicrometerRegistry.scrape())
        }
    }
}
