package com.peoplemanager.adapters.web.dto

import jakarta.validation.constraints.NotBlank
import java.time.LocalDate

data class CreatePdpGoalRequest(
    @field:NotBlank(message = "Title must not be blank")
    val title: String?,
    val description: String? = null,
    val targetDate: LocalDate? = null
)

data class UpdatePdpGoalRequest(
    val title: String? = null,
    val description: String? = null,
    val targetDate: LocalDate? = null
)

data class CreatePdpUpdateRequest(
    @field:NotBlank(message = "Text must not be blank")
    val textMarkdown: String?,
    val sensitive: Boolean? = null
)
