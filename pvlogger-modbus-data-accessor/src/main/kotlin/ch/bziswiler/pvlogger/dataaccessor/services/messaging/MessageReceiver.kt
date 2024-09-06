package ch.bziswiler.pvlogger.dataaccessor.services.messaging

interface MessageReceiver {

    fun addMessageListener(listener: MessageHandler)
}
