package ch.bziswiler.pvlogger.frontend

import ch.bziswiler.pvlogger.frontend.configuration.appModule
import ch.bziswiler.pvlogger.frontend.configuration.readConfiguration
import ch.bziswiler.pvlogger.frontend.plugins.configureCors
import ch.bziswiler.pvlogger.frontend.plugins.configureHTTP
import ch.bziswiler.pvlogger.frontend.plugins.configureRouting
import ch.bziswiler.pvlogger.frontend.plugins.configureSerialization
import ch.bziswiler.pvlogger.frontend.plugins.configureWebSockets
import ch.bziswiler.pvlogger.shared.ktor.configureMonitoring
import ch.bziswiler.pvlogger.shared.ktor.configureStatusPages
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.netty.EngineMain
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun main(args: Array<String>) {
    EngineMain.main(args)
}

// No parameters: Koin pulls in Ktor's DI plugin, which tries to inject every
// parameter of a configured module function (a `testing: Boolean` default breaks startup).
@Suppress("unused") // application.conf references the main function. This annotation prevents the IDE from marking it as unused.
fun Application.module() {
    install(Koin) {
        slf4jLogger()
        modules(appModule)
    }
    configureApplication()
}

/** Everything except Koin, which tests install themselves. */
fun Application.configureApplication() {
    readConfiguration()
    configureStatusPages()
    configureMonitoring()
    configureWebSockets()
    configureHTTP()
    configureSerialization()
    configureRouting()
    configureCors()
}
