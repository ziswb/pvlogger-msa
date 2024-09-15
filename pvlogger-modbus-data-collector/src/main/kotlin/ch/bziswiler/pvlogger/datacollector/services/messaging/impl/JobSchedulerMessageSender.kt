package ch.bziswiler.pvlogger.datacollector.services.messaging.impl

import ch.bziswiler.pvlogger.shared.messaging.MqSessionFactory
import ch.bziswiler.pvlogger.shared.messaging.impl.ActiveMqMessageSender
import ch.bziswiler.pvlogger.shared.messaging.QueueName.JOB_SCHEDULER

class JobSchedulerMessageSender(mqSessionFactory: MqSessionFactory) : ActiveMqMessageSender(mqSessionFactory, JOB_SCHEDULER, ephemeralMessages = true)
