package ch.bziswiler.pvlogger.dataaccessor.database.schema

import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.javatime.CurrentTimestamp
import org.jetbrains.exposed.v1.javatime.timestamp

object ResponseTable : IntIdTable("modbus_response_data") {
    val timestamp = timestamp("date_created").defaultExpression(CurrentTimestamp)
    val job_id = text("job_id")
    val request_time = timestamp("request_time")
    val request_id = ushort("request_id")
    val response_time = timestamp("response_time")
    val protocol_type = ushort("protocol_type")
    val data_length = ushort("data_length")
    val logic_device_id = ubyte("logic_device_id")
    val function_code = ubyte("function_code")
    val number_of_bytes = ubyte("number_of_bytes").nullable()
}
