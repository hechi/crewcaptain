package com.peoplemanager.adapters.web

import com.peoplemanager.adapters.auth.AuthenticatedUser
import com.peoplemanager.adapters.web.dto.*
import com.peoplemanager.application.StrategyGoalLinkService
import com.peoplemanager.application.StrategyGoalService
import com.peoplemanager.application.StrategyGoalNotFoundException
import com.peoplemanager.application.commands.*
import com.peoplemanager.application.queries.*
import com.peoplemanager.domain.PdpGoalId
import com.peoplemanager.domain.PersonId
import com.peoplemanager.domain.StrategyGoalId
import com.peoplemanager.domain.StrategyGoalStatus
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1")
class StrategyGoalController(
    private val strategyGoalService: StrategyGoalService,
    private val strategyGoalLinkService: StrategyGoalLinkService
) {

    // ===== Strategy Goals =====

    @PostMapping("/strategy-goals")
    fun createStrategyGoal(
        @Valid @RequestBody request: CreateStrategyGoalRequest
    ): ResponseEntity<StrategyGoalResponse> {
        val userId = AuthenticatedUser.getUserId()
        val command = CreateStrategyGoalCommand(
            userId = userId,
            title = request.title!!,
            description = request.description,
            targetDate = request.targetDate,
            sensitive = request.sensitive ?: false
        )
        val goal = strategyGoalService.createStrategyGoal(command)
        return ResponseEntity.status(HttpStatus.CREATED).body(StrategyGoalResponse.from(goal))
    }

    @GetMapping("/strategy-goals")
    fun listStrategyGoals(
        @RequestParam(required = false) status: StrategyGoalStatus?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<PaginatedStrategyGoalResponse> {
        val userId = AuthenticatedUser.getUserId()
        val query = ListStrategyGoalsQuery(
            userId = userId,
            status = status,
            page = page,
            size = size
        )
        val result = strategyGoalService.listStrategyGoals(query)
        return ResponseEntity.ok(PaginatedStrategyGoalResponse.from(result))
    }

    @GetMapping("/strategy-goals/{strategyGoalId}")
    fun getStrategyGoal(
        @PathVariable strategyGoalId: UUID
    ): ResponseEntity<StrategyGoalResponse> {
        val userId = AuthenticatedUser.getUserId()
        val query = GetStrategyGoalQuery(
            userId = userId,
            strategyGoalId = StrategyGoalId(strategyGoalId)
        )
        val goal = strategyGoalService.getStrategyGoal(query)
        return ResponseEntity.ok(StrategyGoalResponse.from(goal))
    }

    @PutMapping("/strategy-goals/{strategyGoalId}")
    fun updateStrategyGoal(
        @PathVariable strategyGoalId: UUID,
        @Valid @RequestBody request: UpdateStrategyGoalRequest
    ): ResponseEntity<StrategyGoalResponse> {
        val userId = AuthenticatedUser.getUserId()
        val command = UpdateStrategyGoalCommand(
            userId = userId,
            strategyGoalId = StrategyGoalId(strategyGoalId),
            title = request.title,
            description = request.description,
            targetDate = request.targetDate
        )
        val goal = strategyGoalService.updateStrategyGoal(command)
        return ResponseEntity.ok(StrategyGoalResponse.from(goal))
    }

    @DeleteMapping("/strategy-goals/{strategyGoalId}")
    fun deleteStrategyGoal(
        @PathVariable strategyGoalId: UUID
    ): ResponseEntity<Void> {
        val userId = AuthenticatedUser.getUserId()
        val command = DeleteStrategyGoalCommand(
            userId = userId,
            strategyGoalId = StrategyGoalId(strategyGoalId)
        )
        strategyGoalService.deleteStrategyGoal(command)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/strategy-goals/{strategyGoalId}/achieve")
    fun achieveStrategyGoal(
        @PathVariable strategyGoalId: UUID
    ): ResponseEntity<StrategyGoalResponse> {
        val userId = AuthenticatedUser.getUserId()
        val command = AchieveStrategyGoalCommand(
            userId = userId,
            strategyGoalId = StrategyGoalId(strategyGoalId)
        )
        val goal = strategyGoalService.achieveStrategyGoal(command)
        return ResponseEntity.ok(StrategyGoalResponse.from(goal))
    }

    @PostMapping("/strategy-goals/{strategyGoalId}/drop")
    fun dropStrategyGoal(
        @PathVariable strategyGoalId: UUID
    ): ResponseEntity<StrategyGoalResponse> {
        val userId = AuthenticatedUser.getUserId()
        val command = DropStrategyGoalCommand(
            userId = userId,
            strategyGoalId = StrategyGoalId(strategyGoalId)
        )
        val goal = strategyGoalService.dropStrategyGoal(command)
        return ResponseEntity.ok(StrategyGoalResponse.from(goal))
    }

    // ===== Links to PDP Goals =====

    @PostMapping("/strategy-goals/{strategyGoalId}/links")
    fun linkPdpGoal(
        @PathVariable strategyGoalId: UUID,
        @Valid @RequestBody request: LinkPdpGoalRequest
    ): ResponseEntity<Void> {
        val userId = AuthenticatedUser.getUserId()
        val command = LinkPdpGoalToStrategyGoalCommand(
            userId = userId,
            strategyGoalId = StrategyGoalId(strategyGoalId),
            pdpGoalId = PdpGoalId(UUID.fromString(request.pdpGoalId!!)),
            personId = PersonId(UUID.fromString(request.personId!!))
        )
        strategyGoalLinkService.linkPdpGoal(command)
        return ResponseEntity.status(HttpStatus.CREATED).build()
    }

    @GetMapping("/strategy-goals/{strategyGoalId}/links")
    fun getLinkedPdpGoals(
        @PathVariable strategyGoalId: UUID
    ): ResponseEntity<List<LinkedPdpGoalInfo>> {
        val userId = AuthenticatedUser.getUserId()
        val links = strategyGoalLinkService.getLinkedPdpGoals(StrategyGoalId(strategyGoalId), userId)
        // TODO: Convert links to response DTOs
        return ResponseEntity.ok(emptyList())
    }

    @DeleteMapping("/strategy-goals/{strategyGoalId}/links/{pdpGoalId}")
    fun unlinkPdpGoal(
        @PathVariable strategyGoalId: UUID,
        @PathVariable pdpGoalId: UUID
    ): ResponseEntity<Void> {
        val userId = AuthenticatedUser.getUserId()
        val command = UnlinkPdpGoalFromStrategyGoalCommand(
            userId = userId,
            strategyGoalId = StrategyGoalId(strategyGoalId),
            pdpGoalId = PdpGoalId(pdpGoalId)
        )
        strategyGoalLinkService.unlinkPdpGoal(command)
        return ResponseEntity.noContent().build()
    }

    // ===== Alignment & Gap Analysis =====

    @GetMapping("/strategy-goals/{strategyGoalId}/alignment")
    fun getAlignmentScore(
        @PathVariable strategyGoalId: UUID
    ): ResponseEntity<AlignmentScoreResponse> {
        val userId = AuthenticatedUser.getUserId()
        val score = strategyGoalLinkService.getAlignmentScore(StrategyGoalId(strategyGoalId), userId)
        return ResponseEntity.ok(AlignmentScoreResponse.from(score))
    }

    @GetMapping("/strategy-goals/alignment")
    fun getAllAlignmentScores(): ResponseEntity<AllAlignmentScoresResponse> {
        val userId = AuthenticatedUser.getUserId()
        val scores = strategyGoalLinkService.getAllAlignmentScores(userId)
        return ResponseEntity.ok(AllAlignmentScoresResponse.from(scores))
    }

    @GetMapping("/strategy-goals/gap-analysis")
    fun getGapAnalysis(): ResponseEntity<GapAnalysisResponse> {
        val userId = AuthenticatedUser.getUserId()
        val gapAnalysis = strategyGoalLinkService.getGapAnalysis(userId)
        return ResponseEntity.ok(GapAnalysisResponse.from(gapAnalysis))
    }

    // ===== Exception Handlers =====

    @ExceptionHandler(StrategyGoalNotFoundException::class)
    fun handleStrategyGoalNotFound(e: StrategyGoalNotFoundException): ResponseEntity<Void> {
        return ResponseEntity.notFound().build()
    }

    // TODO: Remove this placeholder data class
    data class LinkedPdpGoalInfo(
        val pdpGoalId: UUID,
        val personId: UUID,
        val title: String
    )
}
