package ch.bziswiler.pvlogger.modbus.mockserver

import ch.bziswiler.pvlogger.masterdata.modbus.Type
import ch.bziswiler.pvlogger.modbus.mockserver.utils.MockDataBuilder.buildData
import ch.bziswiler.pvlogger.modbus.mockserver.utils.MockDataBuilder.buildFunctionCode
import ch.bziswiler.pvlogger.modbus.mockserver.utils.MockDataBuilder.buildHeader
import ch.bziswiler.pvlogger.modbus.mockserver.utils.MockDataReader
import ch.bziswiler.pvlogger.modbus.mockserver.utils.MockDataWriter
import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.lang.Thread.sleep
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplicationTest {

    private val hostname = "127.0.0.1"

    @Test
    fun test() = testApplication {
        environment {
            config =
                MapApplicationConfig(
                    "ktor.socket-server.tcp-port" to "9090",
                    "ktor.socket-server.ip" to hostname,
                )
        }
        application {
            module()
            sleep(1000) // TODO: this is definitely not the way to do it. Also the 'application'-block should be closed before the testing code starts
            runBlocking {
                val selectorManager = SelectorManager(Dispatchers.IO)
                val socket = aSocket(selectorManager).tcp().connect(hostname, 9090)
                val reader = MockDataReader(socket.openReadChannel())
                val writer = MockDataWriter(socket.openWriteChannel(autoFlush = true))
                writer.writeData(buildHeader(), buildFunctionCode(), buildData())
                reader.readData(mockValueTypes())
                socket.close()
                assertEquals(0u, reader.protocolIdentifier)
                assertEquals(0u, reader.protocolType)
                assertEquals(81u, reader.dataLength)
                assertEquals(1u, reader.logicDeviceId)
                assertEquals(mockValueTypes().size, reader.values.size)
                assertEquals(1u, reader.values.get(0) as UShort)
                assertEquals(2333, reader.values.get(1) as Int)
            }
        }
    }

    private fun mockValueTypes(): MutableList<Type> {
        val valueTypes = mutableListOf<Type>()
        valueTypes.add(Type.U16)
        valueTypes.add(Type.I32)
        valueTypes.add(Type.I32)
        valueTypes.add(Type.I32)
        valueTypes.add(Type.I32)
        valueTypes.add(Type.I32)
        valueTypes.add(Type.I32)
        valueTypes.add(Type.I32)
        valueTypes.add(Type.I32)
        valueTypes.add(Type.I16)
        valueTypes.add(Type.I16)
        valueTypes.add(Type.I32)
        valueTypes.add(Type.I32)
        valueTypes.add(Type.I32)
        valueTypes.add(Type.U16)
        valueTypes.add(Type.I32)
        valueTypes.add(Type.I32)
        valueTypes.add(Type.I32)
        valueTypes.add(Type.I32)
        valueTypes.add(Type.I32)
        valueTypes.add(Type.I32)
        valueTypes.add(Type.U16)
        return valueTypes
    }
}

/*
ID  DevType no  name            r/w type    unit    gain   address  quantity    scope                                                               category
8   METER   8   Active power    RO  I32     W       ONE     37113   2           ">0: feed-in to the power grid. <0: supply from the power grid."    DYNAMIC

response id: 8554357
protocol type     0
data length:     81
dev id:           1
function code:    3
number of bytes: 78

register    type    value
val _37100 = RegisterUShortValue(value = 1u)
val _37101 = RegisterIntValue(value = 2333)
val _37103 = RegisterIntValue(value = 2416)
val _37105 = RegisterIntValue(value = 2366)
val _37107 = RegisterIntValue(value = -257)
val _37109 = RegisterIntValue(value = -246)
val _37111 = RegisterIntValue(value = -622)
val _37113 = RegisterIntValue(value = -2401)
val _37115 = RegisterIntValue(value = -968)
val _37117 = RegisterShortValue(value = 896)
val _37118 = RegisterShortValue(value = 4997)
val _37119 = RegisterIntValue(value = 1295013)
val _37121 = RegisterIntValue(value = 798831)
val _37123 = RegisterIntValue(value = 0)
val _37125 = RegisterUShortValue(value = 1u)
val _37126 = RegisterIntValue(value = 4113)
val _37128 = RegisterIntValue(value = 4142)
val _37130 = RegisterIntValue(value = 4071)
val _37132 = RegisterIntValue(value = -427)
val _37134 = RegisterIntValue(value = -569)
val _37136 = RegisterIntValue(value = -1405)
val _37138 = RegisterUShortValue(value = 1u)

1. Header

        MbapHeader(
            protocolIdentifier = channel.readUShort(),
            protocolType = channel.readUShort(),
            dataLength = channel.readUShort(),
            logicDeviceId = channel.readUByte(),
        )

2. Function Code

        val functionCode = from(channel.readUByte())

3. Number of Bytes

        channel.readUByte()

4. Registers

    override suspend fun readRegisters(channel: ByteReadChannel) =
        registers.fold(listOf<ModbusRegisterValue>()) { list, register ->
            list.plus(readRegisterValue(register, channel))
        }

    private suspend fun readRegisterValue(register: ModbusRegisterDefinition, channel: ByteReadChannel) =
        when (register.type) {
            U16 -> ModbusRegisterUShortValue(register, channel.readUShort())
            U32 -> ModbusRegisterUIntValue(register, channel.readUInt())
            I16 -> ModbusRegisterShortValue(register, channel.readShort())
            I32 -> ModbusRegisterIntValue(register, channel.readInt())
            STR -> ModbusRegisterStringValue(register, channel.readString(toNOfBytes(register.quantity.value)))
            MLD -> ModbusRegisterShortArrayValue(register, channel.readMld(register.quantity.value))
            BITFIELD_16 -> ModbusRegisterUShortValue(register, channel.readUShort())
            BITFIELD_32 -> ModbusRegisterUIntValue(register, channel.readUInt())
        }

 */
