package com.peoplemanager.domain

import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * Property-based tests for OneOnOneSeries domain aggregate.
 *
 * **Validates: Requirements 1.5**
 */
@Tag("property")
class OneOnOneSeriesPropertyTest {

    /**
     * Property 9: Custom cadence requires positive interval
     *
     * For any series upsert with cadenceType=CUSTOM, the customIntervalDays field must be
     * a positive integer. Zero or negative values SHALL be rejected with an IllegalArgumentException.
     *
     * **Validates: Requirements 1.5**
     */
    @Test
    fun `Property 9 - non-positive integers should be rejected for CUSTOM cadence`() = runBlocking {
        val nonPositiveIntArb: Arb<Int> = Arb.int(Int.MIN_VALUE..0)

        checkAll(100, nonPositiveIntArb) { nonPositiveInterval ->
            assertThrows<IllegalArgumentException> {
                OneOnOneSeries(
                    id = OneOnOneSeriesId.generate(),
                    userId = UserId.generate(),
                    personId = PersonId.generate(),
                    cadenceType = CadenceType.CUSTOM,
                    customIntervalDays = nonPositiveInterval
                )
            }.message shouldBe "Custom cadence requires a positive interval in days"
        }
        Unit
    }

    /**
     * Property 9 (positive case): Custom cadence with positive interval succeeds
     *
     * For any positive integer, constructing OneOnOneSeries with cadenceType=CUSTOM
     * and that interval should succeed without throwing.
     *
     * **Validates: Requirements 1.5**
     */
    @Test
    fun `Property 9 - positive integers should be accepted for CUSTOM cadence`() = runBlocking {
        val positiveIntArb: Arb<Int> = Arb.int(1..Int.MAX_VALUE)

        checkAll(100, positiveIntArb) { positiveInterval ->
            val series = OneOnOneSeries(
                id = OneOnOneSeriesId.generate(),
                userId = UserId.generate(),
                personId = PersonId.generate(),
                cadenceType = CadenceType.CUSTOM,
                customIntervalDays = positiveInterval
            )
            series.cadenceType shouldBe CadenceType.CUSTOM
            series.customIntervalDays shouldBe positiveInterval
        }
        Unit
    }
}
