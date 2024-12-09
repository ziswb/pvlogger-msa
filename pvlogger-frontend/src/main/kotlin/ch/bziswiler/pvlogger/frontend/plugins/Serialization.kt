package ch.bziswiler.pvlogger.frontend.plugins

import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonTransformingSerializer
import kotlinx.serialization.modules.SerializersModule
import mu.KotlinLogging

fun Application.configureSerialization() {
    install(ContentNegotiation) { json(myJson) }
}

val myJson = Json {
    prettyPrint = true
    isLenient = true
    ignoreUnknownKeys = true
    allowSpecialFloatingPointValues = true
    encodeDefaults = true
    serializersModule = SerializersModule {
        contextual(Double::class, MySrzr)
    }
}

object MySrzr : JsonTransformingSerializer<Double>(Double.serializer()) {
    val logger = KotlinLogging.logger {}
    override fun transformDeserialize(element: JsonElement): JsonElement {
        logger.trace { "######################################################" }
        logger.trace { "MySrzr: juhui, My-Double-Serializer twitched!" }
        logger.trace { "######################################################" }
        return super.transformDeserialize(element)
    }
}
