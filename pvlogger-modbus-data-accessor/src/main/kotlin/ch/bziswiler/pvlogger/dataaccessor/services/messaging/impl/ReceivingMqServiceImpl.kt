package ch.bziswiler.pvlogger.dataaccessor.services.messaging.impl

import ch.bziswiler.pvlogger.dataaccessor.config.AppConfig
import ch.bziswiler.pvlogger.dataaccessor.services.messaging.ReceivingMqService
import ch.bziswiler.pvlogger.messaging.meta.QueueName
import org.apache.activemq.ActiveMQConnectionFactory
import javax.jms.Connection
import javax.jms.Session.AUTO_ACKNOWLEDGE

/*
 * see https://codenotfound.com/jms-publish-subscribe-messaging-example-activemq-maven.html for examples
 * see https://github.com/code-not-found/jms for code
 */
class ReceivingMqServiceImpl(
    private val appConfig: AppConfig,
    private val msgListener: StoreModbusValuesMessageListener
) : ReceivingMqService {

    private val clientId = appConfig.mqConfig.clientId
    private val user = appConfig.mqConfig.user
    private val passwd = appConfig.mqConfig.passwd

    override fun start() {
        with(connection()) {
            with(createSession(false, AUTO_ACKNOWLEDGE)) {
                with(
                    createConsumer(
                        createQueue(QueueName.PERSIST_MODBUS.value),
                        msgListener.messageSelector
                    )
                ) { messageListener = msgListener }
            }
            start()
        }
    }

    private fun connection(): Connection {
        val connectionFactory = ActiveMQConnectionFactory(appConfig.mqConfig.brokerUrl)
        connectionFactory.userName = user
        connectionFactory.password = passwd
        connectionFactory.clientID = clientId
        return connectionFactory.createConnection()
    }
}
