package ch.bziswiler.pvlogger.frontend.plugins

import io.ktor.client.plugins.websocket.WebSockets as ClientWebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.seconds

/** Security finding 59: a `/livedata` client must not be able to send arbitrarily large frames. */
class WebSocketsLimitTest {

    @Test
    fun `an oversized incoming frame closes the session with 1009`() = testApplication {
        application {
            installWebSockets()
            routing {
                // Same shape as /livedata: drain and ignore whatever the client sends.
                webSocket("/ws") { incoming.consumeEach { } }
            }
        }
        val client = createClient { install(ClientWebSockets) }

        client.webSocket("/ws") {
            send(Frame.Binary(true, ByteArray(MAX_INCOMING_FRAME_BYTES.toInt() * 256)))
            val reason = withTimeout(5.seconds) { closeReason.await() }
            assertEquals(CloseReason.Codes.TOO_BIG.code, reason?.code)
        }
    }

    @Test
    fun `outgoing frames above the limit still reach the client`() = testApplication {
        val big = "x".repeat(MAX_INCOMING_FRAME_BYTES.toInt() * 16)
        application {
            installWebSockets()
            routing {
                webSocket("/ws") { send(Frame.Text(big)) }
            }
        }
        val client = createClient { install(ClientWebSockets) }

        client.webSocket("/ws") {
            val frame = withTimeout(5.seconds) { incoming.receive() } as Frame.Text
            assertEquals(big.length, frame.readText().length)
        }
    }
}
