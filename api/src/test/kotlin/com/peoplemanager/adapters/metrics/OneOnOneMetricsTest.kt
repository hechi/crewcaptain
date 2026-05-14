package com.peoplemanager.adapters.metrics

import com.peoplemanager.adapters.persistence.SpringDataOneOnOneEntryRepository
import io.kotest.matchers.doubles.shouldBeExactly
import io.kotest.matchers.shouldNotBe
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant

class OneOnOneMetricsTest {

    private lateinit var repository: SpringDataOneOnOneEntryRepository
    private lateinit var registry: SimpleMeterRegistry
    private lateinit var metrics: OneOnOneMetrics

    @BeforeEach
    fun setUp() {
        repository = mockk()
        registry = SimpleMeterRegistry()
        metrics = OneOnOneMetrics(repository)
    }

    @Test
    fun `should register total 1-1 entries gauge`() {
        every { repository.count() } returns 42L
        every { repository.countByMeetingDateAfter(any()) } returns 5L

        metrics.bindTo(registry)

        val gauge = registry.find("crewcaptain.one_on_ones.total").gauge()
        gauge shouldNotBe null
        gauge!!.value() shouldBeExactly 42.0
    }

    @Test
    fun `should register last 7 days gauge`() {
        every { repository.count() } returns 100L
        every { repository.countByMeetingDateAfter(any()) } returns 7L

        metrics.bindTo(registry)

        val gauge = registry.find("crewcaptain.one_on_ones.last_7_days").gauge()
        gauge shouldNotBe null
        gauge!!.value() shouldBeExactly 7.0
    }

    @Test
    fun `should return zero when no entries exist`() {
        every { repository.count() } returns 0L
        every { repository.countByMeetingDateAfter(any()) } returns 0L

        metrics.bindTo(registry)

        val totalGauge = registry.find("crewcaptain.one_on_ones.total").gauge()
        totalGauge!!.value() shouldBeExactly 0.0

        val recentGauge = registry.find("crewcaptain.one_on_ones.last_7_days").gauge()
        recentGauge!!.value() shouldBeExactly 0.0
    }

    @Test
    fun `should query repository on each gauge read`() {
        every { repository.count() } returns 10L andThen 20L
        every { repository.countByMeetingDateAfter(any()) } returns 3L

        metrics.bindTo(registry)

        val gauge = registry.find("crewcaptain.one_on_ones.total").gauge()
        gauge!!.value() // first read
        gauge.value() // second read

        verify(atLeast = 2) { repository.count() }
    }

    @Test
    fun `should pass a date approximately 7 days ago to repository`() {
        every { repository.count() } returns 0L
        every { repository.countByMeetingDateAfter(any()) } returns 0L

        metrics.bindTo(registry)

        val gauge = registry.find("crewcaptain.one_on_ones.last_7_days").gauge()
        gauge!!.value()

        verify {
            repository.countByMeetingDateAfter(withArg { date ->
                val now = Instant.now()
                val diff = now.epochSecond - date.epochSecond
                // Should be approximately 7 days (604800 seconds), allow 10 seconds tolerance
                assert(diff in 604790..604810) { "Expected ~7 days ago, got ${diff}s difference" }
            })
        }
    }
}
