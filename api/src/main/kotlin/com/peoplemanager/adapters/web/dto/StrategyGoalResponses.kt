package com.peoplemanager.adapters.web.dto

import com.peoplemanager.application.AiLinkDiscoveryService
import com.peoplemanager.application.StrategyGoalLinkService
import com.peoplemanager.domain.StrategyGoal
import com.peoplemanager.domain.StrategyGoalStatus
import org.springframework.data.domain.Page
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class StrategyGoalResponse(
    val id: UUID,
    val title: String,
    val description: String?,
    val targetDate: LocalDate?,
    val status: StrategyGoalStatus,
    val sensitive: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
    val linkedPdpGoalCount: Int? = null
) {
    companion object {
        fun from(goal: StrategyGoal): StrategyGoalResponse = StrategyGoalResponse(
            id = goal.id.value,
            title = goal.title,
            description = goal.description,
            targetDate = goal.targetDate,
            status = goal.status,
            sensitive = goal.sensitive,
            createdAt = goal.createdAt,
            updatedAt = goal.updatedAt
        )

        fun from(goal: StrategyGoal, linkedCount: Int): StrategyGoalResponse = StrategyGoalResponse(
            id = goal.id.value,
            title = goal.title,
            description = goal.description,
            targetDate = goal.targetDate,
            status = goal.status,
            sensitive = goal.sensitive,
            createdAt = goal.createdAt,
            updatedAt = goal.updatedAt,
            linkedPdpGoalCount = linkedCount
        )
    }
}

data class PaginatedStrategyGoalResponse(
    val content: List<StrategyGoalResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int
) {
    companion object {
        fun from(pageResult: Page<StrategyGoal>): PaginatedStrategyGoalResponse =
            PaginatedStrategyGoalResponse(
                content = pageResult.content.map { StrategyGoalResponse.from(it) },
                page = pageResult.number,
                size = pageResult.size,
                totalElements = pageResult.totalElements,
                totalPages = pageResult.totalPages
            )
    }
}

data class AlignmentScoreResponse(
    val strategyGoalId: UUID,
    val strategyGoalTitle: String,
    val totalActivePdpGoals: Int,
    val linkedPdpGoals: Int,
    val alignmentPercentage: Int
) {
    companion object {
        fun from(score: StrategyGoalLinkService.AlignmentScore): AlignmentScoreResponse =
            AlignmentScoreResponse(
                strategyGoalId = score.strategyGoalId.value,
                strategyGoalTitle = score.strategyGoalTitle,
                totalActivePdpGoals = score.totalActivePdpGoals,
                linkedPdpGoals = score.linkedPdpGoals,
                alignmentPercentage = score.alignmentPercentage
            )
    }
}

data class AllAlignmentScoresResponse(
    val scores: List<AlignmentScoreResponse>
) {
    companion object {
        fun from(scores: List<StrategyGoalLinkService.AlignmentScore>): AllAlignmentScoresResponse =
            AllAlignmentScoresResponse(
                scores = scores.map { AlignmentScoreResponse.from(it) }
            )
    }
}

data class UnlinkedPdpGoalResponse(
    val pdpGoalId: UUID,
    val personId: UUID,
    val title: String
) {
    companion object {
        fun from(info: StrategyGoalLinkService.UnlinkedPdpGoalInfo): UnlinkedPdpGoalResponse =
            UnlinkedPdpGoalResponse(
                pdpGoalId = info.pdpGoalId.value,
                personId = info.personId.value,
                title = info.title
            )
    }
}

data class EmptyStrategyGoalResponse(
    val strategyGoalId: UUID,
    val title: String
) {
    companion object {
        fun from(info: StrategyGoalLinkService.EmptyStrategyGoalInfo): EmptyStrategyGoalResponse =
            EmptyStrategyGoalResponse(
                strategyGoalId = info.strategyGoalId.value,
                title = info.title
            )
    }
}

data class GapAnalysisResponse(
    val unlinkedPdpGoals: List<UnlinkedPdpGoalResponse>,
    val emptyStrategyGoals: List<EmptyStrategyGoalResponse>
) {
    companion object {
        fun from(gapAnalysis: StrategyGoalLinkService.GapAnalysis): GapAnalysisResponse =
            GapAnalysisResponse(
                unlinkedPdpGoals = gapAnalysis.unlinkedPdpGoals.map { UnlinkedPdpGoalResponse.from(it) },
                emptyStrategyGoals = gapAnalysis.emptyStrategyGoals.map { EmptyStrategyGoalResponse.from(it) }
            )
    }
}

data class StrategyGoalBasicInfoResponse(
    val strategyGoalId: UUID,
    val title: String,
    val status: StrategyGoalStatus
) {
    companion object {
        fun from(info: StrategyGoalLinkService.StrategyGoalBasicInfo): StrategyGoalBasicInfoResponse =
            StrategyGoalBasicInfoResponse(
                strategyGoalId = info.strategyGoalId.value,
                title = info.title,
                status = info.status
            )
    }
}

data class LinkSuggestionResponse(
    val strategyGoalId: UUID,
    val strategyGoalTitle: String,
    val pdpGoalId: UUID,
    val personId: UUID,
    val pdpGoalTitle: String,
    val personName: String,
    val matchScore: Int,
    val reasoning: String
) {
    companion object {
        fun from(suggestion: AiLinkDiscoveryService.LinkSuggestion): LinkSuggestionResponse =
            LinkSuggestionResponse(
                strategyGoalId = suggestion.strategyGoalId.value,
                strategyGoalTitle = suggestion.strategyGoalTitle,
                pdpGoalId = suggestion.pdpGoalId.value,
                personId = suggestion.personId.value,
                pdpGoalTitle = suggestion.pdpGoalTitle,
                personName = suggestion.personName,
                matchScore = suggestion.matchScore,
                reasoning = suggestion.reasoning
            )
    }
}
