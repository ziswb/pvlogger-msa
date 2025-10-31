package ch.bziswiler.pvlogger.datacollector.jobs.configuration

import io.micrometer.core.instrument.Metrics
import io.micrometer.prometheus.PrometheusMeterRegistry
import io.prometheus.client.exporter.PushGateway
import mu.KotlinLogging
import java.io.IOException
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class MetricsPusher(
    private val config: MetricsPushConfig,
) {
    private val logger = KotlinLogging.logger {}
    private val scheduler: ScheduledExecutorService =
        Executors.newSingleThreadScheduledExecutor { r ->
            Thread(r, "metrics-pusher").apply { isDaemon = true }
        }
    private var scheduled: ScheduledFuture<*>? = null
    private var pushGateway: PushGateway? = null

    fun start() {
        if (!config.enabled) {
            logger.info { "Metrics push is disabled" }
            return
        }
        val registry = findPrometheusRegistry()
        if (registry == null) {
            logger.warn { "No PrometheusMeterRegistry found in Metrics.globalRegistry; cannot start metrics pusher." }
            return
        }
        pushGateway = PushGateway(config.url)
        val interval = if (config.intervalSeconds > 0) config.intervalSeconds else 15L
        logger.info {
            "Starting metrics pusher to ${config.url} every ${interval}s with job='${config.job}' and grouping=${config.grouping}"
        }
        scheduled =
            scheduler.scheduleAtFixedRate({
                try {
                    pushGateway?.pushAdd(registry.prometheusRegistry, config.job, config.grouping)
                } catch (e: IOException) {
                    logger.warn(e) { "Failed to push metrics to ${config.url}" }
                }
            }, interval, interval, TimeUnit.SECONDS)
    }

    fun stop(deleteFromGateway: Boolean = true) {
        scheduled?.cancel(false)
        scheduled = null
        if (deleteFromGateway && config.enabled) {
            try {
                val registry = findPrometheusRegistry()
                if (registry != null) {
                    // Delete by job and grouping
                    pushGateway?.delete(config.job, config.grouping)
                }
            } catch (e: IOException) {
                logger.warn(e) { "Failed to delete metrics from Pushgateway ${config.url}" }
            }
        }
        scheduler.shutdown()
    }

    private fun findPrometheusRegistry(): PrometheusMeterRegistry? =
        Metrics.globalRegistry.registries.firstOrNull {
            it is PrometheusMeterRegistry
        } as? PrometheusMeterRegistry
}
