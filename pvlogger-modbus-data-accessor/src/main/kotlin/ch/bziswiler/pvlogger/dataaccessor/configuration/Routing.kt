package ch.bziswiler.pvlogger.dataaccessor.configuration

import ch.bziswiler.pvlogger.dataaccessor.rest.ReadRegisterValueController.readAndTransposeLatestRegisterValue
import io.ktor.server.application.Application
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

fun Application.configureRouting() {
    routing {
        get("/read-latest-register-value/{deviceId}/{registerAddress}") {
            readAndTransposeLatestRegisterValue()
        }
    }
}
