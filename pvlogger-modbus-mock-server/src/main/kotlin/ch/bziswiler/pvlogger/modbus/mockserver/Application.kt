package ch.bziswiler.pvlogger.modbus.mockserver

import ch.bziswiler.pvlogger.modbus.mockserver.utils.MockDataBuilder.buildData
import ch.bziswiler.pvlogger.modbus.mockserver.utils.MockDataBuilder.buildFunctionCode
import ch.bziswiler.pvlogger.modbus.mockserver.utils.MockDataBuilder.buildHeader
import ch.bziswiler.pvlogger.modbus.mockserver.utils.MockDataWriter
import ch.bziswiler.pvlogger.modbus.mockserver.utils.RequestDataReader
import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.server.application.Application
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    launch { startSocketServer() }
}

fun Application.startSocketServer() {
    val logger = KotlinLogging.logger {}
    val port = environment.config.propertyOrNull("ktor.socket-server.tcp-port")?.getString()?.toInt() ?: 8502
    val ip = environment.config.propertyOrNull("ktor.socket-server.ip")?.getString()?: "0.0.0.0"
    logger.info { "socket server: starting at  $ip:$port" }
    runBlocking {
        val selectorManager = SelectorManager(Dispatchers.IO)
        val serverSocket = aSocket(selectorManager).tcp().bind(ip, port)
        while (true) {
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
                    writer.writeData(buildHeader(), buildFunctionCode(), buildData())
                }
            }
        }
    }
}

