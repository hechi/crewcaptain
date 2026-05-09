package com.peoplemanager.adapters.web.dto

import com.peoplemanager.domain.ActionItemOwnerType
import jakarta.validation.constraints.NotBlank
import java.time.LocalDate
import java.util.UUID

data class CreateActionItemRequest(
    @field:NotBlank(message = "Title must not be blank")
    val title: String?,
    val description: String? = null,
    val ownerType: ActionItemOwnerType? = null,
    val dueDate: LocalDate? = null,
    val originatingEntryId: UUID? = null
)

data class UpdateActionItemRequest(
    val title: String? = null,
    val description: String? = null,
    val ownerType: ActionItemOwnerType? = null,
    val dueDate: LocalDate? = null
)
