package ch.bziswiler.pvlogger.shared.messaging

import javax.jms.Session

interface MqSessionFactory {
    var reconnectListener: MqReconnectListener?
    val session: Session
}