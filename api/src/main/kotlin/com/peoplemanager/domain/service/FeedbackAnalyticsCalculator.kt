package com.peoplemanager.domain.service

import com.peoplemanager.domain.FeedbackResponse
import java.time.LocalDate
import java.time.ZoneOffset

/** Bucketing granularity for the average-rating-over-time series. */
enum class AnalyticsBucket {
    MONTH, QUARTER
}

/** A single point in the rating-over-time series. */
data class RatingPeriod(
    val label: String,
    val averageRating: Double,
    val responseCount: Int
)

/** Aggregate analytics computed from a set of usable responses. */
data class FeedbackAnalytics(
    val totalResponses: Int,
    val overallAverageRating: Double?,
    val periods: List<RatingPeriod>,
    val topThemes: List<ThemeCount>
)

/**
 * Pure domain service that computes feedback analytics: an overall average rating,
 * an average-rating-per-period time series, and top free-text themes.
 *
 * Deterministic and framework-free. Works without any AI configured.
 */
object FeedbackAnalyticsCalculator {

    fun compute(
        responses: List<FeedbackResponse>,
        bucket: AnalyticsBucket,
        maxThemes: Int = 10
    ): FeedbackAnalytics {
        val allRatings = responses.flatMap { it.ratingValues() }
        val overall = allRatings.takeIf { it.isNotEmpty() }?.average()

        val periods = responses
            .filter { it.ratingValues().isNotEmpty() }
            .groupBy { bucketLabel(it.createdAtDate(), bucket) }
            .toSortedMap()
            .map { (label, group) ->
                val ratings = group.flatMap { it.ratingValues() }
                RatingPeriod(
                    label = label,
                    averageRating = ratings.average(),
                    responseCount = group.size
                )
            }

        val themes = ThemeExtractor.extract(
            responses.flatMap { it.freeTextParts() },
            maxThemes
        )

        return FeedbackAnalytics(
            totalResponses = responses.size,
            overallAverageRating = overall,
            periods = periods,
            topThemes = themes
        )
    }

    private fun FeedbackResponse.createdAtDate(): LocalDate =
        createdAt.atZone(ZoneOffset.UTC).toLocalDate()

    private fun bucketLabel(date: LocalDate, bucket: AnalyticsBucket): String = when (bucket) {
        AnalyticsBucket.MONTH -> "%04d-%02d".format(date.year, date.monthValue)
        AnalyticsBucket.QUARTER -> "%04d-Q%d".format(date.year, (date.monthValue - 1) / 3 + 1)
    }
}
