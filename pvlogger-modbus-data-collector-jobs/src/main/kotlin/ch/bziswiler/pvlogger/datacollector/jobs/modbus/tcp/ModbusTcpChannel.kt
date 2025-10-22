package ch.bziswiler.pvlogger.datacollector.jobs.modbus.tcp

import ch.bziswiler.pvlogger.datacollector.jobs.configuration.AppConfig
import ch.bziswiler.pvlogger.datacollector.jobs.modbus.tcp.request.ModbusRequest
import ch.bziswiler.pvlogger.shared.modbus.tcp.response.ModbusResponse
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
        // Build request into an in-memory buffer (Okio)
        val outBuffer = okio.Buffer()
        delay(delay)
        request.encode(outBuffer)
        // Extract bytes and send over socket
        val toSend = outBuffer.readByteArray()
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
            val inBuffer = okio.Buffer().write(data)
            request.decode(inBuffer)
        } ?: throw TimeoutException("no response within $readTimeout milliseconds!")
    }
}
