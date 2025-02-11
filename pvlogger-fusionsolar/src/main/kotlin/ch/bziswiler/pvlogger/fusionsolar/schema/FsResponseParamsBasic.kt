package ch.bziswiler.pvlogger.persistence.huawei.fusionsolar.schema

import ch.bziswiler.fusionsolar.model.ResponseObjectParams
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.insert

object FsResponseParamsBasic : IntIdTable("fs_response_params_basic") {
    val response = integer("response").references(FsResponse.id)
    var stationCodes = text("station_codes").nullable()
    var currentTime = long("current_time").nullable()
}

fun FsResponseParamsBasic.save(responseId: EntityID<Int>, params: ResponseObjectParams?) {
    FsResponseParamsBasic.insert {
        it[response] = responseId.value
        it[stationCodes] = params?.stationCodes
        it[currentTime] = params?.currentTime
    }
}
