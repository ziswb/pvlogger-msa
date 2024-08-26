package ch.bziswiler.pvlogger.masterdata.server.di

import ch.bziswiler.pvlogger.masterdata.server.config.AppConfig
import ch.bziswiler.pvlogger.masterdata.server.database.DatabaseFactory
import ch.bziswiler.pvlogger.masterdata.server.database.DatabaseFactoryImpl
import ch.bziswiler.pvlogger.masterdata.server.modbus.schema.MasterDataDao
import ch.bziswiler.pvlogger.masterdata.server.modbus.services.MasterDataService
import org.koin.dsl.module

val appModule =
    module {
        single<AppConfig> { AppConfig() }
        single<DatabaseFactory> { DatabaseFactoryImpl(get()) }
        single<MasterDataDao> { MasterDataDao() }
        single<MasterDataService> { MasterDataService(get()) }
    }
