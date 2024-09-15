package ch.bziswiler.pvlogger.shared.job.status

import java.io.Serializable

enum class JobStatus(val id: Int) : Serializable {
    SUCCESS(1),
    FAILURE(2)
}