package com.peoplemanager.adapters.web

import com.peoplemanager.adapters.auth.AuthenticatedUser
import com.peoplemanager.application.AiTrendRadarResult
import com.peoplemanager.application.AiTrendRadarService
import com.peoplemanager.application.TrendDimension
import com.peoplemanager.application.TrendRadarInsight
import com.peoplemanager.domain.PersonId
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/persons/{personId}")
class AiTrendRadarController(
    private val aiTrendRadarService: AiTrendRadarService
) {

    @PostMapping("/ai-trend-radar")
    fun generateInsights(
        @PathVariable personId: String
    ): ResponseEntity<AiTrendRadarResponse> {
        val userId = AuthenticatedUser.getUserId()

        return when (val result = aiTrendRadarService.generateInsights(userId, PersonId(UUID.fromString(personId)))) {
            is AiTrendRadarResult.Success -> ResponseEntity.ok(
                AiTrendRadarResponse(
                    insights = result.insights.map { it.toDto() },
                    insufficientData = false,
                    meetingsNeeded = null,
                    error = null
                )
            )
            is AiTrendRadarResult.InsufficientData -> ResponseEntity.ok(
                AiTrendRadarResponse(
                    insights = emptyList(),
                    insufficientData = true,
                    meetingsNeeded = result.meetingsNeeded,
                    error = result.message
                )
            )
            is AiTrendRadarResult.Error -> ResponseEntity.ok(
                AiTrendRadarResponse(
                    insights = emptyList(),
                    insufficientData = false,
                    meetingsNeeded = null,
                    error = result.message
                )
            )
        }
    }
}

data class AiTrendRadarResponse(
    val insights: List<TrendRadarInsightDto>,
    val insufficientData: Boolean,
    val meetingsNeeded: Int?,
    val error: String?
)

data class TrendRadarInsightDto(
    val title: String,
    val description: String,
    val dimension: String,
    val confidenceScore: Int
)

private fun TrendRadarInsight.toDto() = TrendRadarInsightDto(
    title = title,
    description = description,
    dimension = dimension.name,
    confidenceScore = confidenceScore
)
