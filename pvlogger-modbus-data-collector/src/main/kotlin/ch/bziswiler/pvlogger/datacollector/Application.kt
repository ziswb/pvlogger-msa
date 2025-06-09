package ch.bziswiler.pvlogger.datacollector

import ch.bziswiler.pvlogger.datacollector.configuration.di
import ch.bziswiler.pvlogger.datacollector.configuration.setupConfig
import ch.bziswiler.pvlogger.shared.messaging.MqReceiverComposer
import org.kodein.di.instance

fun main(args: Array<String>) {
    setupConfig()
    startServices()
}

fun startServices() {
    val composer by di.instance<MqReceiverComposer>()
    composer.compose()
}
