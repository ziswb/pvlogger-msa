package ch.bziswiler.pvlogger.datacollector.services.messaging.impl

import ch.bziswiler.pvlogger.shared.messaging.MqSessionFactory
import ch.bziswiler.pvlogger.shared.messaging.impl.ActiveMqMessageSender
import ch.bziswiler.pvlogger.shared.messaging.meta.QueueName.PERSIST_MODBUS

class PersistenceMessageSender(mqSessionFactory: MqSessionFactory) : ActiveMqMessageSender(mqSessionFactory, PERSIST_MODBUS)