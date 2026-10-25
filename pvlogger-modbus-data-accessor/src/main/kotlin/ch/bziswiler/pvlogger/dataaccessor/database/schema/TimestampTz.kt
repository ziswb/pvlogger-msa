package ch.bziswiler.pvlogger.dataaccessor.database.schema

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.datetime.InstantColumnType
import org.jetbrains.exposed.v1.core.vendors.currentDialect
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import kotlin.time.ExperimentalTime
import kotlin.time.toJavaInstant
import kotlin.time.toKotlinInstant

/**
 * An [Instant] column on a TIMESTAMPTZ that binds the value as a UTC [OffsetDateTime].
 *
 * Exposed's own `timestamp()` binds an Instant as a wall-clock time in the JVM zone,
 * which is ambiguous in the repeated hour of the autumn DST switch: PostgreSQL resolves
 * it to summer time, so readings from the second pass were stored one hour too early.
 */
@OptIn(ExperimentalTime::class)
class InstantTzColumnType : InstantColumnType<Instant>() {
    override fun toInstant(value: Instant) = value.toKotlinInstant()

    override fun fromInstant(instant: kotlin.time.Instant) = instant.toJavaInstant()

    override fun sqlType(): String = currentDialect.dataTypeProvider.timestampWithTimeZoneType()

    override fun notNullValueToDB(value: Instant): Any = OffsetDateTime.ofInstant(value, ZoneOffset.UTC)
}

fun Table.timestampTz(name: String): Column<Instant> = registerColumn(name, InstantTzColumnType())
