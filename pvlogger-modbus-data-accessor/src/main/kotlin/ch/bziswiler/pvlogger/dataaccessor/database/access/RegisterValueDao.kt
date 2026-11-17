package ch.bziswiler.pvlogger.dataaccessor.database.access

import ch.bziswiler.pvlogger.dataaccessor.database.schema.IntegerValueTable
import ch.bziswiler.pvlogger.dataaccessor.database.schema.ResponseTable
import ch.bziswiler.pvlogger.dataaccessor.database.schema.ShortValueTable
import ch.bziswiler.pvlogger.dataaccessor.database.schema.StringValuesTable
import ch.bziswiler.pvlogger.dataaccessor.database.schema.UIntegerValueTable
import ch.bziswiler.pvlogger.dataaccessor.database.schema.UShortValueTable
import ch.bziswiler.pvlogger.shared.job.meta.JobId
import ch.bziswiler.pvlogger.shared.masterdata.modbus.RegisterAddress
import ch.bziswiler.pvlogger.shared.masterdata.modbus.Type
import io.github.oshai.kotlinlogging.KotlinLogging
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.SortOrder.DESC
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.select
import ch.bziswiler.pvlogger.shared.database.dbTransaction

object RegisterValueDao {
    private const val ONE = 1

    private val logger = KotlinLogging.logger {}
    private val responseTimeDescending = ResponseTable.response_time to DESC
    private val idDescending = ResponseTable.id to DESC

    suspend fun readLatestRegisterValue(
        type: Type,
        deviceId: Int,
        registerAddress: RegisterAddress,
        jobId: JobId,
    ): Any? = dbTransaction { latestRegisterValue(type, deviceId, registerAddress, jobId) }

    /** [readLatestRegisterValue] inside the caller's transaction (tests attach an SQL logger to it). */
    internal fun latestRegisterValue(
        type: Type,
        deviceId: Int,
        registerAddress: RegisterAddress,
        jobId: JobId,
    ): Any? =
        when (type) {
            Type.U16, Type.BITFIELD_16 -> {
                queryLatestValue(
                    UShortValueTable,
                    UShortValueTable.register_value,
                    UShortValueTable.register_address,
                    UShortValueTable.response_id,
                    registerAddress,
                    deviceId,
                    jobId,
                )?.toUShort()
            }

            Type.U32, Type.BITFIELD_32 -> {
                queryLatestValue(
                    UIntegerValueTable,
                    UIntegerValueTable.register_value,
                    UIntegerValueTable.register_address,
                    UIntegerValueTable.response_id,
                    registerAddress,
                    deviceId,
                    jobId,
                )?.toUInt()
            }

            Type.I16 -> {
                queryLatestValue(
                    ShortValueTable,
                    ShortValueTable.register_value,
                    ShortValueTable.register_address,
                    ShortValueTable.response_id,
                    registerAddress,
                    deviceId,
                    jobId,
                )
            }

            Type.I32 -> {
                queryLatestValue(
                    IntegerValueTable,
                    IntegerValueTable.register_value,
                    IntegerValueTable.register_address,
                    IntegerValueTable.response_id,
                    registerAddress,
                    deviceId,
                    jobId,
                )
            }

            Type.STR -> {
                queryLatestValue(
                    StringValuesTable,
                    StringValuesTable.register_value,
                    StringValuesTable.register_address,
                    StringValuesTable.response_id,
                    registerAddress,
                    deviceId,
                    jobId,
                )
            }

            Type.MLD -> {
                throw NotImplementedError()
            }
        }

    /**
     * The value of [registerAddress] in the newest response of run [jobId] for device [deviceId] that carries it.
     *
     * Two statements on purpose, never a join (backlog 38/41). With stale planner statistics the joined query
     * could pick a nested loop that ran 29.6 s; here each statement reads one table through its own composite index
     * (V11 `(job_id, logic_device_id)`, V4/V11 `(register_address, response_id)`), so no join order is left to get
     * wrong. A run has only a few responses per device, so the id list stays short. Runs in the caller's
     * transaction.
     */
    private fun <T> queryLatestValue(
        valueTable: Table,
        valueColumn: Column<T?>,
        registerAddressColumn: Column<Int>,
        responseIdColumn: Column<Int>,
        registerAddress: RegisterAddress,
        deviceId: Int,
        jobId: JobId,
    ): T? {
        val responseIdsNewestFirst =
            ResponseTable
                .select(ResponseTable.id)
                .where(
                    ResponseTable.job_id
                        .eq(jobId.id)
                        .and(ResponseTable.logic_device_id.eq(deviceId.toUByte())),
                ).orderBy(responseTimeDescending, idDescending)
                .map { row -> row[ResponseTable.id].value }

        val valuesByResponseId =
            if (responseIdsNewestFirst.isEmpty()) {
                emptyMap()
            } else {
                valueTable
                    .select(responseIdColumn, valueColumn)
                    .where(
                        registerAddressColumn
                            .eq(registerAddress.value)
                            .and(responseIdColumn.inList(responseIdsNewestFirst)),
                    ).associate { row -> row[responseIdColumn] to row[valueColumn] }
            }

        val latestResponseId = responseIdsNewestFirst.firstOrNull { it in valuesByResponseId }
        return if (latestResponseId == null) {
            logger.info { "No register value found for device ID '$deviceId' and register address '$registerAddress'." }
            null
        } else {
            valuesByResponseId[latestResponseId]
        }
    }
}
