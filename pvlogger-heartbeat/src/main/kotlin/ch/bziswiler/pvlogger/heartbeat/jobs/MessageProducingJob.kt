package ch.bziswiler.pvlogger.heartbeat.jobs

import ch.bziswiler.pvlogger.heartbeat.messaging.impl.ActiveMqMessageSender
import ch.bziswiler.pvlogger.messaging.meta.MqConfig
import ch.bziswiler.pvlogger.messaging.meta.QueueName.JOB_SCHEDULER
import org.quartz.Job

abstract class MessageProducingJob(mqConfig: MqConfig) : ActiveMqMessageSender(JOB_SCHEDULER, mqConfig), Job