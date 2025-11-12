package ch.bziswiler.pvlogger.fusionsolar.webservice

import ch.bziswiler.pvlogger.fusionsolar.configuration.FusionSolarConfig
import ch.bziswiler.pvlogger.fusionsolar.configuration.myJson
import ch.bziswiler.pvlogger.fusionsolar.model.LoginCredentialsV1
import ch.bziswiler.pvlogger.fusionsolar.model.LoginResponse
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import mu.KotlinLogging
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Url
import java.util.Timer
import kotlin.concurrent.schedule

class FusionSolarConnection(
    config: FusionSolarConfig,
) {
    val logger = KotlinLogging.logger {}
    val tokenName = "XSRF-TOKEN"
    val mutex = Mutex()

    private val blockedUrlsSet = mutableSetOf<String>()
    private val delayAfter407 = 60 * 1000L
    private val loginUrlNew = "${config.baseUrlNew}/login"
    private val userName = config.username
    private val systemCode = config.password
    private var authenticated = AuthState.NOT_AUTHENTICATED

    var xsrfToken: String? = ""
    private val cookieStore =
        object : CookieJar {
            private val store: MutableMap<String, MutableList<Cookie>> = mutableMapOf()

            override fun saveFromResponse(
                url: HttpUrl,
                cookies: List<Cookie>,
            ) {
                val key = url.topPrivateDomain() ?: url.host
                val list = store.getOrPut(key) { mutableListOf() }
                list.removeAll { existing -> cookies.any { it.name == existing.name && it.matches(url) } }
                list.addAll(cookies)
            }

            override fun loadForRequest(url: HttpUrl): List<Cookie> {
                val key = url.topPrivateDomain() ?: url.host
                return store[key]?.filter { it.matches(url) } ?: emptyList()
            }

            fun cookiesFor(hostOrUrl: String): List<Cookie> =
                try {
                    val url = hostOrUrl.toHttpUrl()
                    val key = url.topPrivateDomain() ?: url.host
                    store[key]?.toList() ?: emptyList()
                } catch (_: Throwable) {
                    store[hostOrUrl]?.toList() ?: emptyList()
                }
        }

    private val okHttpClient =
        OkHttpClient
            .Builder()
            .cookieJar(cookieStore)
            .build()

    private val retrofit: Retrofit =
        Retrofit
            .Builder()
            // Base URL is required but will be overridden by @Url for each call
            .baseUrl(config.baseUrlOld + "/")
            .client(okHttpClient)
            .build()

    @PublishedApi
    internal val api: FusionSolarApi = retrofit.create(FusionSolarApi::class.java)

    @PublishedApi
    internal interface FusionSolarApi {
        @POST
        suspend fun postJson(
            @Url url: String,
            @Body body: RequestBody,
        ): ResponseBody

        @POST
        suspend fun postJsonWithToken(
            @Url url: String,
            @Header("XSRF-TOKEN") token: String?,
            @Body body: RequestBody,
        ): ResponseBody
    }

    suspend fun connect() {
        logger.info { "connecting ..." }
        if (authenticateNewStyle() == AuthState.FAILURE) {
            throw UnsuccessfulRequestException(30029, "connecting new style failed too")
        }
        this.authenticated = AuthState.AUTHENTICATED
        logger.info { "connected" }
    }

    suspend inline fun <reified T, reified R> executeRequest(
        target: String,
        params: T,
    ): R =
        mutex.withLock {
            logger.info { "execute request against '$target'" }
            assertNotBlocked(target)
            assertConnected()
            val jsonBody = myJson.encodeToString(params)
            val requestBody: RequestBody = jsonBody.toRequestBody("application/json".toMediaType())
            val responseBody = api.postJsonWithToken(target, xsrfToken, requestBody)
            val json = myJson.parseToJsonElement(responseBody.string()) as JsonObject
            handleResponse<R>(target, json)
        }

    suspend inline fun <reified R> handleResponse(
        targetUrl: String,
        jsonObject: JsonObject,
    ): R {
        when (jsonObject["failCode"]?.jsonPrimitive?.int) {
            0 -> return convertJsonObjectTo<R>(jsonObject)
            305 -> handleNotLoggedIn()
            407 -> handleAccessFrequencyTooHigh(targetUrl)
        }
        throw UnsuccessfulRequestException(
            jsonObject["failCode"]?.jsonPrimitive?.int ?: 30029,
            jsonObject["message"]?.jsonPrimitive?.contentOrNull,
        )
    }

    inline fun <reified R> convertJsonObjectTo(jsonObject: JsonObject): R =
        myJson.decodeFromString<R>(myJson.encodeToString<JsonObject>(jsonObject))

    suspend fun assertConnected() {
        if (this.authenticated != AuthState.AUTHENTICATED) {
            connect()
        }
    }

    fun assertNotBlocked(target: String) {
        if (blockedUrlsSet.contains(target)) {
            logger.debug { "Target URL '$target' is currently blocked from access" }
            throw UnsuccessfulRequestException(407, "Access is currently blocked")
        }
    }

    fun handleAccessFrequencyTooHigh(target: String) {
        logger.debug { "Adding target URL '$target' to set of blocked URLs" }
        blockedUrlsSet.add(target)
        Timer().schedule(delayAfter407) {
            logger.debug { "Removing URL '$target' from set of blocked URLs" }
            blockedUrlsSet.remove(target)
        }
    }

    suspend fun handleNotLoggedIn() {
        connect()
    }

    private suspend fun authenticateNewStyle() = authenticateMe(loginUrlNew, LoginCredentialsV1(username = userName, password = systemCode))

    private suspend inline fun <reified T> authenticateMe(
        urlString: String,
        loginCredentials: T,
    ): AuthState =
        try {
            logger.info { "authenticating ..." }
            val jsonBody = myJson.encodeToString(loginCredentials)
            val requestBody: RequestBody = jsonBody.toRequestBody("application/json".toMediaType())
            val responseBody = api.postJson(urlString, requestBody)
            val response = myJson.decodeFromString<LoginResponse>(responseBody.string())
            logger.debug { response }
            if (response.failCode != 0) throw UnsuccessfulRequestException(response.failCode, response.message)
            logger.info { "updating authentication properties" }
            xsrfToken = tokenValueFromCookie(cookieStore.cookiesFor(urlString))
            logger.info { "authenticated" }
            AuthState.AUTHENTICATED
        } catch (e: Throwable) {
            xsrfToken = ""
            logger.error(e) { "authentication failed" }
            AuthState.FAILURE
        }

    private fun tokenValueFromCookie(cookies: List<Cookie>) =
        cookies
            .firstOrNull { cookie ->
                cookie.name.equals(tokenName, ignoreCase = true)
            }?.value ?: ""

    enum class AuthState {
        AUTHENTICATED,
        NOT_AUTHENTICATED,
        FAILURE,
    }
}
