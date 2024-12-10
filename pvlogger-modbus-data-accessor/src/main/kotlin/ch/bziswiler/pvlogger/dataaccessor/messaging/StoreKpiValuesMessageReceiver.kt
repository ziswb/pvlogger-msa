package ch.bziswiler.pvlogger.dataaccessor.messaging

import ch.bziswiler.pvlogger.shared.messaging.MqSessionFactory
import ch.bziswiler.pvlogger.shared.messaging.TopicName.KPI_EVENTS
import ch.bziswiler.pvlogger.shared.messaging.impl.ActiveMqTopicReceiver

class StoreKpiValuesMessageReceiver(mqSessionFactory: MqSessionFactory) : ActiveMqTopicReceiver(mqSessionFactory, KPI_EVENTS)
