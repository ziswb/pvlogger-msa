package ch.bziswiler.pvlogger.dataaccessor.messaging

import ch.bziswiler.pvlogger.shared.messaging.MqSessionFactory
import ch.bziswiler.pvlogger.shared.messaging.QueueName.PERSIST_KPIS
import ch.bziswiler.pvlogger.shared.messaging.impl.ActiveMqMessageReceiver

class StoreKpiValuesMessageReceiver(mqSessionFactory: MqSessionFactory) : ActiveMqMessageReceiver(mqSessionFactory, PERSIST_KPIS)
