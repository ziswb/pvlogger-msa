package ch.bziswiler.pvlogger.modbus.mockserver

import ch.bziswiler.pvlogger.modbus.mockserver.utils.MockDataBuilder.buildData
import ch.bziswiler.pvlogger.modbus.mockserver.utils.MockDataBuilder.buildFunctionCode
import ch.bziswiler.pvlogger.modbus.mockserver.utils.MockDataBuilder.buildHeader
import ch.bziswiler.pvlogger.modbus.mockserver.utils.MockDataWriter
import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.server.application.Application
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

fun main(args: Array<String>) {
    args.forEach {
        println(it)
    }
    var port = 8080
    if (args.isNotEmpty()) {
        port = args[0].toInt()
    }
    startSocketServer(port)
}

fun Application.module(testing: Boolean = false) {
    if (testing) {
        launch { startSocketServer(getPort()) }
    }
}

fun startSocketServer(port: Int) {
    println("starting socket server at port $port")
    runBlocking {
        val selectorManager = SelectorManager(Dispatchers.IO)
        val serverSocket = aSocket(selectorManager).tcp().bind("127.0.0.1", port)
        while (true) {
            val socket = serverSocket.accept()
            launch {
                val receiveChannel = socket.openReadChannel()
                val sendChannel = socket.openWriteChannel(autoFlush = true)
                socket.use {
                    while (receiveChannel.availableForRead > 0) {
                        receiveChannel.readByte()
                    }
                    val writer = MockDataWriter(sendChannel)
                    writer.writeData(buildHeader(), buildFunctionCode(), buildData())
                }
            }
        }
    }
}

private fun Application.getPort() = environment.config.propertyOrNull("ktor.server.port")?.getString()?.toInt() ?: 8080
