package ch.bziswiler.pvlogger.datacollector.jobs.configuration

import io.micrometer.core.instrument.Metrics
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics
import io.micrometer.core.instrument.binder.system.FileDescriptorMetrics
import io.micrometer.core.instrument.binder.system.ProcessorMetrics
import io.micrometer.core.instrument.binder.system.UptimeMetrics
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry

fun initMetrics(): PrometheusMeterRegistry =
    PrometheusMeterRegistry(PrometheusConfig.DEFAULT).also { registry ->
        Metrics.addRegistry(registry)
        listOf(
            ClassLoaderMetrics(),
            FileDescriptorMetrics(),
            JvmGcMetrics(),
            JvmMemoryMetrics(),
            JvmThreadMetrics(),
            ProcessorMetrics(),
            UptimeMetrics(),
        ).forEach { it.bindTo(registry) }
    }
