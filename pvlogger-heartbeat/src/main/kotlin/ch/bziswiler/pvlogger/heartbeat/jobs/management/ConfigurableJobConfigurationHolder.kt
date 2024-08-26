package ch.bziswiler.pvlogger.heartbeat.jobs.management

import java.time.ZonedDateTime

interface ConfigurableJobConfigurationHolder {
    fun config(start: ZonedDateTime): JobConfiguration
    fun config(start: ZonedDateTime, stop: ZonedDateTime): JobConfiguration
}
