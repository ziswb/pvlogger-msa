package ch.bziswiler.pvlogger.shared.messaging

interface MqReceiverComposer {
    fun addMessageHandler(handler: MessageHandler)
    fun compose()
}