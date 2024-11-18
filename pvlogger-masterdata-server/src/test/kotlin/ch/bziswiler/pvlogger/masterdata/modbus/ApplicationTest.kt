package ch.bziswiler.pvlogger.masterdata.modbus

import ch.bziswiler.pvlogger.masterdata.modbus.testutils.BaseApplicationTest
import ch.bziswiler.pvlogger.masterdata.server.configuration.ROOT_PATH
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ApplicationTest : BaseApplicationTest() {
    @Test
    fun testRoot() =
        withApp {
            client.get(ROOT_PATH).apply {
                assertEquals(HttpStatusCode.NotFound, status)
            }
        }
}
