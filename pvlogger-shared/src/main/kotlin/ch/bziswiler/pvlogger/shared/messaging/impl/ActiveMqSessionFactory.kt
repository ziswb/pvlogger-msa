package ch.bziswiler.pvlogger.shared.messaging.impl

import ch.bziswiler.pvlogger.shared.messaging.MqSessionFactory
import ch.bziswiler.pvlogger.shared.messaging.config.MqConfig
import mu.KotlinLogging
import org.apache.activemq.ActiveMQConnectionFactory
import java.lang.Thread.sleep
import java.util.concurrent.atomic.AtomicInteger
import javax.jms.Connection
import javax.jms.Session
import javax.jms.Session.AUTO_ACKNOWLEDGE

class ActiveMqSessionFactory(private val mqConfig: MqConfig) : MqSessionFactory {

    private val activeMqSession: Session
    private val transacted = false
    private val logger = KotlinLogging.logger {}
    private val maxRetries = 30
    private val sleepMs: Long = 5000
    private val retryCounter = AtomicInteger(0)

    init {
        with(connection()) {
            activeMqSession = createSession(transacted, AUTO_ACKNOWLEDGE)
            start()
        }
    }

    override fun session(): Session = activeMqSession

    private fun connection(): Connection = with(ActiveMQConnectionFactory(mqConfig.brokerUrl)) {
        userName = mqConfig.user
        password = mqConfig.passwd
        clientID = mqConfig.clientId
        tryConnectingToMessageQueue()
    }

    private fun ActiveMQConnectionFactory.tryConnectingToMessageQueue(): Connection {
        retryCounter.set(0)
        while (retryCounter.incrementAndGet() <= maxRetries) {
            try {
                logger.info("Connecting to message queue, attempt number: {}", retryCounter.get())
                val connection = createConnection()
                return connection
            } catch (th: Throwable) {
                logger.info("Connection to message queue failed: {}", th.message)
                sleep(sleepMs)
            }
        }
        throw IllegalStateException("Connection to message queue failed")
    }
}
