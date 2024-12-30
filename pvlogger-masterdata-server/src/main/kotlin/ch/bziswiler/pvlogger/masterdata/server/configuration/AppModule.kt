package ch.bziswiler.pvlogger.masterdata.server.configuration

import ch.bziswiler.pvlogger.masterdata.server.database.access.MasterDataDao
import ch.bziswiler.pvlogger.shared.databbase.DatabaseFactory
import org.koin.dsl.module

val appModule = module {
    single<AppConfig> { AppConfig() }
    single<DatabaseFactory> { DatabaseFactory(get<AppConfig>().databaseConfig) }
    single<MasterDataDao> { MasterDataDao() }
}
