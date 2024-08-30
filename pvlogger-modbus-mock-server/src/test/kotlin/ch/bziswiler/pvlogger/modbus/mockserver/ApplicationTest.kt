package ch.bziswiler.pvlogger.modbus.mockserver

import ch.bziswiler.pvlogger.masterdata.modbus.Type
import ch.bziswiler.pvlogger.masterdata.modbus.Type.I16
import ch.bziswiler.pvlogger.masterdata.modbus.Type.I32
import ch.bziswiler.pvlogger.masterdata.modbus.Type.U16
import ch.bziswiler.pvlogger.modbus.ModbusRegisterValue
import ch.bziswiler.pvlogger.modbus.tcp.frame.FunctionCode
import ch.bziswiler.pvlogger.modbus.tcp.frame.MbapHeader
import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.server.testing.testApplication
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.core.writeInt
import io.ktor.utils.io.core.writeShort
import io.ktor.utils.io.core.writeUByte
import io.ktor.utils.io.core.writeUShort
import io.ktor.utils.io.readUTF8Line
import io.ktor.utils.io.writePacket
import io.ktor.utils.io.writeStringUtf8
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplicationTest {

    @Test
    fun test() = testApplication {
        application {
            module(testing = true)
            runBlocking {
                val selectorManager = SelectorManager(Dispatchers.IO)
                val socket = aSocket(selectorManager).tcp().connect("127.0.0.1", 8502)
                val receiveChannel = socket.openReadChannel()
                val sendChannel = socket.openWriteChannel(autoFlush = true)
                writeData(sendChannel, buildHeader(), buildFunctionCode(), buildData())
//                sendChannel.writeStringUtf8("King\n")
                val greeting2 = receiveChannel.readUTF8Line()
                socket.close()
                assertEquals("Hello, King!", greeting2)
            }
        }
    }
}

fun buildHeader(
    protocolIdentifier: UShort = 0u,
    protocolType: UShort = 0u,
    dataLength: UShort = 81u,
    logicDeviceId: UByte = 1u
) = MbapHeader(
    protocolIdentifier = protocolIdentifier,
    protocolType = protocolType,
    dataLength = dataLength,
    logicDeviceId = logicDeviceId
)

fun buildFunctionCode(code: UByte = 0x03u) = FunctionCode.from(code)

fun buildData() = listOf(
    RegisterUShortValue(value = 1u),
    RegisterIntValue(value = 2333),
    RegisterIntValue(value = 2416),
    RegisterIntValue(value = 2366),
    RegisterIntValue(value = -257),
    RegisterIntValue(value = -246),
    RegisterIntValue(value = -622),
    RegisterIntValue(value = -2401),
    RegisterIntValue(value = -968),
    RegisterShortValue(value = 896),
    RegisterShortValue(value = 4997),
    RegisterIntValue(value = 1295013),
    RegisterIntValue(value = 798831),
    RegisterIntValue(value = 0),
    RegisterUShortValue(value = 1u),
    RegisterIntValue(value = 4113),
    RegisterIntValue(value = 4142),
    RegisterIntValue(value = 4071),
    RegisterIntValue(value = -427),
    RegisterIntValue(value = -569),
    RegisterIntValue(value = -1405),
    RegisterUShortValue(value = 1u)
)

@OptIn(ExperimentalUnsignedTypes::class)
suspend fun writeData(
    channel: ByteWriteChannel,
    header: MbapHeader,
    functionCode: FunctionCode,
    data: List<RegisterValue<*>> = emptyList()
) {
    channel.writePacket {
        writeUShort(header.protocolIdentifier)
        writeUShort(header.protocolType)
        writeUShort(header.dataLength)
        writeUByte(header.logicDeviceId)
    }
    channel.writePacket { writeUByte(functionCode.value) }
    channel.writePacket {
        data.forEach {
            when (it.type) {
                U16 -> writeUShort(it.value as UShort)
                I16 -> writeShort(it.value as Short)
                I32 -> writeInt(it.value as Int)
                else -> UnsupportedOperationException("Unknown type: ${it.type}")
            }
        }
    }
}

interface RegisterValue<T> {
    val type: Type
    val value: T
}

data class RegisterShortValue(override val type: Type = I16, override val value: Short) : RegisterValue<Short>
data class RegisterUShortValue(override val type: Type = U16, override val value: UShort) : RegisterValue<UShort>
data class RegisterIntValue(override val type: Type = I32, override val value: Int) : RegisterValue<Int>

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
