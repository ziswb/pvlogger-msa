package ch.bziswiler.pvlogger.fusionsolar.webservice

import ch.bziswiler.pvlogger.fusionsolar.configuration.FusionSolarConfig
import ch.bziswiler.pvlogger.fusionsolar.configuration.myJson
import ch.bziswiler.pvlogger.fusionsolar.model.LoginCredentials
import ch.bziswiler.pvlogger.fusionsolar.model.LoginCredentialsV1
import ch.bziswiler.pvlogger.fusionsolar.model.LoginResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.cookies.cookies
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.Cookie
import io.ktor.http.Url
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import mu.KotlinLogging
import java.util.*
import kotlin.concurrent.schedule

class FusionSolarConnection(config: FusionSolarConfig) {

    val logger = KotlinLogging.logger {}
    val tokenName = "XSRF-TOKEN"
    val mutex = Mutex()

    private val blockedUrlsSet = mutableSetOf<String>()
    private val delayAfter407 = 60 * 1000L
    private val loginUrlOld = "${config.baseUrlOld}/login"
    private val loginUrlNew = "${config.baseUrlNew}/login"
    private val userName = config.username
    private val systemCode = config.password
    private var authenticated = AuthState.NOT_AUTHENTICATED

    var xsrfToken: String? = ""
    val httpClient = HttpClient(CIO) {
        install(Logging) { level = LogLevel.ALL }
        install(ContentNegotiation) { json(myJson) }
        install(HttpCookies)
    }

    suspend fun connect() {
        logger.info { "connecting ..." }
        if (authenticateOldStyle() == AuthState.FAILURE) {
            this.authenticated = AuthState.FAILURE
            logger.info { "connecting old style failed, trying new style ..." }
            if (authenticateNewStyle() == AuthState.FAILURE) {
                throw UnsuccessfulRequestException(30029, "connecting new style failed too")
            }
        }
        this.authenticated = AuthState.AUTHENTICATED
        logger.info { "connected" }
    }

    suspend inline fun <reified T, reified R> executeRequest(target: Url, params: T): R = mutex.withLock {
        logger.info { "execute request against '$target'" }
        assertNotBlocked(target)
        assertConnected()
        httpClient.post(target) {
            contentType(ContentType.Application.Json)
            setBody(params)
            header(tokenName, xsrfToken)
        }.body<JsonObject>().let { response ->
            handleResponse<R>(target, response)
        }
    }

    suspend inline fun <reified R> handleResponse(targetUrl: Url, jsonObject: JsonObject): R {
        when (jsonObject["failCode"]?.jsonPrimitive?.int) {
            0 -> return convertJsonObjectTo<R>(jsonObject)
            305 -> handleNotLoggedIn()
            407 -> handleAccessFrequencyTooHigh(targetUrl)
        }
        throw UnsuccessfulRequestException(
            jsonObject["failCode"]?.jsonPrimitive?.int ?: 30029,
            jsonObject["message"]?.jsonPrimitive?.contentOrNull
        )
    }

    inline fun <reified R> convertJsonObjectTo(jsonObject: JsonObject): R =
        myJson.decodeFromString<R>(myJson.encodeToString<JsonObject>(jsonObject))

    suspend fun assertConnected() {
        if (this.authenticated != AuthState.AUTHENTICATED) {
            connect()
        }
    }

    fun assertNotBlocked(target: Url) {
        if (blockedUrlsSet.contains(target.toString())) {
            logger.debug { "Target URL '${target}' is currently blocked from access" }
            throw UnsuccessfulRequestException(407, "Access is currently blocked")
        }
    }

    fun handleAccessFrequencyTooHigh(target: Url) {
        logger.debug { "Adding target URL '${target}' to set of blocked URLs" }
        blockedUrlsSet.add(target.toString())
        Timer().schedule(delayAfter407) {
            logger.debug { "Removing URL '${target}' from set of blocked URLs" }
            blockedUrlsSet.remove(target.toString())
        }
    }

    suspend fun handleNotLoggedIn() {
        connect()
    }

    private suspend fun authenticateOldStyle() =
        authenticateMe(loginUrlOld, LoginCredentials(userName = userName, systemCode = systemCode))

    private suspend fun authenticateNewStyle() =
        authenticateMe(loginUrlNew, LoginCredentialsV1(username = userName, password = systemCode))

    private suspend inline fun <reified T> authenticateMe(urlString: String, loginCredentials: T): AuthState = try {
        logger.info { "authenticating ..." }
        with(httpClient.post(urlString) {
            contentType(ContentType.Application.Json)
            setBody(loginCredentials)
        }.body<LoginResponse>()) {
            logger.debug { this }
            if (failCode != 0) throw UnsuccessfulRequestException(failCode, message)
        }
        logger.info { "updating authentication properties" }
        xsrfToken = tokenValueFromCookie(httpClient.cookies(urlString))
        logger.info { "authenticated" }
        AuthState.AUTHENTICATED
    } catch (e: Throwable) {
        xsrfToken = ""
        logger.error(e) { "authentication failed" }
        AuthState.FAILURE
    }

    private fun tokenValueFromCookie(cookies: List<Cookie>) = cookies.firstOrNull { cookie ->
        cookie.name.lowercase() == tokenName.lowercase()
    }?.value ?: ""

    enum class AuthState {
        AUTHENTICATED,
        NOT_AUTHENTICATED,
        FAILURE
    }
}
