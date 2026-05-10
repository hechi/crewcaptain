package com.peoplemanager.adapters.web.dto

import jakarta.validation.constraints.NotBlank
import java.util.UUID

data class CreateQuickNoteRequest(
    @field:NotBlank(message = "Text must not be blank")
    val text: String?,
    val personId: UUID? = null,
    val sensitive: Boolean? = null
)

data class UpdateQuickNoteRequest(
    val text: String? = null,
    val personId: UUID? = null,
    val sensitive: Boolean? = null
)

data class AssignQuickNoteToPersonRequest(
    val personId: UUID
)
