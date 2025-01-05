package ch.bziswiler.pvlogger.exporter.configuration

import ch.bziswiler.pvlogger.exporter.messaging.KpiExportingMessageHandler
import ch.bziswiler.pvlogger.exporter.rest.DataAccessorRestProxy
import ch.bziswiler.pvlogger.exporter.service.ExportService
import ch.bziswiler.pvlogger.exporter.service.JsonFileService
import ch.bziswiler.pvlogger.shared.messaging.MessageHandler
import ch.bziswiler.pvlogger.shared.messaging.MessageReceiver
import ch.bziswiler.pvlogger.shared.messaging.MqSessionFactory
import ch.bziswiler.pvlogger.shared.messaging.TopicName.JOB_SCHEDULER
import ch.bziswiler.pvlogger.shared.messaging.impl.ActiveMqSessionFactory
import ch.bziswiler.pvlogger.shared.messaging.impl.ActiveMqTopicReceiver
import org.koin.dsl.module

val appModule = module {
    single<AppConfig> { AppConfig() }
    single<MqSessionFactory> { ActiveMqSessionFactory(get<AppConfig>().mqConfig) }
    single<DataAccessorRestProxy> { DataAccessorRestProxy(get<AppConfig>().dataAccessConfig) }
    single<ExportService> { JsonFileService(get<AppConfig>().exportConfig) }
    single<MessageHandler> { KpiExportingMessageHandler(get<DataAccessorRestProxy>(), get<ExportService>()) }
    single<MessageReceiver> { ActiveMqTopicReceiver(get<MqSessionFactory>(), JOB_SCHEDULER) }
}
