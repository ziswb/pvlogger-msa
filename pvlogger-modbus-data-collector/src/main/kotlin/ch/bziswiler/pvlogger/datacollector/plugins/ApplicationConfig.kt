@file:OptIn(ExperimentalUnsignedTypes::class)

package ch.bziswiler.pvlogger.datacollector.plugins

import ch.bziswiler.pvlogger.datacollector.config.AppConfig
import ch.bziswiler.pvlogger.datacollector.config.MasterDataConfig
import ch.bziswiler.pvlogger.datacollector.config.ModbusTcpConfig
import ch.bziswiler.pvlogger.datacollector.config.ServerConfig
import ch.bziswiler.pvlogger.shared.messaging.config.MqConfig
import io.ktor.server.application.Application
import org.koin.ktor.ext.inject

private const val DELIMITER = ","

fun Application.setupConfig() {
    val appConfig by inject<AppConfig>()
    readServerConfig(appConfig)
    readModbusConfig(appConfig)
    readMqConfig(appConfig)
    readMdConfig(appConfig)
}

private fun Application.readMdConfig(appConfig: AppConfig) {
    val mdObject = environment.config.config("pvlogger.masterdata")
    val host = mdObject.property("host").getString()
    val port = mdObject.property("port").getString().toInt()
    appConfig.masterDataConfig = MasterDataConfig(host, port)
}

private fun Application.readServerConfig(appConfig: AppConfig) {
    val serverObject = environment.config.config("ktor.server")
    val isProd = serverObject.property("isProd").getString().toBoolean()
    appConfig.serverConfig = ServerConfig(isProd)
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

@kotlin.ExperimentalUnsignedTypes
private fun Application.readModbusConfig(appConfig: AppConfig) {
    val modbusObject = environment.config.config("huawei.modbus.tcp")
    val host = modbusObject.property("host").getString()
    val port = modbusObject.property("port").getString().toInt()
    val delay = modbusObject.property("delay").getString().toLong()
    val meterDeviceIds = modbusObject.property("meterDeviceIds").getString()
        .split(DELIMITER)
        .map { it.trim().toUByte() }
        .toUByteArray()
    val inverterDeviceIds = modbusObject.property("inverterDeviceIds").getString()
        .split(DELIMITER)
        .map { it.trim().toUByte() }
        .toUByteArray()
    appConfig.modbusTcpConfig = ModbusTcpConfig(
        host, port, delay, meterDeviceIds, inverterDeviceIds
    )
}
