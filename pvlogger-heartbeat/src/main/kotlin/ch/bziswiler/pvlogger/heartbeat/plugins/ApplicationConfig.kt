package ch.bziswiler.pvlogger.heartbeat.plugins

import ch.bziswiler.pvlogger.heartbeat.config.AppConfig
import ch.bziswiler.pvlogger.heartbeat.config.DatabaseConfig
import ch.bziswiler.pvlogger.heartbeat.config.JobConfig
import ch.bziswiler.pvlogger.heartbeat.config.ServerConfig
import ch.bziswiler.pvlogger.shared.messaging.config.MqConfig
import io.ktor.server.application.Application
import org.koin.ktor.ext.inject

fun Application.setupConfig() {
    val appConfig by inject<AppConfig>()
    readServerConfig(appConfig)
    readDbConfig(appConfig)
    readMqConfig(appConfig)
    readJobConfig(appConfig)
}

private fun Application.readMqConfig(appConfig: AppConfig) {
    val mqObject = environment.config.config("messagequeue")
    val clientId = mqObject.property("clientId").getString()
    val user = mqObject.property("user").getString()
    val passwd = mqObject.property("passwd").getString()
    val brokerUrl = mqObject.property("brokerUrl").getString()
    appConfig.mqConfig = MqConfig(
        clientId,
        user,
        passwd,
        brokerUrl
    )
}

private fun Application.readDbConfig(appConfig: AppConfig) {
    val databaseObject = environment.config.config("database")
    val driverClass = databaseObject.property("driverClass").getString()
    val driverDelegateClass = databaseObject.property("driverDelegateClass").getString()
    val dbServerUrl = databaseObject.property("dbServerUrl").getString()
    val dbName = databaseObject.property("dbName").getString()
    val dbCanonicalUrl = databaseObject.property("dbServerCanonicalUrl").getString()
    val user = databaseObject.property("user").getString()
    val password = databaseObject.property("password").getString()
    val maxPoolSize = databaseObject.property("maxPoolSize").getString().toInt()
    appConfig.databaseConfig = DatabaseConfig(
        driverClass,
        driverDelegateClass,
        dbServerUrl,
        dbName,
        dbCanonicalUrl,
        user,
        password,
        maxPoolSize
    )
}

private fun Application.readServerConfig(appConfig: AppConfig) {
    val serverObject = environment.config.config("ktor.server")
    val isProd = serverObject.property("isProd").getString().toBoolean()
    appConfig.serverConfig = ServerConfig(isProd)
}

private fun Application.readJobConfig(appConfig: AppConfig) {
    val serverObject = environment.config.config("job")
    val lateEveningTime = serverObject.property("lateEvening").getString()
    appConfig.jobConfig = JobConfig(lateEveningTime)
}
