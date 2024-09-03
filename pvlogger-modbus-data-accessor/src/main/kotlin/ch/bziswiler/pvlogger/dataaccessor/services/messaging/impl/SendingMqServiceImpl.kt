package ch.bziswiler.pvlogger.dataaccessor.services.messaging.impl

import ch.bziswiler.pvlogger.dataaccessor.config.AppConfig
import ch.bziswiler.pvlogger.dataaccessor.services.messaging.SendingMqService
import ch.bziswiler.pvlogger.modbus.tcp.response.ModbusResponse
import mu.KotlinLogging
import org.apache.activemq.ActiveMQConnectionFactory
import javax.jms.Connection

/*
 * see https://codenotfound.com/jms-publish-subscribe-messaging-example-activemq-maven.html for examples
 * see https://github.com/code-not-found/jms for code
 */
class SendingMqServiceImpl(private val appConfig: AppConfig) : SendingMqService {

    private val logger = KotlinLogging.logger {}

    override fun sendRegisterValuesSavedMessage(message: ModbusResponse) {
        logger.warn { "TODO('Not yet implemented') -> jobId: ${message.jobId.id}, requestId: ${message.requestId}" }
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
