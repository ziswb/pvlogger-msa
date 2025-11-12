package ch.bziswiler.pvlogger.fusionsolar

import ch.bziswiler.pvlogger.fusionsolar.configuration.AppConfig
import ch.bziswiler.pvlogger.fusionsolar.jobs.DeviceFiveMinutesKpiCollectionJob
import ch.bziswiler.pvlogger.fusionsolar.jobs.DeviceKpiCollectionJob
import ch.bziswiler.pvlogger.fusionsolar.jobs.ExtendedKpiCollectionJob
import ch.bziswiler.pvlogger.fusionsolar.jobs.StationKpiCollectionJob
import ch.bziswiler.pvlogger.fusionsolar.webservice.impl.FusionSolarDataAccessImpl
import ch.bziswiler.pvlogger.shared.databbase.DatabaseFactory
import ch.bziswiler.pvlogger.shared.job.PvJob
import ch.bziswiler.pvlogger.shared.job.meta.JobName
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

fun main(args: Array<String>) {
    startServices()
}

private fun startServices() =
    logger
        .info { "Starting job execution" }
        .also {
            val databaseFactory = DatabaseFactory(AppConfig.databaseConfig)
            databaseFactory.connect()
            val job: PvJob
            val fusionSolarDataAccess = FusionSolarDataAccessImpl(AppConfig.fusionSolarConfig)
            when (AppConfig.fusionSolarConfig.jobName) {
                JobName.DEVICE_5MIN_KPI_JOB.value -> {
                    job = DeviceFiveMinutesKpiCollectionJob(fusionSolarDataAccess)
                }

                JobName.DEVICE_KPI_JOB.value -> {
                    job = DeviceKpiCollectionJob(fusionSolarDataAccess)
                }

                JobName.EXTENDED_KPI_JOB.value -> {
                    job = ExtendedKpiCollectionJob(fusionSolarDataAccess)
                }

                JobName.STATION_KPI_JOB.value -> {
                    job = StationKpiCollectionJob(fusionSolarDataAccess)
                }

                else -> throw IllegalArgumentException("Unsupported job name: '${AppConfig.fusionSolarConfig.jobName}'")
            }
            try {
                job.execute()
            } finally {
                databaseFactory.close()
            }
        }.also {
            logger.info { "Finished executing job" }
        }
