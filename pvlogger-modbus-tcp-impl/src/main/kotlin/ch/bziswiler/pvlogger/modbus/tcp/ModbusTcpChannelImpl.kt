package ch.bziswiler.pvlogger.modbus.tcp

import ch.bziswiler.pvlogger.modbus.api.tcp.ModbusRequest
import ch.bziswiler.pvlogger.modbus.api.tcp.ModbusResponse
import ch.bziswiler.pvlogger.modbus.api.tcp.ModbusTcpChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import mu.KotlinLogging
import okio.Buffer
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.TimeoutException

class ModbusTcpChannelImpl(
    private val peerHost: String,
    private val port: Int,
    private val delay: Long,
) : ModbusTcpChannel {
    private val logger = KotlinLogging.logger {}
    private val readTimeout = 30000L
    private lateinit var socket: Socket
    private val mutex = Mutex()

    override suspend fun request(request: ModbusRequest): ModbusResponse =
        mutex.withLock {
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

    private fun connect(): ModbusTcpChannelImpl {
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
        val outBuffer = Buffer()
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
            val inBuffer = Buffer().write(data)
            request.decode(inBuffer)
        } ?: throw TimeoutException("no response within $readTimeout milliseconds!")
    }
}
