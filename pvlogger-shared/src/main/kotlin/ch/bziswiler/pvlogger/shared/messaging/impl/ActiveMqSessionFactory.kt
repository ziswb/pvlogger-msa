package ch.bziswiler.pvlogger.shared.messaging.impl

import ch.bziswiler.pvlogger.shared.messaging.MqSessionFactory
import ch.bziswiler.pvlogger.shared.messaging.meta.MqConfig
import org.apache.activemq.ActiveMQConnectionFactory
import javax.jms.Connection
import javax.jms.Session
import javax.jms.Session.AUTO_ACKNOWLEDGE

class ActiveMqSessionFactory(private val mqConfig: MqConfig) : MqSessionFactory {

    private val activeMqSession: Session
    private val transacted = false

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
        createConnection()
    }
}