package ch.bziswiler.pvlogger.modbus.mockserver

import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.server.testing.testApplication
import io.ktor.utils.io.readUTF8Line
import io.ktor.utils.io.writeStringUtf8
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplicationTest {

    @Test
    fun test() = testApplication {
        application {
            module(testing = true)
            runBlocking {
                val selectorManager = SelectorManager(Dispatchers.IO)
                val socket = aSocket(selectorManager).tcp().connect("127.0.0.1", 8502)
                val receiveChannel = socket.openReadChannel()
                val sendChannel = socket.openWriteChannel(autoFlush = true)
                sendChannel.writeStringUtf8("King\n")
                val greeting2 = receiveChannel.readUTF8Line()
                socket.close()
                assertEquals("Hello, King!", greeting2)
            }
        }
    }

}

