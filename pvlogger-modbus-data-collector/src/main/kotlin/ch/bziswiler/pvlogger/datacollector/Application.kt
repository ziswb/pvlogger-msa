package ch.bziswiler.pvlogger.datacollector

import ch.bziswiler.pvlogger.datacollector.di.appModule
import ch.bziswiler.pvlogger.datacollector.plugins.configureMonitoring
import ch.bziswiler.pvlogger.datacollector.plugins.setupConfig
import ch.bziswiler.pvlogger.datacollector.services.messaging.impl.JobTriggeringMqHandler
import ch.bziswiler.pvlogger.datacollector.services.modbus.impl.ModbusBatchedDynamicDataCollectionJob
import ch.bziswiler.pvlogger.datacollector.services.modbus.impl.ModbusBatchedStaticDataCollectionJob
import ch.bziswiler.pvlogger.datacollector.services.modbus.impl.ModbusDeviceIdentifiersCollectionJob
import ch.bziswiler.pvlogger.datacollector.services.modbus.impl.ModbusDeviceListCollectionJob
import ch.bziswiler.pvlogger.shared.messaging.MessageReceiver
import ch.bziswiler.pvlogger.shared.messaging.meta.JobName.DEVICE_IDS_JOB
import ch.bziswiler.pvlogger.shared.messaging.meta.JobName.DEVICE_LIST_JOB
import ch.bziswiler.pvlogger.shared.messaging.meta.JobName.DYNAMIC_DATA_JOB
import ch.bziswiler.pvlogger.shared.messaging.meta.JobName.STATIC_DATA_JOB
import ch.bziswiler.pvlogger.shared.messaging.meta.JobStartMessageTag
import io.ktor.server.application.Application
import io.ktor.server.application.install
import org.koin.ktor.ext.inject
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module(testing: Boolean = false) {
    if (!testing) {
        install(Koin) {
            slf4jLogger()
            modules(appModule)
        }
    }
    setupConfig()
    configureMonitoring()
    start()
}

fun Application.start() {
    val dynDataJob by inject<ModbusBatchedDynamicDataCollectionJob>()
    val stcDataJob by inject<ModbusBatchedStaticDataCollectionJob>()
    val devIdsJob by inject<ModbusDeviceIdentifiersCollectionJob>()
    val devLstJob by inject<ModbusDeviceListCollectionJob>()
    val jobMsgReceiver by inject<MessageReceiver>()
    jobMsgReceiver.addMessageHandler(JobTriggeringMqHandler(JobStartMessageTag(DYNAMIC_DATA_JOB).toSelector(), dynDataJob))
    jobMsgReceiver.addMessageHandler(JobTriggeringMqHandler(JobStartMessageTag(STATIC_DATA_JOB).toSelector(), stcDataJob))
    jobMsgReceiver.addMessageHandler(JobTriggeringMqHandler(JobStartMessageTag(DEVICE_IDS_JOB).toSelector(), devIdsJob))
    jobMsgReceiver.addMessageHandler(JobTriggeringMqHandler(JobStartMessageTag(DEVICE_LIST_JOB).toSelector(), devLstJob))
}
