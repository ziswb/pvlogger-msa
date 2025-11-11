package ch.bziswiler.pvlogger.exporter.configuration

object AppConfig {
    val dataAccessConfig: ModbusDataAccessConfig
        get() =
            ModbusDataAccessConfig(
                host = System.getenv("MODBUS_HOST") ?: "pvlogger-modbus-data-accessor-service.pvlogger.svc.cluster.local",
                port = System.getenv("MODBUS_PORT")?.toInt() ?: 8083,
            )
    val exportConfig: ExportConfig
        get() =
            ExportConfig(
                path = System.getenv("EXPORT_PATH") ?: "/pvlogger_export",
                name = System.getenv("EXPORT_NAME") ?: "pv_export_<site>",
            )
}

data class ModbusDataAccessConfig(
    val host: String,
    val port: Int,
)

data class ExportConfig(
    val path: String,
    val name: String,
)
