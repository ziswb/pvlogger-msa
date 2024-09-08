package ch.bziswiler.pvlogger.shared.messaging

import javax.jms.Session

interface MqSessionFactory {
    fun session(): Session
}