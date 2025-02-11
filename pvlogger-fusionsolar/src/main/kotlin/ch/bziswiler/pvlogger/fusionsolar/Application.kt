package ch.bziswiler.pvlogger.fusionsolar

import ch.bziswiler.pvlogger.fusionsolar.configuration.appModule
import ch.bziswiler.pvlogger.fusionsolar.configuration.configureDatabases
import ch.bziswiler.pvlogger.fusionsolar.configuration.configureMonitoring
import ch.bziswiler.pvlogger.fusionsolar.configuration.configureSerialization
import ch.bziswiler.pvlogger.fusionsolar.configuration.setupConfig
import ch.bziswiler.pvlogger.shared.messaging.MqReceiverComposer
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.netty.EngineMain
import org.koin.core.qualifier.named
import org.koin.ktor.ext.inject
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun main(args: Array<String>) = EngineMain.main(args)

@Suppress("unused")
fun Application.module(testing: Boolean = false) {
    if (!testing) {
        install(Koin) {
            slf4jLogger()
            modules(appModule)
        }
    }
    setupConfig()
    configureMonitoring()
    configureSerialization()
    configureDatabases()
    startServices(testing)
}

fun Application.startServices(testing: Boolean = false) {
    if (!testing) {
        startStoringModbusValues()
        startStoringKpiValues()
    }
}

private fun Application.startStoringModbusValues() {
    val composer by inject<MqReceiverComposer>(named("modbus_values"))
    composer.compose()
}

private fun Application.startStoringKpiValues() {
    val composer by inject<MqReceiverComposer>(named("kpi_values"))
    composer.compose()
}