package ch.bziswiler.pvlogger.persistence.huawei.fusionsolar.schema

import ch.bziswiler.fusionsolar.model.ResponseObjectParams
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.insert

object FsResponseParamsExtended : IntIdTable("fs_response_params_extended") {
    val response = integer("response").references(FsResponse.id)
    var stationCodes = text("station_codes").nullable()
    var collectTime = long("collect_time").nullable()
    var currentTime = long("current_time").nullable()
}

fun FsResponseParamsExtended.save(responseId: EntityID<Int>, params: ResponseObjectParams?) {
    FsResponseParamsExtended.insert {
        it[response] = responseId.value
        it[stationCodes] = params?.stationCodes
        it[currentTime] = params?.currentTime
        it[collectTime] = params?.collectTime
    }
}
