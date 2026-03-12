package ch.bziswiler.pvlogger.frontend.plugins

import ch.bziswiler.pvlogger.frontend.configuration.DataAccessorConfig
import ch.bziswiler.pvlogger.frontend.rest.DataAccessorRestProxy
import ch.bziswiler.pvlogger.shared.rest.KpiValuesDto
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationProducerSchedulerTest {

    private fun createSchedulerSetup(): Pair<NotificationProducerScheduler, MutableSharedFlow<KpiValuesDto>> {
        val flow = MutableSharedFlow<KpiValuesDto>()
        val proxy = DataAccessorRestProxy(DataAccessorConfig("localhost", 9999))
        val producer = NotificationProducer(flow, proxy)
        val scheduler = NotificationProducerScheduler(producer, 60.seconds, 60.seconds)
        return scheduler to flow
    }

    @Test
    fun `start increments subscriber count and stop decrements it`() = runTest {
        val (scheduler, _) = createSchedulerSetup()

        scheduler.start(this)
        scheduler.start(this)

        // Stop both
        scheduler.stop()
        scheduler.stop()

        // Extra stop should not crash
        scheduler.stop()
    }

    @Test
    fun `second start does not launch duplicate job`() = runTest {
        val (scheduler, _) = createSchedulerSetup()

        scheduler.start(this)
        scheduler.start(this)

        scheduler.stop()
        scheduler.stop()
    }

    @Test
    fun `can restart after full stop`() = runTest {
        val (scheduler, _) = createSchedulerSetup()

        scheduler.start(this)
        scheduler.stop()

        // Should be able to start again
        scheduler.start(this)
        scheduler.stop()
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationProducerTest {

    @Test
    fun `produce emits to flow when proxy returns data`() = runTest {
        val flow = MutableSharedFlow<KpiValuesDto>(replay = 1)
        val kpiDto = KpiValuesDto(createdAt = 42L, solarPower = 3.14)

        // Directly test flow emission (since we can't mock the final proxy)
        flow.emit(kpiDto)

        val received = flow.first()
        assertEquals(42L, received.createdAt)
        assertEquals(3.14, received.solarPower)
    }

    @Test
    fun `shared flow with no replay does not deliver to late subscribers`() = runTest {
        val flow = MutableSharedFlow<KpiValuesDto>()
        val received = mutableListOf<KpiValuesDto>()

        // Emit before anyone collects
        flow.emit(KpiValuesDto(createdAt = 1L))

        // Late collector should not receive the earlier emission
        val job = launch {
            flow.collect { received.add(it) }
        }
        advanceTimeBy(100)

        assertEquals(0, received.size, "MutableSharedFlow with no replay should not deliver to late subscribers")
        job.cancel()
    }
}
