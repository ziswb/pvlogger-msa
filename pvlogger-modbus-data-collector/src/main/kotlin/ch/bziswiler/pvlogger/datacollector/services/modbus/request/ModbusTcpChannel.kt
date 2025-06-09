package ch.bziswiler.pvlogger.datacollector.services.modbus.request

import ch.bziswiler.pvlogger.datacollector.configuration.AppConfig
import ch.bziswiler.pvlogger.datacollector.services.modbus.response.ModbusResponse
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.io.Sink
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
import mu.KotlinLogging
import java.net.Socket
import java.util.concurrent.TimeoutException

class ModbusTcpChannel(config: AppConfig) {
    private val logger = KotlinLogging.logger {}
    private val readTimeout = 30000L
    private lateinit var socket: Socket
    private val mutex = Mutex()
    private val peerHost = config.modBus.host
    private val port = config.modBus.port

    suspend fun request(request: ModbusRequest): ModbusResponse = mutex.withLock {
        logger.debug("ModbusTcpChannel.request()")
        try {
            connect()
            doRequest(request, socket.outputStream.asSink().buffered())
        } catch (th: Throwable) {
            logger.error("ModbusTcpChannel is unable to execute the request", th)
            throw th
        } finally {
            disconnect()
        }
    }

    private fun connect() {
        logger.debug("ModbusTcpChannel.connect()")
        logger.debug("ModbusTcpChannel connecting to $peerHost:$port")
        socket = Socket(peerHost, port)
    }

    private fun disconnect() {
        logger.debug("ModbusTcpChannel.disconnect()")
        if (this::socket.isInitialized) {
            socket.close()
        }
    }

    private suspend fun doRequest(request: ModbusRequest, sendChannel: Sink): ModbusResponse {
        logger.debug("ModbusTcpChannel.doRequest()")
        val receiveChannel = socket.inputStream.asSource().buffered()
        request.encode(sendChannel)
        sendChannel.flush()
        return withTimeoutOrNull(readTimeout) {
            request.decode(receiveChannel)
        } ?: throw TimeoutException("no response within $readTimeout milliseconds!")
    }
}