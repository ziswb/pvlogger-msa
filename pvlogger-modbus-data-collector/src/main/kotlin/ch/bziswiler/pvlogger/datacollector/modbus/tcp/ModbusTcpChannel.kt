package ch.bziswiler.pvlogger.datacollector.modbus.tcp

import ch.bziswiler.pvlogger.datacollector.configuration.AppConfig
import ch.bziswiler.pvlogger.datacollector.modbus.tcp.request.ModbusRequest
import ch.bziswiler.pvlogger.shared.modbus.tcp.response.ModbusResponse
import io.ktor.utils.io.ByteChannel
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.core.readBytes
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import mu.KotlinLogging
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.TimeoutException

class ModbusTcpChannel(config: AppConfig) {
    private val logger = KotlinLogging.logger {}
    private val readTimeout = 30000L
    private lateinit var socket: Socket
    private val mutex = Mutex()
    private val peerHost = config.modbusTcpConfig.host
    private val port = config.modbusTcpConfig.port
    private val delay = config.modbusTcpConfig.delay

    suspend fun request(request: ModbusRequest): ModbusResponse = mutex.withLock {
        logger.debug("ModbusTcpChannel.request()")
        try {
            connect()
            doRequest(request)
        } catch (th: Throwable) {
            logger.error("ModbusTcpChannel is unable to execute the request", th)
            throw th
        } finally {
            disconnect()
        }
    }

    private fun connect(): ModbusTcpChannel {
        logger.debug("ModbusTcpChannel.connect()")
        logger.debug("ModbusTcpChannel connecting to $peerHost:$port")
        socket = Socket()
        socket.soTimeout = readTimeout.toInt()
        socket.connect(InetSocketAddress(peerHost, port), readTimeout.toInt())
        return this
    }

    private fun disconnect() {
        logger.debug("ModbusTcpChannel.disconnect()")
        if (this::socket.isInitialized) {
            socket.close()
        }
    }

    private suspend fun doRequest(request: ModbusRequest): ModbusResponse {
        logger.debug("ModbusTcpChannel.doRequest()")
        // Build request into an in-memory ByteChannel
        val outChannel = ByteChannel(autoFlush = true)
        delay(delay)
        request.encode(outChannel)
        // Extract bytes and send over socket
        val packet = outChannel.readRemaining()
        val toSend = packet.readBytes()
        socket.getOutputStream().write(toSend)
        socket.getOutputStream().flush()
        // Read response fully with timeout (soTimeout is set). We don't know exact length, so read available after small delay
        return withTimeoutOrNull(readTimeout) {
            delay(delay)
            val input = socket.getInputStream()
            val available = input.available().coerceAtLeast(1)
            val buffer = ByteArray(available)
            val read = input.read(buffer)
            if (read <= 0) throw TimeoutException("no data")
            val data = buffer.copyOf(read)
            val inChannel: ByteReadChannel = ByteReadChannel(data)
            request.decode(inChannel)
        } ?: throw TimeoutException("no response within $readTimeout milliseconds!")
    }
}
