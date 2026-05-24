package com.peoplemanager.adapters.web

import com.peoplemanager.adapters.auth.AuthenticatedUser
import com.peoplemanager.application.AiExtractionResult
import com.peoplemanager.application.AiOutcomeExtractorService
import com.peoplemanager.application.ApplyActionItem
import com.peoplemanager.application.ApplyOutcomesCommand
import com.peoplemanager.application.ExtractedActionItem
import com.peoplemanager.domain.OneOnOneEntryId
import com.peoplemanager.domain.PersonId
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/persons/{personId}/one-on-one-entries/{entryId}")
class AiOutcomeExtractorController(
    private val aiOutcomeExtractorService: AiOutcomeExtractorService
) {

    @PostMapping("/extract-outcomes")
    fun extractOutcomes(
        @PathVariable personId: UUID,
        @PathVariable entryId: UUID
    ): ResponseEntity<ExtractOutcomesResponse> {
        val userId = AuthenticatedUser.getUserId()

        return when (val result = aiOutcomeExtractorService.extractOutcomes(
            userId,
            PersonId(personId),
            OneOnOneEntryId(entryId)
        )) {
            is AiExtractionResult.Success -> ResponseEntity.ok(
                ExtractOutcomesResponse(
                    actionItems = result.actionItems.map { item ->
                        ExtractedActionItemResponse(
                            title = item.title,
                            ownerType = item.ownerType,
                            suggestedDaysToDue = item.suggestedDaysToDue
                        )
                    },
                    decisions = result.decisions,
                    error = null
                )
            )
            is AiExtractionResult.Error -> ResponseEntity.ok(
                ExtractOutcomesResponse(
                    actionItems = emptyList(),
                    decisions = emptyList(),
                    error = result.message
                )
            )
        }
    }

    @PostMapping("/apply-outcomes")
    fun applyOutcomes(
        @PathVariable personId: UUID,
        @PathVariable entryId: UUID,
        @Valid @RequestBody request: ApplyOutcomesRequest
    ): ResponseEntity<ApplyOutcomesResponse> {
        val userId = AuthenticatedUser.getUserId()

        val command = ApplyOutcomesCommand(
            actionItems = request.actionItems.map { item ->
                ApplyActionItem(
                    title = item.title,
                    ownerType = item.ownerType,
                    suggestedDaysToDue = item.suggestedDaysToDue
                )
            },
            decisions = request.decisions
        )

        val result = aiOutcomeExtractorService.applyOutcomes(
            userId,
            PersonId(personId),
            OneOnOneEntryId(entryId),
            command
        )

        return ResponseEntity.ok(
            ApplyOutcomesResponse(
                actionItemsCreated = result.actionItemsCreated,
                decisionsAppended = result.decisionsAppended
            )
        )
    }
}

// --- Request/Response DTOs ---

data class ExtractOutcomesResponse(
    val actionItems: List<ExtractedActionItemResponse>,
    val decisions: List<String>,
    val error: String?
)

data class ExtractedActionItemResponse(
    val title: String,
    val ownerType: String,
    val suggestedDaysToDue: Int?
)

data class ApplyOutcomesRequest(
    val actionItems: List<ApplyActionItemRequest>,
    val decisions: List<String> = emptyList()
)

data class ApplyActionItemRequest(
    val title: String,
    val ownerType: String = "MANAGER",
    val suggestedDaysToDue: Int? = null
)

data class ApplyOutcomesResponse(
    val actionItemsCreated: Int,
    val decisionsAppended: Int
)
