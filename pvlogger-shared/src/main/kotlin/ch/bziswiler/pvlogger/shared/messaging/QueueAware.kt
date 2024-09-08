package ch.bziswiler.pvlogger.shared.messaging

import ch.bziswiler.pvlogger.shared.messaging.meta.QueueName

interface QueueAware {
    val queueName: QueueName
}
