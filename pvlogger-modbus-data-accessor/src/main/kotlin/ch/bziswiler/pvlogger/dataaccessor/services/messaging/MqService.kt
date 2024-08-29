package ch.bziswiler.pvlogger.dataaccessor.services.messaging

interface MqService {

    fun start()

    fun sendRegisterValuesSavedMessage()
}
