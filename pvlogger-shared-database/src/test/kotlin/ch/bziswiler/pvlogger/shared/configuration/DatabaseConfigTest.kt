package ch.bziswiler.pvlogger.shared.configuration

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DatabaseConfigTest {
    private fun config(
        dbUrlParams: String = "?reWriteBatchedInserts=true",
        sslMode: String = DatabaseConfig.DEFAULT_SSL_MODE,
    ) = DatabaseConfig(
        driverClass = "org.postgresql.Driver",
        dbServerUrl = "jdbc:postgresql://db:5432/",
        dbName = "pvdata",
        user = "pvlogger",
        password = "pw",
        maxPoolSize = 1,
        dbUrlParams = dbUrlParams,
        sslMode = sslMode,
    )

    @Test
    fun `every URL carries the TLS mode, prefer by default`() {
        val config = config()

        assertEquals("jdbc:postgresql://db:5432/pvdata?reWriteBatchedInserts=true&sslmode=prefer", config.dbServerCanonicalUrl)
        assertEquals("jdbc:postgresql://db:5432/postgres?reWriteBatchedInserts=true&sslmode=prefer", config.maintenanceUrl)
    }

    @Test
    fun `the TLS mode stands alone when there are no other parameters`() {
        assertEquals("jdbc:postgresql://db:5432/pvdata?sslmode=require", config(dbUrlParams = "", sslMode = "require").dbServerCanonicalUrl)
    }

    @Test
    fun `an unknown TLS mode is rejected at startup`() {
        assertFailsWith<IllegalArgumentException> { config(sslMode = "on") }
    }
}
