package ch.bziswiler.pvlogger.masterdata.server.configuration

import ch.bziswiler.pvlogger.masterdata.server.database.connection.DatabaseFactory
import ch.bziswiler.pvlogger.masterdata.server.database.access.MasterDataDao
import org.koin.dsl.module

val appModule =
    module {
        single<AppConfig> { AppConfig() }
        single<DatabaseFactory> { DatabaseFactory(get()) }
        single<MasterDataDao> { MasterDataDao() }
    }
