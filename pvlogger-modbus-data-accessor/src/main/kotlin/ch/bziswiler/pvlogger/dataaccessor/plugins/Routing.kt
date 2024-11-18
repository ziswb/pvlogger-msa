package ch.bziswiler.pvlogger.dataaccessor.plugins

import ch.bziswiler.pvlogger.dataaccessor.rest.EnergiesDataController.energiesTodaySoFar
import io.ktor.server.application.Application
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

fun Application.configureRouting() {
    routing {
        get("/energiesTodaySoFar") {
            energiesTodaySoFar()
        }
    }
}
