package ch.bziswiler.testutils

import ch.bziswiler.pvlogger.dataaccessor.configuration.AppConfig
import ch.bziswiler.pvlogger.dataaccessor.configuration.appModule
import ch.bziswiler.pvlogger.dataaccessor.module
import ch.bziswiler.pvlogger.shared.databbase.DatabaseFactory
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.RegisterExtension
import org.koin.test.KoinTest
import org.koin.test.junit5.KoinTestExtension

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class TestBase : KoinTest {
    @JvmField
    @RegisterExtension
    @Suppress("unused")
    val koinTestExtension = KoinTestExtension.create { modules(appModule) }

    fun <R> withApp(applicationTestBuilder: suspend ApplicationTestBuilder.() -> R) =
        testApplication {
            application {
                module(testing = true)
                org.koin.dsl.module {
                    single<AppConfig> { AppConfig() }
                    single<DatabaseFactory> { DatabaseFactory(get<AppConfig>().databaseConfig) }
                }
            }
            environment {
                config =
                    MapApplicationConfig(
                        "ktor.server.isProd" to "false",
                        "database.driverClass" to "org.h2.Driver",
                        "database.dbServerUrl" to "jdbc:h2:mem:mem;DATABASE_TO_UPPER=false;MODE=PostgreSQL",
                        "database.dbName" to "pvmasterdata",
                        "database.dbServerCanonicalUrl" to "jdbc:h2:mem:mem;DATABASE_TO_UPPER=false;MODE=PostgreSQL",
                        "database.user" to "sa",
                        "database.password" to "",
                        "database.maxPoolSize" to "1",
                        "masterdata.import.meterMasterDataPath" to "",
                        "masterdata.import.inverterMasterDataPath" to "",
                        "masterdata.import.deviceIdentifiersMasterDataPath" to "",
                        "messagequeue.clientId" to "pvlogger-modbus-data-accessor",
                        "messagequeue.user" to "artemis",
                        "messagequeue.passwd" to "artemis",
                        "messagequeue.brokerUrl" to "",
                    )
            }
            applicationTestBuilder.invoke(this)
        }
}
