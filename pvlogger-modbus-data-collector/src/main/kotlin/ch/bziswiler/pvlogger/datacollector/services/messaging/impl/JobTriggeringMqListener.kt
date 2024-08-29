package ch.bziswiler.pvlogger.datacollector.services.messaging.impl

import ch.bziswiler.pvlogger.datacollector.services.messaging.MqListener
import ch.bziswiler.pvlogger.datacollector.services.modbus.ModbusDataCollectionJob

class JobTriggeringMqListener(override val messageSelector: String, private val job: ModbusDataCollectionJob) : MqListener {

    override fun messageReceived() = job.execute()
}