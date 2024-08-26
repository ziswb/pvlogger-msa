package ch.bziswiler.pvlogger.dataaccessor.services.messaging

interface MqService {

    fun setListener(listener: MqListener)

    fun sendRegisterValuesSavedMessage()
}
