package ch.bziswiler.pvlogger.dataaccessor.services.messaging

import javax.jms.MessageListener

interface MessageHandler : MessageListener {
    val messageSelector: String
}