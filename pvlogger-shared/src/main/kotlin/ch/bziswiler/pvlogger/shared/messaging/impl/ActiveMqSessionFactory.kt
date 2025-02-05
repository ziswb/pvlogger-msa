package ch.bziswiler.pvlogger.shared.messaging.impl

import ch.bziswiler.pvlogger.shared.messaging.MqReconnectListener
import ch.bziswiler.pvlogger.shared.messaging.MqSessionFactory
import ch.bziswiler.pvlogger.shared.messaging.config.MqConfig
import io.ktor.util.logging.error
import mu.KotlinLogging
import org.apache.activemq.ActiveMQConnectionFactory
import java.lang.Thread.sleep
import java.util.concurrent.atomic.AtomicInteger
import javax.jms.Connection
import javax.jms.ExceptionListener
import javax.jms.Session
import javax.jms.Session.AUTO_ACKNOWLEDGE

class ActiveMqSessionFactory(
    private val mqConfig: MqConfig
) : MqSessionFactory {

    override var reconnectListener: MqReconnectListener? = null

    private val transacted = false
    private val logger = KotlinLogging.logger {}
    private val maxRetries = 30
    private val sleepMs: Long = 5000
    private val retryCounter = AtomicInteger(0)
    private var connection: Connection

    init {
        connection = connect()
    }

    override val session: Session
        get() = try {
            connection.createSession(transacted, AUTO_ACKNOWLEDGE)
        } catch (th: Throwable) {
            logger.error(th)
            handleConnectionError()
            connection.createSession(transacted, AUTO_ACKNOWLEDGE)
        }

    private fun connect(): Connection = with(ActiveMQConnectionFactory(mqConfig.brokerUrl)) {
        userName = mqConfig.user
        password = mqConfig.passwd
        clientID = mqConfig.clientId
        exceptionListener = ExceptionListener() { jmsException ->
            run {
                logger.warn(jmsException) { "MQ connection error: $jmsException" }
                handleConnectionError()
            }
        }
        tryConnectingToMessageQueue()
    }

    private fun ActiveMQConnectionFactory.tryConnectingToMessageQueue(): Connection {
        retryCounter.set(0)
        while (retryCounter.incrementAndGet() <= maxRetries) {
            try {
                logger.info("Connecting to message queue, attempt number: {}", retryCounter.get())
                val connection = createConnection()
                connection.start()
                return connection
            } catch (th: Throwable) {
                logger.info("Connection to message queue failed: {}", th.message)
                sleep(sleepMs)
            }
        }
        throw IllegalStateException("Connection to message queue failed")
    }

    private fun handleConnectionError() {
        logger.warn { "Trying to stop message queue connection" }
        try {
            connection.stop()
        } catch (th: Throwable) {
            logger.error(th) { "Stopping message queue connection failed" }
        }
        logger.warn { "Trying to close message queue connection" }
        try {
            connection.close()
        } catch (th: Throwable) {
            logger.error(th) { "Closing message queue connection failed" }
        }
        connection = connect()
        reconnectListener?.onAfterReconnect()
    }
}
