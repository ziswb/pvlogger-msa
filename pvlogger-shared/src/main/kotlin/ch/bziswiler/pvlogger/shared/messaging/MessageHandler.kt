package ch.bziswiler.pvlogger.shared.messaging

import javax.jms.MessageListener

interface MessageHandler : MessageListener {
    val messageSelector: String
}