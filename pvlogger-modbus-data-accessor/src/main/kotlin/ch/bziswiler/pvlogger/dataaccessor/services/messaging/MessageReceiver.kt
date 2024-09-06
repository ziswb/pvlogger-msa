package ch.bziswiler.pvlogger.dataaccessor.services.messaging

interface MessageReceiver {

    fun addMessageHandler(listener: MessageHandler)
}
