package ch.bziswiler.pvlogger.shared.rest

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * Pins the KPI wire format. The data accessor serves it on `read-latest-kpi`; the frontend reads it into this DTO
 * with a client that ignores unknown keys, so a renamed field would silently become 0.0 there. The KPI exporter
 * reads the same JSON into its own Go struct (`pvlogger-kpi-exporter/internal/kpi/values.go`), whose tags must
 * match [WIRE_NAMES].
 */
class KpiValuesDtoTest {
    // Like the services: zero values are sent too (encodeDefaults). Otherwise strict, so unknown keys fail.
    private val json = Json { encodeDefaults = true }

    private val sample =
        KpiValuesDto(
            createdAt = 1_761_210_000_000,
            solarPower = 6.974,
            gridPower = -6.089,
            solarToHousePower = 0.885,
            gridToHousePower = 0.0,
            solarToGridPower = 6.089,
            ownConsumption = 0.885,
            dailyEnergyYield = 21.5,
            dailyEnergySelfCovered = 4.25,
            dailyEnergyExported = 17.25,
            dailyEnergyImported = 1.5,
            dailyEnergyConsumed = 5.75,
        )

    @Test
    fun `round-trips through JSON unchanged`() {
        assertEquals(sample, json.decodeFromString<KpiValuesDto>(json.encodeToString(sample)))
    }

    @Test
    fun `field names are the wire contract`() {
        val keys = json.parseToJsonElement(json.encodeToString(sample)).jsonObject.keys
        assertEquals(WIRE_NAMES, keys)
    }

    @Test
    fun `decodes the wire form strictly`() {
        // A field added or renamed on one side fails here.
        val wire =
            """
            {"createdAt":1761210000000,"solarPower":6.974,"gridPower":-6.089,"solarToHousePower":0.885,
             "gridToHousePower":0.0,"solarToGridPower":6.089,"ownConsumption":0.885,"dailyEnergyYield":21.5,
             "dailyEnergySelfCovered":4.25,"dailyEnergyExported":17.25,"dailyEnergyImported":1.5,
             "dailyEnergyConsumed":5.75}
            """.trimIndent()
        assertEquals(sample, json.decodeFromString<KpiValuesDto>(wire))
    }

    @Test
    fun `missing fields default to zero`() {
        assertEquals(KpiValuesDto(createdAt = 5), json.decodeFromString<KpiValuesDto>("""{"createdAt":5}"""))
    }

    private companion object {
        val WIRE_NAMES =
            setOf(
                "createdAt",
                "solarPower",
                "gridPower",
                "solarToHousePower",
                "gridToHousePower",
                "solarToGridPower",
                "ownConsumption",
                "dailyEnergyYield",
                "dailyEnergySelfCovered",
                "dailyEnergyExported",
                "dailyEnergyImported",
                "dailyEnergyConsumed",
            )
    }
}
