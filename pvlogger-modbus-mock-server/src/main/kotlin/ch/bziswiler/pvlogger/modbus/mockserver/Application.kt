package ch.bziswiler.pvlogger.modbus.mockserver

import ch.bziswiler.pvlogger.modbus.mockserver.utils.MockDataBuilder.buildData
import ch.bziswiler.pvlogger.modbus.mockserver.utils.MockDataBuilder.buildDataUsingMasterData
import ch.bziswiler.pvlogger.modbus.mockserver.utils.MockDataBuilder.buildFunctionCode
import ch.bziswiler.pvlogger.modbus.mockserver.utils.MockDataBuilder.buildHeader
import ch.bziswiler.pvlogger.modbus.mockserver.utils.MasterDataCache
import ch.bziswiler.pvlogger.modbus.mockserver.utils.MockDataWriter
import ch.bziswiler.pvlogger.modbus.mockserver.utils.RequestDataReader
import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.server.application.Application
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import io.github.oshai.kotlinlogging.KotlinLogging

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

// No parameters: Ktor's DI plugin would try to inject them (see the other services).
fun Application.module() = configureMockServer(testing = false)

/** With [testing], the socket server answers with fixed data instead of master-data-driven values. */
fun Application.configureMockServer(testing: Boolean) {
    configureMasterDataCache()
    launch { startSocketServer(testing) }
}

private fun Application.configureMasterDataCache() {
    MasterDataCache.configure(
        host = environment.config.propertyOrNull("pvlogger.masterdata.host")?.getString() ?: "localhost",
        port = environment.config.propertyOrNull("pvlogger.masterdata.port")?.getString()?.toInt() ?: 8081,
    )
}

suspend fun Application.startSocketServer(testing: Boolean) {
    val logger = KotlinLogging.logger {}
    val port = environment.config.propertyOrNull("ktor.socket-server.tcp-port")?.getString()?.toInt() ?: 8502
    val ip = environment.config.propertyOrNull("ktor.socket-server.ip")?.getString() ?: "0.0.0.0"
    logger.info { "socket server: starting at  $ip:$port" }
    coroutineScope {
        val selectorManager = SelectorManager(Dispatchers.IO)
        val serverSocket = aSocket(selectorManager).tcp().bind(ip, port)
        while (isActive) {
            logger.info { "socket server: waiting for connections" }
            val socket = serverSocket.accept()
            logger.info { "socket server: accepting ${socket.remoteAddress}" }
            launch {
                val receiveChannel = socket.openReadChannel()
                val sendChannel = socket.openWriteChannel(autoFlush = true)
                socket.use {
                    logger.info { "socket server: reading request" }
                    val request = RequestDataReader(receiveChannel).readRequest()
                    logger.info { "received request: $request" }
                    val writer = MockDataWriter(sendChannel)
                    logger.info { "socket server: writing response" }
                    if (testing) {
                        writer.writeData(buildHeader(), buildFunctionCode(), buildData())
                    } else {
                        val data = buildDataUsingMasterData(request.first, request.second)
                        val dataLength = data.fold(3) { sum, values -> sum + values.quantity }
                        writer.writeData(buildHeader(dataLength = dataLength.toUShort()), buildFunctionCode(), data)
                    }
                    sendChannel.flush()
                }
            }
        }
    }
}

