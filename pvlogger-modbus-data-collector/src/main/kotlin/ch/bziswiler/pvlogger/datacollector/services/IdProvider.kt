package ch.bziswiler.pvlogger.datacollector.services

import java.util.*
import java.util.concurrent.atomic.AtomicInteger

class IdProvider {
    private val requestId = AtomicInteger(1)

    val nextRequestId get() = requestId.getAndIncrement().toUShort()
    val nextJobId get() = UUID.randomUUID().toString()
}
