package ch.bziswiler.pvlogger.shared.messaging

interface MessageReceiver : NameAware {
    fun addMessageHandler(handler: MessageHandler)
}
