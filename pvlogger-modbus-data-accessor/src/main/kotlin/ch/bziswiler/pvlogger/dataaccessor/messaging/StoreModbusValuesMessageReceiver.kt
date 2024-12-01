package ch.bziswiler.pvlogger.dataaccessor.messaging

import ch.bziswiler.pvlogger.shared.messaging.MqSessionFactory
import ch.bziswiler.pvlogger.shared.messaging.QueueName.PERSIST_MODBUS
import ch.bziswiler.pvlogger.shared.messaging.impl.ActiveMqMessageReceiver

class StoreModbusValuesMessageReceiver(mqSessionFactory: MqSessionFactory) : ActiveMqMessageReceiver(mqSessionFactory, PERSIST_MODBUS)
