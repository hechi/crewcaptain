package com.peoplemanager.adapters.web.dto

import com.peoplemanager.domain.FeedbackSummary
import com.peoplemanager.domain.service.FeedbackAnalytics
import jakarta.validation.constraints.NotBlank
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class RatingPeriodDto(
    val label: String,
    val averageRating: Double,
    val responseCount: Int
)

data class ThemeCountDto(
    val theme: String,
    val count: Int
)

data class FeedbackAnalyticsResponse(
    val totalResponses: Int,
    val overallAverageRating: Double?,
    val periods: List<RatingPeriodDto>,
    val topThemes: List<ThemeCountDto>
) {
    companion object {
        fun from(a: FeedbackAnalytics): FeedbackAnalyticsResponse = FeedbackAnalyticsResponse(
            totalResponses = a.totalResponses,
            overallAverageRating = a.overallAverageRating,
            periods = a.periods.map { RatingPeriodDto(it.label, it.averageRating, it.responseCount) },
            topThemes = a.topThemes.map { ThemeCountDto(it.theme, it.count) }
        )
    }
}

data class GenerateFeedbackSummaryRequest(
    val from: LocalDate? = null,
    val to: LocalDate? = null
)

data class GeneratedFeedbackSummaryResponse(
    val content: String,
    val responseCount: Int
)

data class SaveFeedbackSummaryRequest(
    val periodFrom: LocalDate?,
    val periodTo: LocalDate?,
    @field:NotBlank(message = "Summary content must not be blank")
    val content: String?,
    val responseCount: Int? = null
)

data class FeedbackSummaryResponse(
    val id: UUID,
    val personId: UUID,
    val periodFrom: LocalDate,
    val periodTo: LocalDate,
    val content: String,
    val responseCount: Int,
    val createdAt: Instant,
    val updatedAt: Instant
) {
    companion object {
        fun from(s: FeedbackSummary): FeedbackSummaryResponse = FeedbackSummaryResponse(
            id = s.id.value,
            personId = s.personId.value,
            periodFrom = s.periodFrom,
            periodTo = s.periodTo,
            content = s.content,
            responseCount = s.responseCount,
            createdAt = s.createdAt,
            updatedAt = s.updatedAt
        )
    }
}
