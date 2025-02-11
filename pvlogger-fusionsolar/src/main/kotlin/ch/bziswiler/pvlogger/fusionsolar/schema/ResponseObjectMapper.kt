package ch.bziswiler.pvlogger.persistence.huawei.fusionsolar.schema

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.insertAndGetId
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.reflect.full.memberProperties

class ResponseObjectMapper {
    companion object {
        inline fun <reified T> saveGenericResponse(response: T): EntityID<Int> =
            FsResponse.insertAndGetId { columns ->
                columns[timestamp] = ZonedDateTime.now(ZoneId.systemDefault()).toInstant()
                response?.value<Boolean>("success")?.let { value -> columns[success] = value }
                response?.value<Int>("failCode")?.let { value -> columns[failCode] = value }
                response?.value<String>("message")?.let { value -> columns[message] = value }
                columns[raw] = Json.encodeToString(response)
            }
    }
}

@Throws(IllegalAccessException::class, ClassCastException::class)
inline fun <reified T> Any.value(fieldName: String): T? {
    this::class.memberProperties.forEach { kCallable ->
        if (fieldName == kCallable.name) {
            return kCallable.getter.call(this) as T?
        }
    }
    return null
}
