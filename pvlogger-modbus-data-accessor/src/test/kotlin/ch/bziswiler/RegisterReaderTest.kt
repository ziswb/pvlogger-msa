package ch.bziswiler

import ch.bziswiler.pvlogger.dataaccessor.database.schema.IntegerValueTable
import ch.bziswiler.pvlogger.dataaccessor.database.schema.ResponseTable
import ch.bziswiler.pvlogger.shared.rest.DataAccessorPaths
import ch.bziswiler.testutils.TestBase
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.http.appendPathSegments
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDateTime.now

class RegisterReaderTest : TestBase() {

    private fun setUpDatabase() {
        transaction {
            ResponseTable.insert {
                it[ResponseTable.id] = 1
                it[timestamp] = now().minusDays(1)
                it[response_time] = now().minusDays(1)
                it[request_time] = now().minusDays(1)
                it[job_id] = "1"
                it[request_id] = 1u
                it[logic_device_id] = 1u
                it[function_code] = 1u
                it[data_length] = 1u
                it[protocol_type] = 1u
            }
        }
        transaction {
            ResponseTable.insert {
                it[ResponseTable.id] = 2
                it[timestamp] = now()
                it[response_time] = now()
                it[request_time] = now()
                it[job_id] = "2"
                it[request_id] = 2u
                it[logic_device_id] = 1u
                it[function_code] = 1u
                it[data_length] = 1u
                it[protocol_type] = 1u
            }
        }
        transaction {
            IntegerValueTable.insert {
                it[response_id] = 1
                it[register_address] = 1
                it[register_value] = 1
            }
        }
        transaction {
            IntegerValueTable.insert {
                it[response_id] = 2
                it[register_address] = 1
                it[register_value] = 2
            }
        }
    }


    @Test
    fun `test register reader`() = withApp {
        startApplication()
        setUpDatabase()
        client.get("/${DataAccessorPaths.DATA_ACCESSOR_READ_KW_TODAY_SO_FAR_PATH}") {
            url {
                appendPathSegments("I32", "1")
            }
        }.apply {
            assertEquals(OK, status)
            val string = bodyAsText()
            assertEquals("1", string)
        }
    }
}
