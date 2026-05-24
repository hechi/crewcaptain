package com.peoplemanager.adapters.web

import com.peoplemanager.adapters.auth.AuthenticatedUser
import com.peoplemanager.application.AiPrepResult
import com.peoplemanager.application.AiPrepService
import com.peoplemanager.application.PersonNotFoundException
import com.peoplemanager.domain.PersonId
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1")
class AiPrepController(
    private val aiPrepService: AiPrepService
) {

    @PostMapping("/persons/{personId}/ai-prep")
    fun generateAgendaSuggestions(
        @PathVariable personId: UUID
    ): ResponseEntity<AiPrepResponse> {
        val userId = AuthenticatedUser.getUserId()

        return try {
            when (val result = aiPrepService.generateAgendaSuggestions(userId, PersonId(personId))) {
                is AiPrepResult.Success -> ResponseEntity.ok(
                    AiPrepResponse(suggestions = result.suggestions, error = null)
                )
                is AiPrepResult.Error -> ResponseEntity.ok(
                    AiPrepResponse(suggestions = emptyList(), error = result.message)
                )
            }
        } catch (e: PersonNotFoundException) {
            ResponseEntity.notFound().build()
        }
    }
}

data class AiPrepResponse(
    val suggestions: List<String>,
    val error: String?
)
