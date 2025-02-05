package ch.bziswiler.pvlogger.shared.messaging

import javax.jms.Destination

interface MessageReceiver<T : Destination> : NameAware {
    val destination: T
}
