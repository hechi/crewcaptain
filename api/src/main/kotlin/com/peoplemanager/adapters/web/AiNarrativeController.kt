package com.peoplemanager.adapters.web

import com.peoplemanager.adapters.auth.AuthenticatedUser
import com.peoplemanager.application.AiNarrativeResult
import com.peoplemanager.application.AiNarrativeService
import com.peoplemanager.application.PersonNotFoundException
import com.peoplemanager.domain.PersonId
import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.util.UUID

@RestController
@RequestMapping("/api/v1")
class AiNarrativeController(
    private val aiNarrativeService: AiNarrativeService
) {

    @PostMapping("/persons/{personId}/ai-narrative")
    fun generateNarrative(
        @PathVariable personId: UUID,
        @Valid @RequestBody request: GenerateNarrativeRequest
    ): ResponseEntity<AiNarrativeResponse> {
        val userId = AuthenticatedUser.getUserId()

        return try {
            when (val result = aiNarrativeService.generateNarrative(
                userId,
                PersonId(personId),
                request.dateFrom,
                request.dateTo
            )) {
                is AiNarrativeResult.Success -> ResponseEntity.ok(
                    AiNarrativeResponse(narrative = result.narrative, error = null)
                )
                is AiNarrativeResult.Error -> ResponseEntity.ok(
                    AiNarrativeResponse(narrative = null, error = result.message)
                )
            }
        } catch (e: PersonNotFoundException) {
            ResponseEntity.notFound().build()
        }
    }
}

data class GenerateNarrativeRequest(
    @field:NotNull
    val dateFrom: LocalDate,
    @field:NotNull
    val dateTo: LocalDate
)

data class AiNarrativeResponse(
    val narrative: String?,
    val error: String?
)
