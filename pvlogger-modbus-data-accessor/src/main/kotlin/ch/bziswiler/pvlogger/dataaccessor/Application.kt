package ch.bziswiler.pvlogger.dataaccessor

import ch.bziswiler.pvlogger.dataaccessor.configuration.appModule
import ch.bziswiler.pvlogger.dataaccessor.configuration.configureDatabases
import ch.bziswiler.pvlogger.dataaccessor.configuration.configureMonitoring
import ch.bziswiler.pvlogger.dataaccessor.configuration.configureRouting
import ch.bziswiler.pvlogger.dataaccessor.configuration.configureSerialization
import ch.bziswiler.pvlogger.dataaccessor.configuration.setupConfig
import ch.bziswiler.pvlogger.dataaccessor.messaging.StoreKpiValuesMessageHandler
import ch.bziswiler.pvlogger.dataaccessor.messaging.StoreKpiValuesMessageReceiver
import ch.bziswiler.pvlogger.dataaccessor.messaging.StoreModbusValuesMessageHandler
import ch.bziswiler.pvlogger.dataaccessor.messaging.StoreModbusValuesMessageReceiver
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.netty.EngineMain
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
    configureRouting()
    startServices(testing)
}

fun Application.startServices(testing: Boolean = false) {
    if (!testing) {
        startStoringModbusValues()
        startStoringKpiValues()
    }
}

private fun Application.startStoringModbusValues() {
    val storeModbusValuesMessageReceiver by inject<StoreModbusValuesMessageReceiver>()
    val storeModbusValuesMessageHandler by inject<StoreModbusValuesMessageHandler>()
    storeModbusValuesMessageReceiver.addMessageHandler(storeModbusValuesMessageHandler)
}

private fun Application.startStoringKpiValues() {
    val storeKpiValuesMessageReceiver by inject<StoreKpiValuesMessageReceiver>()
    val storeKpiValuesMessageHandler by inject<StoreKpiValuesMessageHandler>()
    storeKpiValuesMessageReceiver.addMessageHandler(storeKpiValuesMessageHandler)
}