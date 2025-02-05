package ch.bziswiler.pvlogger.exporter.configuration

import ch.bziswiler.pvlogger.exporter.messaging.KpiExportingMessageHandler
import ch.bziswiler.pvlogger.exporter.rest.DataAccessorRestProxy
import ch.bziswiler.pvlogger.exporter.service.DirectoryService
import ch.bziswiler.pvlogger.exporter.service.ExportService
import ch.bziswiler.pvlogger.exporter.service.JsonFileService
import ch.bziswiler.pvlogger.shared.messaging.MessageHandler
import ch.bziswiler.pvlogger.shared.messaging.MessageReceiver
import ch.bziswiler.pvlogger.shared.messaging.MqReceiverComposer
import ch.bziswiler.pvlogger.shared.messaging.MqSessionFactory
import ch.bziswiler.pvlogger.shared.messaging.TopicName.JOB_SCHEDULER
import ch.bziswiler.pvlogger.shared.messaging.impl.ActiveMqReceiverComposer
import ch.bziswiler.pvlogger.shared.messaging.impl.ActiveMqSessionFactory
import ch.bziswiler.pvlogger.shared.messaging.impl.ActiveMqTopicReceiver
import org.koin.dsl.module
import javax.jms.Topic

val appModule = module {
    single<AppConfig> { AppConfig() }
    single<MqSessionFactory> { ActiveMqSessionFactory(get<AppConfig>().mqConfig) }
    single<DataAccessorRestProxy> { DataAccessorRestProxy(get<AppConfig>().dataAccessConfig) }
    single<DirectoryService> { DirectoryService(get<AppConfig>().exportConfig) }
    single<ExportService> { JsonFileService(get<AppConfig>().exportConfig, get<DirectoryService>()) }
    single<MessageHandler> { KpiExportingMessageHandler(get<DataAccessorRestProxy>(), get<ExportService>()) }
    single<MessageReceiver<Topic>> { ActiveMqTopicReceiver(get<MqSessionFactory>(), JOB_SCHEDULER) }
    single<MqReceiverComposer> {
        ActiveMqReceiverComposer(
            get<MqSessionFactory>(),
            get<MessageReceiver<Topic>>().destination,
            listOf(get<MessageHandler>())
        )
    }
}
