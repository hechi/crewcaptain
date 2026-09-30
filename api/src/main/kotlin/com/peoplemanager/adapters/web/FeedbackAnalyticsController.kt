package com.peoplemanager.adapters.web

import com.peoplemanager.adapters.auth.AuthenticatedUser
import com.peoplemanager.adapters.web.dto.*
import com.peoplemanager.application.FeedbackAnalyticsService
import com.peoplemanager.application.FeedbackSummaryAiService
import com.peoplemanager.application.FeedbackSummaryResult
import com.peoplemanager.application.commands.GenerateFeedbackSummaryCommand
import com.peoplemanager.application.commands.SaveFeedbackSummaryCommand
import com.peoplemanager.application.queries.GetFeedbackAnalyticsQuery
import com.peoplemanager.application.queries.ListFeedbackSummariesQuery
import com.peoplemanager.domain.PersonId
import com.peoplemanager.domain.service.AnalyticsBucket
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.util.UUID

@RestController
@RequestMapping("/api/v1/persons/{personId}")
class FeedbackAnalyticsController(
    private val analyticsService: FeedbackAnalyticsService,
    private val summaryAiService: FeedbackSummaryAiService
) {

    @GetMapping("/feedback-analytics")
    fun analytics(
        @PathVariable personId: UUID,
        @RequestParam(required = false) from: LocalDate?,
        @RequestParam(required = false) to: LocalDate?,
        @RequestParam(required = false, defaultValue = "QUARTER") bucket: String
    ): ResponseEntity<FeedbackAnalyticsResponse> {
        val userId = AuthenticatedUser.getUserId()
        val bucketEnum = when (bucket.uppercase()) {
            "MONTH" -> AnalyticsBucket.MONTH
            else -> AnalyticsBucket.QUARTER
        }
        val analytics = analyticsService.getAnalytics(
            GetFeedbackAnalyticsQuery(userId, PersonId(personId), from, to, bucketEnum)
        )
        return ResponseEntity.ok(FeedbackAnalyticsResponse.from(analytics))
    }

    @PostMapping("/feedback-summary/generate")
    fun generateSummary(
        @PathVariable personId: UUID,
        @RequestBody request: GenerateFeedbackSummaryRequest
    ): ResponseEntity<Any> {
        val userId = AuthenticatedUser.getUserId()
        val result = summaryAiService.generate(
            GenerateFeedbackSummaryCommand(userId, PersonId(personId), request.from, request.to)
        )
        return when (result) {
            is FeedbackSummaryResult.Success ->
                ResponseEntity.ok(GeneratedFeedbackSummaryResponse(result.content, result.responseCount))
            is FeedbackSummaryResult.Error ->
                ResponseEntity.unprocessableEntity().body(mapOf("message" to result.message))
        }
    }

    @GetMapping("/feedback-summaries")
    fun listSummaries(@PathVariable personId: UUID): ResponseEntity<List<FeedbackSummaryResponse>> {
        val userId = AuthenticatedUser.getUserId()
        val summaries = analyticsService.listSummaries(ListFeedbackSummariesQuery(userId, PersonId(personId)))
        return ResponseEntity.ok(summaries.map { FeedbackSummaryResponse.from(it) })
    }

    @PostMapping("/feedback-summaries")
    fun saveSummary(
        @PathVariable personId: UUID,
        @Valid @RequestBody request: SaveFeedbackSummaryRequest
    ): ResponseEntity<FeedbackSummaryResponse> {
        val userId = AuthenticatedUser.getUserId()
        val today = LocalDate.now()
        val saved = analyticsService.saveSummary(
            SaveFeedbackSummaryCommand(
                userId = userId,
                personId = PersonId(personId),
                periodFrom = request.periodFrom ?: today.minusMonths(6),
                periodTo = request.periodTo ?: today,
                content = request.content!!,
                responseCount = request.responseCount ?: 0
            )
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(FeedbackSummaryResponse.from(saved))
    }
}
