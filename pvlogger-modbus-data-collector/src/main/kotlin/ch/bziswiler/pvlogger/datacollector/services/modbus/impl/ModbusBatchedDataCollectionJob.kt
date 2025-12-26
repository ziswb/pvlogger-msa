package ch.bziswiler.pvlogger.datacollector.services.modbus.impl

import ch.bziswiler.pvlogger.datacollector.configuration.ModbusTcpConfig
import ch.bziswiler.pvlogger.datacollector.modbus.tcp.ModbusTcpChannel
import ch.bziswiler.pvlogger.datacollector.modbus.tcp.request.ReadRegistersValueRequest
import ch.bziswiler.pvlogger.datacollector.services.IdProvider
import ch.bziswiler.pvlogger.datacollector.services.modbus.ModbusDataCollectionJob
import ch.bziswiler.pvlogger.datacollector.services.rest.MasterDataService
import ch.bziswiler.pvlogger.datacollector.services.rest.StoreModbusValuesMessageSender
import ch.bziswiler.pvlogger.shared.job.meta.JobId
import ch.bziswiler.pvlogger.shared.masterdata.modbus.Category
import ch.bziswiler.pvlogger.shared.masterdata.modbus.DeviceType
import ch.bziswiler.pvlogger.shared.masterdata.modbus.dto.ModbusRegisterDefinition
import ch.bziswiler.pvlogger.shared.modbus.tcp.response.ModbusRegisterValueResponse
import ch.bziswiler.pvlogger.shared.modbus.tcp.response.ModbusResponse
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import java.time.LocalDateTime

@OptIn(ExperimentalUnsignedTypes::class)
abstract class ModbusBatchedDataCollectionJob(
    config: ModbusTcpConfig,
    private val channel: ModbusTcpChannel,
    private val idProvider: IdProvider,
    private val masterDataService: MasterDataService,
    private val persistenceMessageSender: StoreModbusValuesMessageSender,
) : ModbusDataCollectionJob {

    private val logger = KotlinLogging.logger {}

    private val meterDeviceIds = config.meterDeviceIds
    private val inverterDeviceIds = config.inverterDeviceIds
    private val deviceIdToDeviceTypesMap = mutableMapOf<UByte, List<DeviceType>>()

    protected abstract val categories: List<Category>

    init {
        meterDeviceIds
            .intersect(inverterDeviceIds).toSet()
            .associateWith { listOf(DeviceType.INVERTER, DeviceType.METER) }
            .forEach { entry -> deviceIdToDeviceTypesMap[entry.key] = entry.value }
        inverterDeviceIds
            .minus(meterDeviceIds).toSet()
            .associateWith { listOf(DeviceType.INVERTER) }
            .forEach { entry -> deviceIdToDeviceTypesMap[entry.key] = entry.value }
        meterDeviceIds
            .minus(inverterDeviceIds).toSet()
            .associateWith { listOf(DeviceType.METER) }
            .forEach { entry -> deviceIdToDeviceTypesMap[entry.key] = entry.value }
    }


    private fun storeResponse(response: ModbusResponse) {
        logger.debug("ModbusBatchedDataCollectionJob.storeResponse()")
        when (response) {
            is ModbusRegisterValueResponse -> persistenceMessageSender.sendValuesToStore(response)
            else -> throw UnsupportedOperationException("'storeResponse()' does not support '${response::class.java.canonicalName}'")
        }
    }

    final override fun execute() {
            val jobId = JobId(idProvider.nextJobId)
            runCatching {
                logger.debug { "ModbusInverterBatchedDataCollectionJobScaffold.execute().runCatching()" }
                deviceIdToDeviceTypesMap.forEach { fetchRegisterValues(jobId, it.key, it.value) }
            }.onSuccess {
                logger.debug { "ModbusInverterBatchedDataCollectionJobScaffold.execute().onSuccess()" }
                // TODO: send message "job 'jobId' succeeded"
            }.onFailure {
                logger.error(it) { "ModbusInverterBatchedDataCollectionJobScaffold.execute().onFailure()" }
                // TODO: send message "job 'jobId' failed"
                throw it
            }
    }

    private fun fetchRegisterValues(jobId: JobId, deviceId: UByte, deviceTypes: List<DeviceType>) {
        logger.debug { "ModbusInverterBatchedDataCollectionJobScaffold.readAndStoreRegisterValues()" }
        listOfRegisterDefinitionLists(deviceTypes).forEach { registerDefinitions ->
            runCatching {
                logger.debug { "ModbusInverterBatchedDataCollectionJobScaffold.fetchRegisterValues().runCatching()" }
                requestRegisterValues(buildRequest(jobId, deviceId, registerDefinitions))
            }.onSuccess {
                logger.debug { "ModbusInverterBatchedDataCollectionJobScaffold.fetchRegisterValues().onSuccess()" }
                storeResponse(it)
            }.onFailure {
                logger.error(it) { "Fetching modbus data failed" }
                throw it
            }
        }
    }

    private fun listOfRegisterDefinitionLists(deviceTypes: List<DeviceType>) = registerDefinitionList(
        deviceTypes,
        categories
    ).fold(mutableListOf<MutableList<ModbusRegisterDefinition>>()) { list, definition ->
        updateListOfRegisterDefinitions(list, definition)
        list
    }

    private fun updateListOfRegisterDefinitions(
        list: MutableList<MutableList<ModbusRegisterDefinition>>,
        definition: ModbusRegisterDefinition,
    ) {
        if (list.isEmpty() || isNotSuccessor(list, definition)) {
            list.add(mutableListOf(definition))
        } else {
            list.last().add(definition)
        }
    }

    private fun isNotSuccessor(
        listOfRegisters: List<MutableList<ModbusRegisterDefinition>>,
        nextRegister: ModbusRegisterDefinition,
    ): Boolean {
        val lastRegister = listOfRegisters.last().last()
        return (lastRegister.registerAddress.value + lastRegister.quantity.value) != nextRegister.registerAddress.value
    }

    private fun requestRegisterValues(request: ReadRegistersValueRequest): ModbusResponse = runBlocking {
        logger.debug("ModbusInverterBatchedDataCollectionJobScaffold.requestRegisterValues()")
        channel.request(request)
    }

    private fun buildRequest(
        jobId: JobId,
        deviceId: UByte,
        registerDefinitions: List<ModbusRegisterDefinition>,
    ) = ReadRegistersValueRequest(
        jobId.id,
        LocalDateTime.now(),
        idProvider.nextRequestId,
        deviceId,
        registerDefinitions
    )

    private fun registerDefinitionList(
        deviceTypes: List<DeviceType>, categories: List<Category>
    ) = masterDataService.fetchRegisterDefinitions(deviceTypes, categories)
}