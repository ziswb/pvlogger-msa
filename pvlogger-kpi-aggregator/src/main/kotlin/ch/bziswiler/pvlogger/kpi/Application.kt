package ch.bziswiler.pvlogger.kpi

import ch.bziswiler.pvlogger.kpi.configuration.appModule
import ch.bziswiler.pvlogger.kpi.configuration.setupConfig
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.netty.EngineMain
import io.ktor.server.plugins.callloging.CallLogging
import mu.KotlinLogging
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger
import org.slf4j.event.Level

private val logger = KotlinLogging.logger {}

fun main(args: Array<String>) = EngineMain.main(args)

fun Application.module(testing: Boolean = false) {
    if (!testing) {
        install(Koin) {
            slf4jLogger()
            modules(appModule)
        }
    }
    install(CallLogging) { level = Level.TRACE }
    setupConfig()
}
