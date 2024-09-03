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

    override fun start() {
        with(connection()) {
            with(createSession(false, AUTO_ACKNOWLEDGE)) {
                with(createConsumer(createQueue(QueueName.PERSIST_MODBUS.value), msgListener.messageSelector)) {
                    messageListener = msgListener
                }
            }
            start()
        }
    }

    private fun connection(): Connection {
        with(ActiveMQConnectionFactory(appConfig.mqConfig.brokerUrl)) {
            userName = appConfig.mqConfig.user
            password = appConfig.mqConfig.passwd
            clientID = appConfig.mqConfig.clientId
            return createConnection()
        }
    }
}
