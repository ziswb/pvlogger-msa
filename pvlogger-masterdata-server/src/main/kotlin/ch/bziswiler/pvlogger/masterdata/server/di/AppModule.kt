package ch.bziswiler.pvlogger.masterdata.server.di

import ch.bziswiler.pvlogger.masterdata.server.config.AppConfig
import ch.bziswiler.pvlogger.masterdata.server.database.connection.DatabaseFactory
import ch.bziswiler.pvlogger.masterdata.server.modbus.schema.MasterDataDao
import org.koin.dsl.module

val appModule =
    module {
        single<AppConfig> { AppConfig() }
        single<DatabaseFactory> { DatabaseFactory(get()) }
        single<MasterDataDao> { MasterDataDao() }
    }
