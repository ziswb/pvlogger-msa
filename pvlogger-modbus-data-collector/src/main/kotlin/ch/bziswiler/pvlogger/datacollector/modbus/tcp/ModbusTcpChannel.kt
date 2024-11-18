package ch.bziswiler.pvlogger.datacollector.modbus.tcp

import ch.bziswiler.pvlogger.datacollector.configuration.AppConfig
import ch.bziswiler.pvlogger.datacollector.modbus.tcp.request.ModbusRequest
import ch.bziswiler.pvlogger.shared.modbus.tcp.response.ModbusResponse
import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.utils.io.ByteWriteChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import mu.KotlinLogging
import java.util.concurrent.TimeoutException

class ModbusTcpChannel(config: AppConfig) {
    private val logger = KotlinLogging.logger {}
    private val readTimeout = 30000L
    private lateinit var socket: Socket
    private lateinit var selectorManager: SelectorManager
    private val mutex = Mutex()
    private val peerHost = config.modbusTcpConfig.host
    private val port = config.modbusTcpConfig.port
    private val delay = config.modbusTcpConfig.delay

    suspend fun request(request: ModbusRequest): ModbusResponse = mutex.withLock {
        logger.debug("ModbusTcpChannel.request()")
        try {
            connect()
            doRequest(request, socket.openWriteChannel(autoFlush = false))
        } catch (th: Throwable) {
            logger.error("ModbusTcpChannel is unable to execute the request", th)
            throw th
        } finally {
            disconnect()
        }
    }

    private suspend fun connect(): ModbusTcpChannel {
        logger.debug("ModbusTcpChannel.connect()")
        selectorManager = SelectorManager(Dispatchers.IO)
        logger.debug("ModbusTcpChannel connecting to $peerHost:$port")
        socket = aSocket(selectorManager).tcp().connect(peerHost, port)
        return this
    }

    private fun disconnect() {
        logger.debug("ModbusTcpChannel.disconnect()")
        if (this::socket.isInitialized) {
            socket.close()
        }
        if (this::selectorManager.isInitialized) {
            selectorManager.close()
        }
    }

    private suspend fun doRequest(request: ModbusRequest, sendChannel: ByteWriteChannel): ModbusResponse {
        logger.debug("ModbusTcpChannel.doRequest()")
        val receiveChannel = socket.openReadChannel()
        delay(delay)
        request.encode(sendChannel)
        sendChannel.flush()
        return withTimeoutOrNull(readTimeout) {
            delay(delay)
            request.decode(receiveChannel)
        } ?: throw TimeoutException("no response within $readTimeout milliseconds!")
    }
}
