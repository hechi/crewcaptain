package com.peoplemanager.adapters.web.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.util.UUID

data class CreateQuickNoteRequest(
    @field:NotBlank(message = "Text must not be blank")
    val text: String?,
    val personId: UUID? = null,
    val sensitive: Boolean? = null,
    val selfAssigned: Boolean? = null,
    @field:Size(max = 30, message = "Tag must be 30 characters or fewer")
    val tag: String? = null
)

data class UpdateQuickNoteRequest(
    val text: String? = null,
    val personId: UUID? = null,
    val sensitive: Boolean? = null,
    @field:Size(max = 30, message = "Tag must be 30 characters or fewer")
    val tag: String? = null,
    val updateTag: Boolean? = null
)

data class AssignQuickNoteToPersonRequest(
    val personId: UUID
)

data class AttachQuickNoteToEntryRequest(
    val entryId: UUID
)

data class ConvertQuickNoteRequest(
    val personId: UUID? = null,
    val dueDate: java.time.LocalDate? = null
)

data class MoveQuickNoteRequest(
    @field:jakarta.validation.constraints.Pattern(
        regexp = "UP|DOWN",
        message = "Direction must be UP or DOWN"
    )
    val direction: String
)

data class ReorderQuickNoteRequest(
    @field:jakarta.validation.constraints.Min(value = 0, message = "targetIndex must be zero or greater")
    val targetIndex: Int
)
