package ch.bziswiler.pvlogger.heartbeat.jobs.management

import ch.bziswiler.pvlogger.heartbeat.configuration.AppConfig
import ch.bziswiler.pvlogger.heartbeat.jobs.general.KpiExportJobTrigger
import ch.bziswiler.pvlogger.heartbeat.jobs.modbus.ModbusDeviceIdentifiersCollectionJob
import ch.bziswiler.pvlogger.heartbeat.jobs.modbus.ModbusDeviceListCollectionJob
import ch.bziswiler.pvlogger.heartbeat.jobs.modbus.ModbusInverterBatchedDynamicDataCollectionJob
import ch.bziswiler.pvlogger.heartbeat.jobs.modbus.ModbusInverterBatchedStaticDataCollectionJob
import mu.KotlinLogging
import org.quartz.CronScheduleBuilder
import org.quartz.Job
import org.quartz.JobBuilder.newJob
import org.quartz.SimpleScheduleBuilder.simpleSchedule
import org.quartz.Trigger
import org.quartz.TriggerBuilder.newTrigger
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class JobScheduler(private val jobSchedulerManager: JobSchedulerManager) {

    private val logger = KotlinLogging.logger {}

    fun initializeJobs(appConfig: AppConfig) {
        setUpDailyOnceJobs(appConfig)
        setUpIndefiniteJobs()
    }

    private fun setUpDailyOnceJobs(appConfig: AppConfig) {
        ZonedDateTime.of(
            LocalDate.now(),
            LocalTime.of(getStartHour(appConfig), getStartMinute(appConfig)),
            ZoneId.systemDefault()
        ).let { startTime ->
            scheduleDailyOnceJob(ModbusDeviceListCollectionJob.config(startTime))
            scheduleDailyOnceJob(ModbusDeviceIdentifiersCollectionJob.config(startTime))
            scheduleDailyOnceJob(ModbusInverterBatchedStaticDataCollectionJob.config(startTime))
        }
    }

    private fun scheduleDailyOnceJob(config: JobConfiguration) {
        logger.debug { "scheduling once-daily-job '$config'" }
        newTrigger()
            .withIdentity(config.triggerKey)
            .withSchedule(CronScheduleBuilder.dailyAtHourAndMinute(config.startHour, config.startMinute))
            .forJob(config.jobKey)
            .build()
            .let { trigger -> scheduleJob(config.jobClass, trigger) }
    }

    private fun setUpIndefiniteJobs() {
        scheduleForeverRepeatingJob(ModbusInverterBatchedDynamicDataCollectionJob.config)
        scheduleForeverRepeatingJob(KpiExportJobTrigger.config)
    }

    private fun scheduleForeverRepeatingJob(config: JobConfiguration) {
        logger.debug { "scheduling forever-repeating-job '$config'" }
        newTrigger()
            .withIdentity(config.triggerKey)
            .withSchedule(
                simpleSchedule()
                    .withIntervalInSeconds(config.intervalInSeconds)
                    .withMisfireHandlingInstructionIgnoreMisfires()
                    .repeatForever()
            )
            .forJob(config.jobKey).build()
            .let { trigger -> scheduleJob(config.jobClass, trigger) }
    }

    private fun scheduleJob(jobClass: Class<out Job>, trigger: Trigger) =
        with(jobSchedulerManager.scheduler) {
            this.deleteJob(trigger.jobKey)
            newJob(jobClass)
                .withIdentity(trigger.jobKey)
                .build()
                .let { job -> this.scheduleJob(job, trigger) }
        }

    private fun getStartMinute(appConfig: AppConfig): Int = when (appConfig.serverConfig.isProd) {
        true -> appConfig.jobConfig.lateEvening.split(":")[1].toInt()
        else -> ZonedDateTime.now().plusMinutes(1).hour
    }

    private fun getStartHour(appConfig: AppConfig): Int = when (appConfig.serverConfig.isProd) {
        true -> appConfig.jobConfig.lateEvening.split(":")[0].toInt()
        else -> ZonedDateTime.now().plusMinutes(1).hour
    }
}
