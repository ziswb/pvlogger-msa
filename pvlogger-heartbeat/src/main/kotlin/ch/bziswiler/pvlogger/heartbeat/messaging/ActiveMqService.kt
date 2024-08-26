package ch.bziswiler.pvlogger.heartbeat.messaging

import ch.bziswiler.pvlogger.heartbeat.config.AppConfig
import ch.bziswiler.pvlogger.messaging.job.naming.QueueName.JOB_SCHEDULER
import ch.bziswiler.pvlogger.messaging.job.tag.JobTag
import mu.KotlinLogging
import org.apache.activemq.ActiveMQConnectionFactory
import java.time.Instant.now
import javax.jms.Connection
import javax.jms.MessageProducer
import javax.jms.Session
import javax.jms.Session.AUTO_ACKNOWLEDGE

/*
 * see https://codenotfound.com/jms-publish-subscribe-messaging-example-activemq-maven.html for examples
 * see https://github.com/code-not-found/jms for code
 */
class ActiveMqService(private val appConfig: AppConfig) : MqService {

    private val logger = KotlinLogging.logger {}

    private val messageProducer: MessageProducer
    private val connection: Connection
    private val session: Session

    init {
        // see https://activemq.apache.org/components/classic/documentation/connection-configuration-uri for details
        val connectionFactory = ActiveMQConnectionFactory(appConfig.mqConfig.brokerUrl)
        connectionFactory.userName = appConfig.mqConfig.user
        connectionFactory.password = appConfig.mqConfig.passwd
        connectionFactory.clientID = appConfig.mqConfig.clientId
        connection = connectionFactory.createConnection()
        this.session = connection.createSession(false, AUTO_ACKNOWLEDGE)
        val destination = session.createQueue(JOB_SCHEDULER.value)
        this.messageProducer = session.createProducer(destination)
        connection.start()
    }

    override fun sendMessage(tags: List<JobTag>) {
        logger.debug { "Sending message ..." }
        val msg = session.createMessage()
        tags.forEach {
            msg.setStringProperty(it.key, it.value.value)
        }
        msg.setLongProperty("timestamp", now().toEpochMilli())
        messageProducer.send(msg)
        logger.debug {
            """
SEND #########################################################################
${appConfig.mqConfig.clientId}: sent message with properties 'job' = ${msg.getStringProperty("job")} and 'timestamp' = ${msg.getLongProperty("timestamp")}
SEND #########################################################################
""""
        }
    }

    override fun close() {
        logger.debug { "Closing connection ..." }
        this.connection.close()
        logger.debug { "Connection closed!" }
    }
}
