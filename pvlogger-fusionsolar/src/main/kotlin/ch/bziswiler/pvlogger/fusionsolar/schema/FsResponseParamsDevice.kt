package ch.bziswiler.pvlogger.fusionsolar.schema

import ch.bziswiler.pvlogger.fusionsolar.model.ResponseObjectParams
import ch.bziswiler.pvlogger.persistence.huawei.fusionsolar.schema.FsResponse
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.insert

object FsResponseParamsDevice : IntIdTable("fs_response_params_device") {
    val response = integer("response").references(FsResponse.id)
    var devIds = text("dev_ids").nullable()
    var devTypeId = integer("dev_type_id").nullable()
    var currentTime = long("current_time").nullable()
}

fun FsResponseParamsDevice.save(responseId: EntityID<Int>, params: ResponseObjectParams?) {
    FsResponseParamsDevice.insert {
        it[response] = responseId.value
        it[devIds] = params?.devIds
        it[devTypeId] = params?.devTypeId
        it[currentTime] = params?.currentTime
    }
}
