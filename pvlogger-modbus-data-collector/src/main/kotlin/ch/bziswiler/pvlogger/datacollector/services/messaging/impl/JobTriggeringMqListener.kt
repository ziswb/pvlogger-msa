package ch.bziswiler.pvlogger.datacollector.services.messaging.impl

import ch.bziswiler.pvlogger.datacollector.services.messaging.MqListener
import ch.bziswiler.pvlogger.datacollector.services.messaging.MqService
import ch.bziswiler.pvlogger.datacollector.services.modbus.ModbusDataCollectionJob

class JobTriggeringMqListener(override val messageSelector: String, mqService: MqService, private val job: ModbusDataCollectionJob) : MqListener {

    init {
        mqService.addListener(this)
    }

    override fun messageReceived() = job.execute()
}