package ch.bziswiler.pvlogger.heartbeat.messaging

import ch.bziswiler.pvlogger.heartbeat.config.AppConfig
import ch.bziswiler.pvlogger.messaging.meta.JobStartMessageTag
import ch.bziswiler.pvlogger.messaging.meta.QueueName.JOB_SCHEDULER
import mu.KotlinLogging
import org.apache.activemq.ActiveMQConnectionFactory
import javax.jms.MessageProducer
import javax.jms.Session
import javax.jms.Session.AUTO_ACKNOWLEDGE

private const val SECONDS_PER_MINUTE = 60
private const val TWO = 2L

/*
 * see https://codenotfound.com/jms-publish-subscribe-messaging-example-activemq-maven.html for examples
 * see https://github.com/code-not-found/jms for code
 */
class ActiveMqService(private val appConfig: AppConfig) : MqService {

    private val logger = KotlinLogging.logger {}

    private val messageProducer: MessageProducer
    private val session: Session

    init {
        // see https://activemq.apache.org/components/classic/documentation/connection-configuration-uri for details
        with(ActiveMQConnectionFactory(appConfig.mqConfig.brokerUrl)) {
            userName = appConfig.mqConfig.user
            password = appConfig.mqConfig.passwd
            clientID = appConfig.mqConfig.clientId
            with(createConnection()) {
                session = createSession(false, AUTO_ACKNOWLEDGE)
                val destination = session.createQueue(JOB_SCHEDULER.value)
                messageProducer = session.createProducer(destination)
                messageProducer.timeToLive = TWO.minutes()
                start()
            }
        }
    }

    override fun sendMessage(tags: List<JobStartMessageTag>) {
        logger.debug { "Sending message ..." }
        val msg = session.createMessage()
        tags.forEach {
            msg.setStringProperty(it.key, it.value)
        }
        messageProducer.send(msg)
        logger.debug {
            """
SEND #########################################################################
${appConfig.mqConfig.clientId}: sent message: $msg
SEND #########################################################################
"""
        }
    }
}

fun Long.minutes() = this.times(SECONDS_PER_MINUTE)
