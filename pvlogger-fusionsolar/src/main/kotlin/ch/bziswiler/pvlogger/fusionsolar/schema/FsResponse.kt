package ch.bziswiler.pvlogger.persistence.huawei.fusionsolar.schema

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.timestamp

object FsResponse : IntIdTable("fs_response") {
    val timestamp = timestamp("timestamp")
    val success = bool("success")
    val failCode = integer("fail_code")
    val message = text("message").nullable()
    val raw = text("raw")
}
