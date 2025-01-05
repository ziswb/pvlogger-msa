package ch.bziswiler.pvlogger.exporter.service

import kotlinx.serialization.Serializable

@Serializable
data class KpiExportRecord(
    val lastUpdateTime: String,
    val currentData: CurrentDataRecord? = CurrentDataRecord(),
    val currentDayData: CurrentDayDataRecord? = CurrentDayDataRecord(),
)

@Serializable
data class CurrentDataRecord(
    val pv_prod_akt_w: Double? = 0.0,
    val pv_verk_akt_w: Double? = 0.0,
    val pv_kauf_akt_w: Double? = 0.0,
    val pv_verb_akt_w: Double? = 0.0,
)

@Serializable
data class CurrentDayDataRecord(
    val pv_prod_tag_wh: Double? = 0.0,
    val pv_verk_tag_wh: Double? = 0.0,
    val pv_kauf_tag_wh: Double? = 0.0,
    val pv_verb_tag_wh: Double? = 0.0,
)
