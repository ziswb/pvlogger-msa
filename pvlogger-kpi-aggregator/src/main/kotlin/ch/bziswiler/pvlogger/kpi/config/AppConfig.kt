package ch.bziswiler.pvlogger.kpi.config

import ch.bziswiler.pvlogger.shared.messaging.config.MqConfig

class AppConfig {
    lateinit var serverConfig: ServerConfig
    lateinit var mqConfig: MqConfig
}

data class ServerConfig(
    val isProd: Boolean,
)
