package ch.bziswiler.pvlogger.datacollector.services.messaging

import ch.bziswiler.pvlogger.shared.messaging.MqSessionFactory
import ch.bziswiler.pvlogger.shared.messaging.QueueName.PERSIST_MODBUS
import ch.bziswiler.pvlogger.shared.messaging.impl.ActiveMqMessageSender

class PersistenceMessageSender(mqSessionFactory: MqSessionFactory) : ActiveMqMessageSender(mqSessionFactory, PERSIST_MODBUS)