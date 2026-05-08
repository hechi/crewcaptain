package com.peoplemanager.domain

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.Instant

class OneOnOneSeriesTest {

    @Test
    fun `should create series with WEEKLY cadence`() {
        val series = OneOnOneSeries(
            id = OneOnOneSeriesId.generate(),
            userId = UserId.generate(),
            personId = PersonId.generate(),
            cadenceType = CadenceType.WEEKLY
        )
        series.cadenceType shouldBe CadenceType.WEEKLY
        series.customIntervalDays shouldBe null
    }

    @Test
    fun `should create series with BIWEEKLY cadence`() {
        val series = OneOnOneSeries(
            id = OneOnOneSeriesId.generate(),
            userId = UserId.generate(),
            personId = PersonId.generate(),
            cadenceType = CadenceType.BIWEEKLY
        )
        series.cadenceType shouldBe CadenceType.BIWEEKLY
    }

    @Test
    fun `should create series with MONTHLY cadence`() {
        val series = OneOnOneSeries(
            id = OneOnOneSeriesId.generate(),
            userId = UserId.generate(),
            personId = PersonId.generate(),
            cadenceType = CadenceType.MONTHLY
        )
        series.cadenceType shouldBe CadenceType.MONTHLY
    }

    @Test
    fun `should create series with CUSTOM cadence and positive interval`() {
        val series = OneOnOneSeries(
            id = OneOnOneSeriesId.generate(),
            userId = UserId.generate(),
            personId = PersonId.generate(),
            cadenceType = CadenceType.CUSTOM,
            customIntervalDays = 10
        )
        series.cadenceType shouldBe CadenceType.CUSTOM
        series.customIntervalDays shouldBe 10
    }

    @Test
    fun `should reject CUSTOM cadence with null interval`() {
        shouldThrow<IllegalArgumentException> {
            OneOnOneSeries(
                id = OneOnOneSeriesId.generate(),
                userId = UserId.generate(),
                personId = PersonId.generate(),
                cadenceType = CadenceType.CUSTOM,
                customIntervalDays = null
            )
        }.message shouldBe "Custom cadence requires a positive interval in days"
    }

    @Test
    fun `should reject CUSTOM cadence with zero interval`() {
        shouldThrow<IllegalArgumentException> {
            OneOnOneSeries(
                id = OneOnOneSeriesId.generate(),
                userId = UserId.generate(),
                personId = PersonId.generate(),
                cadenceType = CadenceType.CUSTOM,
                customIntervalDays = 0
            )
        }.message shouldBe "Custom cadence requires a positive interval in days"
    }

    @Test
    fun `should reject CUSTOM cadence with negative interval`() {
        shouldThrow<IllegalArgumentException> {
            OneOnOneSeries(
                id = OneOnOneSeriesId.generate(),
                userId = UserId.generate(),
                personId = PersonId.generate(),
                cadenceType = CadenceType.CUSTOM,
                customIntervalDays = -5
            )
        }.message shouldBe "Custom cadence requires a positive interval in days"
    }

    @Test
    fun `should allow non-CUSTOM cadence with null interval`() {
        val series = OneOnOneSeries(
            id = OneOnOneSeriesId.generate(),
            userId = UserId.generate(),
            personId = PersonId.generate(),
            cadenceType = CadenceType.WEEKLY,
            customIntervalDays = null
        )
        series.customIntervalDays shouldBe null
    }

    @Test
    fun `should store template markdown`() {
        val template = "## Agenda\n- Review action items\n\n## Notes\n"
        val series = OneOnOneSeries(
            id = OneOnOneSeriesId.generate(),
            userId = UserId.generate(),
            personId = PersonId.generate(),
            cadenceType = CadenceType.WEEKLY,
            templateMarkdown = template
        )
        series.templateMarkdown shouldBe template
    }
}
