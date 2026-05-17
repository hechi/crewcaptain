package com.peoplemanager.adapters.web

import com.peoplemanager.adapters.auth.AuthenticatedUser
import com.peoplemanager.application.AiCoachingResult
import com.peoplemanager.application.AiCoachingService
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/ai")
class AiCoachingController(
    private val aiCoachingService: AiCoachingService
) {

    @PostMapping("/refine-kudos")
    fun refineKudos(
        @Valid @RequestBody request: RefineKudosRequest
    ): ResponseEntity<AiCoachingResponse> {
        val userId = AuthenticatedUser.getUserId()

        return when (val result = aiCoachingService.refineKudos(userId, request.draft)) {
            is AiCoachingResult.Success -> ResponseEntity.ok(
                AiCoachingResponse(result = result.content, error = null)
            )
            is AiCoachingResult.Error -> ResponseEntity.ok(
                AiCoachingResponse(result = null, error = result.message)
            )
        }
    }

    @PostMapping("/optimize-pdp-goal")
    fun optimizePdpGoal(
        @Valid @RequestBody request: OptimizePdpGoalRequest
    ): ResponseEntity<AiCoachingResponse> {
        val userId = AuthenticatedUser.getUserId()

        return when (val result = aiCoachingService.optimizePdpGoal(userId, request.title, request.description)) {
            is AiCoachingResult.Success -> ResponseEntity.ok(
                AiCoachingResponse(result = result.content, error = null)
            )
            is AiCoachingResult.Error -> ResponseEntity.ok(
                AiCoachingResponse(result = null, error = result.message)
            )
        }
    }
}

data class RefineKudosRequest(
    @field:NotBlank(message = "Draft text is required")
    val draft: String
)

data class OptimizePdpGoalRequest(
    @field:NotBlank(message = "Goal title is required")
    val title: String,
    val description: String? = null
)

data class AiCoachingResponse(
    val result: String?,
    val error: String?
)
