package ch.bziswiler.pvlogger.datacollector.jobs.services.messaging

import ch.bziswiler.pvlogger.shared.messaging.MqSessionFactory
import ch.bziswiler.pvlogger.shared.messaging.QueueName.PERSIST_MODBUS
import ch.bziswiler.pvlogger.shared.messaging.impl.ActiveMqQueueSender

class StoreModbusValuesMessageSender(
    mqSessionFactory: MqSessionFactory,
) : ActiveMqQueueSender(mqSessionFactory, PERSIST_MODBUS)
