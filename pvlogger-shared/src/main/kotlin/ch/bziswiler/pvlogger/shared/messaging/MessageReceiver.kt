package ch.bziswiler.pvlogger.shared.messaging

interface MessageReceiver {

    fun addMessageHandler(listener: MessageHandler)
}
