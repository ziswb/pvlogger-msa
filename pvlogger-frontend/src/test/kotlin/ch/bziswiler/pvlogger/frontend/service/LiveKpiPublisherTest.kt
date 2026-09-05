package ch.bziswiler.pvlogger.frontend.service

import ch.bziswiler.pvlogger.shared.rest.KpiValuesDto
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.seconds

class LiveKpiPublisherTest {

    private val pollInterval = 60.seconds
    private val stopTimeout = 5.seconds
    private val retryDelay = 10.seconds

    private fun TestScope.publisherOver(source: LatestKpiSource) =
        LiveKpiPublisher(
            source = source,
            scope = backgroundScope,
            pollInterval = pollInterval,
            retryDelay = retryDelay,
            stopTimeout = stopTimeout,
            replayExpiration = pollInterval,
        )

    /** Counts calls and hands out a KPI row whose `createdAt` is the call number. */
    private class CountingSource(val failuresBeforeSuccess: Int = 0, val nullResults: Boolean = false) :
        LatestKpiSource {
        val calls = AtomicInteger(0)

        override suspend fun retrieveLatestKpis(): KpiValuesDto? {
            val call = calls.incrementAndGet()
            if (call <= failuresBeforeSuccess) throw IllegalStateException("data accessor unavailable")
            return if (nullResults) null else KpiValuesDto(createdAt = call.toLong())
        }
    }

    @Test
    fun `does not poll while nobody is subscribed`() = runTest {
        val source = CountingSource()
        publisherOver(source)

        testScheduler.advanceTimeBy(pollInterval * 10)

        assertEquals(0, source.calls.get(), "an idle dashboard must cause no data accessor traffic")
    }

    @Test
    fun `first subscriber gets a value immediately and then one per interval`() = runTest {
        val source = CountingSource()
        val publisher = publisherOver(source)
        val received = mutableListOf<KpiValuesDto>()

        val collector = backgroundScope.launch { publisher.latestKpis.collect { received.add(it) } }
        testScheduler.runCurrent()

        assertEquals(listOf(1L), received.map { it.createdAt }, "no initial delay before the first value")

        testScheduler.advanceTimeBy(pollInterval + 1.seconds)
        assertEquals(listOf(1L, 2L), received.map { it.createdAt })

        testScheduler.advanceTimeBy(pollInterval)
        assertEquals(listOf(1L, 2L, 3L), received.map { it.createdAt })

        collector.cancel()
    }

    @Test
    fun `a late subscriber gets the cached value at once and shares the same tick`() = runTest {
        val source = CountingSource()
        val publisher = publisherOver(source)

        val first = mutableListOf<KpiValuesDto>()
        val firstCollector = backgroundScope.launch { publisher.latestKpis.collect { first.add(it) } }
        testScheduler.advanceTimeBy(pollInterval / 2)

        val second = mutableListOf<KpiValuesDto>()
        val secondCollector = backgroundScope.launch { publisher.latestKpis.collect { second.add(it) } }
        testScheduler.runCurrent()

        assertEquals(listOf(1L), second.map { it.createdAt }, "replay = 1 must serve the reconnecting browser")
        assertEquals(1, source.calls.get(), "a second session must not trigger its own poll")

        testScheduler.advanceTimeBy(pollInterval)
        assertEquals(listOf(1L, 2L), first.map { it.createdAt })
        assertEquals(listOf(1L, 2L), second.map { it.createdAt })
        assertEquals(2, source.calls.get(), "both sessions share one poll")

        firstCollector.cancel()
        secondCollector.cancel()
    }

    @Test
    fun `the first subscriber leaving does not silence the remaining ones`() = runTest {
        // Regression: the polling job used to be launched in the first WebSocket
        // session's scope, so it died with that session while others stayed on.
        val source = CountingSource()
        val publisher = publisherOver(source)

        val leaving = backgroundScope.launch { publisher.latestKpis.collect { } }
        val staying = mutableListOf<KpiValuesDto>()
        val stayingCollector = backgroundScope.launch { publisher.latestKpis.collect { staying.add(it) } }
        testScheduler.runCurrent()

        leaving.cancel()
        testScheduler.advanceTimeBy(pollInterval * 3)

        assertEquals(listOf(1L, 2L, 3L), staying.map { it.createdAt })

        stayingCollector.cancel()
    }

    @Test
    fun `polling stops again once the last subscriber is gone`() = runTest {
        val source = CountingSource()
        val publisher = publisherOver(source)

        val collector = backgroundScope.launch { publisher.latestKpis.collect { } }
        testScheduler.advanceTimeBy(pollInterval + 1.seconds)
        val callsWhileSubscribed = source.calls.get()
        assertEquals(2, callsWhileSubscribed)

        collector.cancel()
        testScheduler.advanceTimeBy(stopTimeout + pollInterval * 10)

        assertEquals(callsWhileSubscribed, source.calls.get(), "polling must stop when the last session closes")
    }

    @Test
    fun `a failing data accessor does not end the publisher`() = runTest {
        val source = CountingSource(failuresBeforeSuccess = 2)
        val publisher = publisherOver(source)

        val received = mutableListOf<KpiValuesDto>()
        val collector = backgroundScope.launch { publisher.latestKpis.collect { received.add(it) } }

        testScheduler.advanceTimeBy(retryDelay * 2 + 1.seconds)

        assertEquals(listOf(3L), received.map { it.createdAt }, "the flow must recover instead of staying dead")

        testScheduler.advanceTimeBy(pollInterval)
        assertTrue(received.size >= 2, "polling resumes at the normal interval after recovery")

        collector.cancel()
    }

    @Test
    fun `an empty data accessor emits nothing`() = runTest {
        val source = CountingSource(nullResults = true)
        val publisher = publisherOver(source)

        val received = mutableListOf<KpiValuesDto>()
        val collector = backgroundScope.launch { publisher.latestKpis.toList(received) }

        testScheduler.advanceTimeBy(pollInterval * 3)

        assertTrue(received.isEmpty(), "a null latest-KPI response must not be pushed to the browser")
        assertTrue(source.calls.get() >= 3, "but it keeps polling")

        collector.cancel()
    }

    @Test
    fun `a slow session does not stall the others`() = runTest {
        val source = CountingSource()
        val publisher = publisherOver(source)

        // A session that never consumes: conflated collection keeps it from
        // applying backpressure to the shared upstream.
        val stuck = backgroundScope.launch {
            publisher.latestKpis.conflate().collect { awaitCancellation() }
        }
        val healthy = mutableListOf<KpiValuesDto>()
        val healthyCollector =
            backgroundScope.launch { publisher.latestKpis.conflate().collect { healthy.add(it) } }
        testScheduler.runCurrent()

        testScheduler.advanceTimeBy(pollInterval * 3)

        assertEquals(listOf(1L, 2L, 3L), healthy.map { it.createdAt })

        stuck.cancel()
        healthyCollector.cancel()
    }

    @Test
    fun `the cached value expires so a returning browser is not shown stale data`() = runTest {
        val source = CountingSource()
        val publisher = publisherOver(source)

        val collector = backgroundScope.launch { publisher.latestKpis.collect { } }
        testScheduler.runCurrent()
        collector.cancel()

        testScheduler.advanceTimeBy(stopTimeout + pollInterval + 1.seconds)

        val fresh = backgroundScope.launch {
            val value = publisher.latestKpis.first()
            assertTrue(value.createdAt > 1L, "the returning session must get a fresh poll, not the old cache")
        }
        testScheduler.runCurrent()
        fresh.cancel()
    }
}
