package com.peoplemanager.adapters.web.dto

import com.peoplemanager.domain.MoraleStatus
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import java.time.LocalDate
import java.util.UUID

data class CreatePersonRequest(
    @field:NotBlank(message = "Name must not be blank")
    val name: String,
    val preferredName: String? = null,
    val roleTitle: String? = null,
    val timezone: String? = null,
    val startDate: LocalDate? = null,
    val email: String? = null,
    val tags: List<String>? = null
)

data class UpdatePersonRequest(
    @field:NotBlank(message = "Name must not be blank")
    val name: String,
    val preferredName: String? = null,
    val roleTitle: String? = null,
    val timezone: String? = null,
    val startDate: LocalDate? = null,
    val email: String? = null,
    val tags: List<String>? = null
)

data class SetMoraleRequest(
    @field:NotNull(message = "Status must not be null")
    val status: MoraleStatus?,
    val note: String? = null
)

data class AddRememberItemRequest(
    @field:NotBlank(message = "Text must not be blank")
    val text: String
)

data class ReorderRememberItemsRequest(
    @field:NotEmpty(message = "Ordered IDs must not be empty")
    val orderedIds: List<UUID>
)
