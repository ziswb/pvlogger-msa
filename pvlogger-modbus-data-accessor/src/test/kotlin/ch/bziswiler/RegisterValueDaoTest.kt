package ch.bziswiler

import ch.bziswiler.pvlogger.dataaccessor.database.access.RegisterValueDao.latestRegisterValue
import ch.bziswiler.pvlogger.dataaccessor.database.access.RegisterValueDao.readLatestRegisterValue
import ch.bziswiler.pvlogger.shared.job.meta.JobId
import ch.bziswiler.pvlogger.shared.masterdata.modbus.RegisterAddress
import ch.bziswiler.pvlogger.shared.masterdata.modbus.Type
import ch.bziswiler.testutils.TestBase
import ch.bziswiler.testutils.TestDataFactory.seedIntegerValue
import ch.bziswiler.testutils.TestDataFactory.seedResponseRow
import ch.bziswiler.testutils.TestDataFactory.seedUIntegerValue
import org.jetbrains.exposed.v1.core.SqlLogger
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.core.statements.StatementContext
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.temporal.ChronoUnit.MINUTES

class RegisterValueDaoTest : TestBase() {
    private val base = Instant.parse("2026-10-23T10:00:00Z")
    private val dongle: UByte = 100u

    /**
     * Run A for the dongle has three responses; the newest one does not carry 37516, so the value comes from the
     * one before it. A newer value of another device in the same run, and of the same device in another run, must
     * not leak in.
     */
    private fun seedTwoRuns() {
        seedResponseRow(id = 1, jobId = "run-A", logicDeviceId = dongle, responseTime = base)
        seedIntegerValue(responseId = 1, registerAddress = 37516, value = 1)
        seedResponseRow(id = 2, jobId = "run-A", logicDeviceId = dongle, responseTime = base.plus(1, MINUTES))
        seedIntegerValue(responseId = 2, registerAddress = 37516, value = 2)
        seedUIntegerValue(responseId = 2, registerAddress = 37500, value = 885u)
        seedResponseRow(id = 3, jobId = "run-A", logicDeviceId = dongle, responseTime = base.plus(2, MINUTES))
        seedIntegerValue(responseId = 3, registerAddress = 37502, value = -5)
        seedResponseRow(id = 4, jobId = "run-A", logicDeviceId = 1u, responseTime = base.plus(3, MINUTES))
        seedIntegerValue(responseId = 4, registerAddress = 37516, value = 40)
        seedResponseRow(id = 5, jobId = "run-B", logicDeviceId = dongle, responseTime = base.plus(4, MINUTES))
        seedIntegerValue(responseId = 5, registerAddress = 37516, value = 50)
        seedUIntegerValue(responseId = 5, registerAddress = 37500, value = 999u)
    }

    private suspend fun read(type: Type, address: Int, job: String = "run-A") =
        readLatestRegisterValue(type, dongle.toInt(), RegisterAddress(address), JobId(job))

    @Test
    fun `newest response of the run and device that carries the register wins`() = withApp {
        startApplication()
        seedTwoRuns()

        assertEquals(2, read(Type.I32, 37516))
        assertEquals(-5, read(Type.I32, 37502))
        assertEquals(885u, read(Type.U32, 37500))
        assertEquals(50, read(Type.I32, 37516, job = "run-B"))
    }

    @Test
    fun `missing register, run or device gives null`() = withApp {
        startApplication()
        seedTwoRuns()

        assertNull(read(Type.I32, 37504))
        assertNull(read(Type.I32, 37516, job = "run-C"))
        assertNull(readLatestRegisterValue(Type.I32, 16, RegisterAddress(37516), JobId("run-A")))
    }

    @Test
    fun `lookup runs as two single-table statements, never a join`() = withApp {
        startApplication()
        seedTwoRuns()

        // The 2026-08 outage (backlog 38) was the planner choosing a bad join order on stale statistics.
        // Without a join there is no order to choose; this pins that.
        val statements = mutableListOf<String>()
        val value =
            transaction {
                addLogger(
                    object : SqlLogger {
                        override fun log(context: StatementContext, transaction: Transaction) {
                            statements += context.sql(transaction)
                        }
                    },
                )
                latestRegisterValue(Type.I32, dongle.toInt(), RegisterAddress(37516), JobId("run-A"))
            }

        assertEquals(2, value)
        assertEquals(2, statements.size, "statements: $statements")
        statements.forEach { assertFalse(it.contains("JOIN", ignoreCase = true), it) }
    }
}
