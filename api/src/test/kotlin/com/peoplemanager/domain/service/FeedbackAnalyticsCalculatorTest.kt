package com.peoplemanager.domain.service

import com.peoplemanager.domain.FeedbackAnswer
import com.peoplemanager.domain.FeedbackLinkId
import com.peoplemanager.domain.FeedbackResponse
import com.peoplemanager.domain.FeedbackResponseId
import com.peoplemanager.domain.PersonId
import com.peoplemanager.domain.UserId
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class FeedbackAnalyticsCalculatorTest {

    private val userId = UserId.generate()
    private val personId = PersonId.generate()
    private val linkId = FeedbackLinkId.generate()

    private fun responseOn(date: LocalDate, ratings: List<Int>, text: String? = null): FeedbackResponse {
        val answers = ratings.mapIndexed { i, r -> FeedbackAnswer("q$i", ratingValue = r) }
        return FeedbackResponse(
            id = FeedbackResponseId.generate(),
            userId = userId,
            personId = personId,
            linkId = linkId,
            answers = answers,
            additionalComments = text,
            createdAt = date.atStartOfDay(ZoneOffset.UTC).toInstant()
        )
    }

    @Test
    fun `empty responses yield null overall and no periods`() {
        val a = FeedbackAnalyticsCalculator.compute(emptyList(), AnalyticsBucket.QUARTER)
        a.totalResponses shouldBe 0
        a.overallAverageRating.shouldBeNull()
        a.periods.size shouldBe 0
    }

    @Test
    fun `computes overall average across all rating answers`() {
        val responses = listOf(
            responseOn(LocalDate.of(2026, 1, 10), listOf(4, 2)),
            responseOn(LocalDate.of(2026, 1, 20), listOf(5))
        )
        val a = FeedbackAnalyticsCalculator.compute(responses, AnalyticsBucket.MONTH)
        a.overallAverageRating!! shouldBe (3.6667 plusOrMinus 0.001)
    }

    @Test
    fun `buckets by quarter`() {
        val responses = listOf(
            responseOn(LocalDate.of(2026, 2, 1), listOf(4)),   // Q1
            responseOn(LocalDate.of(2026, 5, 1), listOf(2))    // Q2
        )
        val a = FeedbackAnalyticsCalculator.compute(responses, AnalyticsBucket.QUARTER)
        a.periods.map { it.label } shouldBe listOf("2026-Q1", "2026-Q2")
        a.periods[0].averageRating shouldBe 4.0
        a.periods[1].averageRating shouldBe 2.0
    }

    @Test
    fun `buckets by month`() {
        val responses = listOf(
            responseOn(LocalDate.of(2026, 3, 1), listOf(4)),
            responseOn(LocalDate.of(2026, 3, 15), listOf(2))
        )
        val a = FeedbackAnalyticsCalculator.compute(responses, AnalyticsBucket.MONTH)
        a.periods.size shouldBe 1
        a.periods[0].label shouldBe "2026-03"
        a.periods[0].averageRating shouldBe 3.0
        a.periods[0].responseCount shouldBe 2
    }

    @Test
    fun `includes top themes from free text`() {
        val responses = listOf(
            responseOn(LocalDate.of(2026, 1, 1), listOf(4), "excellent communication skills"),
            responseOn(LocalDate.of(2026, 1, 2), listOf(3), "communication could improve")
        )
        val a = FeedbackAnalyticsCalculator.compute(responses, AnalyticsBucket.MONTH)
        a.topThemes.any { it.theme == "communication" && it.count == 2 } shouldBe true
    }
}
