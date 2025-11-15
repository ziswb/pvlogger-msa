package ch.bziswiler.pvlogger.masterdata.modbus.testutils

import ch.bziswiler.pvlogger.masterdata.server.configuration.appModule
import ch.bziswiler.pvlogger.masterdata.server.module
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.RegisterExtension
import org.koin.test.KoinTest
import org.koin.test.junit5.KoinTestExtension

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class BaseApplicationTest : KoinTest {
    @JvmField
    @RegisterExtension
    @Suppress("unused")
    val koinTestExtension = KoinTestExtension.create { modules(appModule) }

    fun <R> withApp(applicationTestBuilder: suspend ApplicationTestBuilder.() -> R) =
        testApplication {
            application {
                module(testing = true)
            }
            environment {
                config =
                    MapApplicationConfig(
                        "ktor.server.isProd" to "false",
                        "database.driverClass" to "org.h2.Driver",
                        "database.dbServerUrl" to "jdbc:h2:mem:mem;DATABASE_TO_UPPER=false;MODE=PostgreSQL;INIT=CREATE SCHEMA IF NOT EXISTS public\\;SET SCHEMA public\\;",
                        "database.dbName" to "pvmasterdata",
                        "database.dbServerCanonicalUrl" to "jdbc:h2:mem:mem;DATABASE_TO_UPPER=false;MODE=PostgreSQL;INIT=CREATE SCHEMA IF NOT EXISTS public\\;SET SCHEMA public\\;",
                        "database.user" to "sa",
                        "database.password" to "",
                        "database.maxPoolSize" to "1",
                        "masterdata.import.meterMasterDataPath" to "",
                        "masterdata.import.inverterMasterDataPath" to "",
                        "masterdata.import.deviceIdentifiersMasterDataPath" to "",
                    )
            }
            applicationTestBuilder.invoke(this)
        }
}
