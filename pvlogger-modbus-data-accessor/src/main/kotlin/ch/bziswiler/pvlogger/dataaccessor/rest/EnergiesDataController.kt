package ch.bziswiler.pvlogger.dataaccessor.rest

import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.response.respondText
import io.ktor.util.pipeline.PipelineContext

object EnergiesDataController {

    suspend fun PipelineContext<Unit, ApplicationCall>.energiesTodaySoFar() {
        call.respondText("Hello World!")
    }

}