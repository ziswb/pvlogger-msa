package ch.bziswiler.pvlogger.datacollector.services.messaging

import ch.bziswiler.pvlogger.shared.messaging.MqSessionFactory
import ch.bziswiler.pvlogger.shared.messaging.TopicName.JOB_SCHEDULER
import ch.bziswiler.pvlogger.shared.messaging.impl.ActiveMqTopicSender

class JobSchedulerMessageSender(mqSessionFactory: MqSessionFactory) : ActiveMqTopicSender(mqSessionFactory, JOB_SCHEDULER, ephemeralMessages = true)
