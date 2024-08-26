package ch.bziswiler.pvlogger.datacollector.services.modbus

import ch.bziswiler.pvlogger.datacollector.config.AppConfig
import ch.bziswiler.pvlogger.datacollector.modbus.tcp.ModbusTcpChannel
import ch.bziswiler.pvlogger.datacollector.modbus.tcp.request.ReadHoldingRegistersValueRequest
import ch.bziswiler.pvlogger.datacollector.services.IdProvider
import ch.bziswiler.pvlogger.masterdata.modbus.Category
import ch.bziswiler.pvlogger.masterdata.modbus.DeviceType
import ch.bziswiler.pvlogger.masterdata.modbus.DeviceType.INVERTER
import ch.bziswiler.pvlogger.masterdata.modbus.DeviceType.METER
import ch.bziswiler.pvlogger.masterdata.modbus.dto.ModbusRegisterDefinition
import ch.bziswiler.pvlogger.modbus.tcp.response.ModbusResponse
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Timer
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import java.time.LocalDateTime.now

@OptIn(ExperimentalUnsignedTypes::class)
abstract class ModbusBatchedDataCollectionJob(
    config: AppConfig,
    private val channel: ModbusTcpChannel,
    private val idProvider: IdProvider,
    private val masterDataService: MasterDataService
) : ModbusDataCollectionJob {
    private val logger = KotlinLogging.logger {}
    private val meterDeviceIds = config.modbusTcpConfig.meterDeviceIds
    private val inverterDeviceIds = config.modbusTcpConfig.inverterDeviceIds
    private val deviceIdToDeviceTypesMap = mutableMapOf<UByte, List<DeviceType>>()
    protected abstract val metricTimer: Timer
    protected abstract val metricCounter: Counter
    protected abstract val categories: List<Category>


    init {
        meterDeviceIds.intersect(inverterDeviceIds).toSet().associateWith { listOf(INVERTER, METER) }
            .forEach { entry -> deviceIdToDeviceTypesMap[entry.key] = entry.value }
        inverterDeviceIds.minus(meterDeviceIds).toSet().associateWith { listOf(INVERTER) }
            .forEach { entry -> deviceIdToDeviceTypesMap[entry.key] = entry.value }
        meterDeviceIds.minus(inverterDeviceIds).toSet().associateWith { listOf(METER) }
            .forEach { entry -> deviceIdToDeviceTypesMap[entry.key] = entry.value }
    }

    abstract fun storeResponse(response: ModbusResponse)

    abstract fun jobFinished(jobId: String)

    final override fun execute() {
        metricCounter.increment()
        metricTimer.recordCallable {
            logger.debug("ModbusInverterBatchedDataCollectionJobScaffold.execute()")
            val jobId = idProvider.nextJobId
            deviceIdToDeviceTypesMap.forEach { fetchRegisterValues(jobId, it.key, it.value) }
            jobFinished(jobId)
        }
    }

    private fun fetchRegisterValues(jobId: String, deviceId: UByte, deviceTypes: List<DeviceType>) {
        logger.debug("ModbusInverterBatchedDataCollectionJobScaffold.readAndStoreRegisterValues()")
        listOfRegisterDefinitionLists(deviceTypes).forEach { registerDefinitions ->
            runCatching {
                requestRegisterValues(buildRequest(jobId, deviceId, registerDefinitions))
            }.onSuccess {
                storeResponse(it)
            }.onFailure {
                logger.error(it) { "Fetching modbus data failed" }
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

    private fun requestRegisterValues(request: ReadHoldingRegistersValueRequest): ModbusResponse = runBlocking {
        logger.debug("ModbusInverterBatchedDataCollectionJobScaffold.requestRegisterValues()")
        channel.request(request)
    }

    private fun buildRequest(
        jobId: String,
        deviceId: UByte,
        registerDefinitions: List<ModbusRegisterDefinition>,
    ) = ReadHoldingRegistersValueRequest(jobId, now(), idProvider.nextRequestId, deviceId, registerDefinitions)

    private fun registerDefinitionList(
        deviceTypes: List<DeviceType>, categories: List<Category>
    ) = masterDataService.fetchRegisterDefinitions(deviceTypes, categories)
}
