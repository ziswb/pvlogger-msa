package ch.bziswiler.pvlogger.fusionsolar.configuration

import ch.bziswiler.pvlogger.shared.configuration.DatabaseConfig

private const val JOB_NAME = "JOB_NAME"
private const val BASE_URL_OLD = "BASE_URL_OLD"
private const val BASE_URL_NEW = "BASE_URL_NEW"
private const val USERNAME = "USERNAME"
private const val PASSWORD = "FUSIONSOLAR_PASSWORD"
private const val DB_USER = "DB_USER"
private const val DB_PASS = "DB_PASSWORD"
private const val MAX_POOL_SIZE = "MAX_POOL_SIZE"
private const val DB_SERVER_CANONICAL_URL = "DB_SERVER_CANONICAL_URL"
private const val DB_NAME = "DB_NAME"
private const val DB_SERVER_URL = "DB_SERVER_URL"
private const val DRIVER_CLASS = "DRIVER_CLASS"

object AppConfig {
    val databaseConfig: DatabaseConfig
        get() =
            DatabaseConfig(
                driverClass = System.getenv(DRIVER_CLASS) ?: "org.h2.Driver",
                driverDelegateClass = "",
                dbServerUrl = System.getenv(DB_SERVER_URL) ?: "jdbc:h2:mem:mem;DATABASE_TO_UPPER=false;MODE=PostgreSQL",
                dbName = System.getenv(DB_NAME) ?: "fsdata",
                dbServerCanonicalUrl =
                    System.getenv(DB_SERVER_CANONICAL_URL)
                        ?: "jdbc:h2:mem:mem;DATABASE_TO_UPPER=false;MODE=PostgreSQL",
                user = System.getenv(DB_USER) ?: "root",
                password = System.getenv(DB_PASS) ?: "password",
                maxPoolSize = System.getenv(MAX_POOL_SIZE)?.toInt() ?: 1,
            )
    val fusionSolarConfig: FusionSolarConfig
        get() =
            FusionSolarConfig(
                username = System.getenv(USERNAME) ?: "<fusionsolar-user>",
                password = System.getenv(PASSWORD) ?: "",
                baseUrlOld = System.getenv(BASE_URL_OLD) ?: "https://eu5.fusionsolar.huawei.com/thirdData",
                baseUrlNew = System.getenv(BASE_URL_NEW) ?: "https://eu5.fusionsolar.huawei.com:27200/rest/openapi/pvms/v1",
                jobName = System.getenv(JOB_NAME) ?: "",
            )
}

data class FusionSolarConfig(
    val username: String,
    val password: String,
    val baseUrlOld: String,
    val baseUrlNew: String,
    val jobName: String,
)
