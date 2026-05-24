package com.peoplemanager.adapters.metrics

import com.peoplemanager.adapters.persistence.SpringDataOneOnOneEntryRepository
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.binder.MeterBinder
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Exposes custom 1:1 metrics to Prometheus:
 * - crewcaptain_one_on_ones_total: Total number of 1:1 entries across all users
 * - crewcaptain_one_on_ones_last_7_days: Number of 1:1 entries created in the last 7 days
 */
@Component
class OneOnOneMetrics(
    private val oneOnOneEntryRepository: SpringDataOneOnOneEntryRepository
) : MeterBinder {

    override fun bindTo(registry: MeterRegistry) {
        Gauge.builder("crewcaptain.one_on_ones.total") { oneOnOneEntryRepository.count().toDouble() }
            .description("Total number of 1:1 entries across all users")
            .register(registry)

        Gauge.builder("crewcaptain.one_on_ones.last_7_days") {
            val sevenDaysAgo = Instant.now().minus(7, ChronoUnit.DAYS)
            oneOnOneEntryRepository.countByMeetingDateAfter(sevenDaysAgo).toDouble()
        }
            .description("Number of 1:1 entries with meeting date in the last 7 days")
            .register(registry)
    }
}
