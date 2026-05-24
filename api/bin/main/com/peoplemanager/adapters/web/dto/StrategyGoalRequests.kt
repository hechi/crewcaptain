package com.peoplemanager.adapters.web.dto

import jakarta.validation.constraints.NotBlank
import java.time.LocalDate

data class CreateStrategyGoalRequest(
    @field:NotBlank(message = "Title must not be blank")
    val title: String?,
    val description: String? = null,
    val targetDate: LocalDate? = null,
    val sensitive: Boolean? = false
)

data class UpdateStrategyGoalRequest(
    val title: String? = null,
    val description: String? = null,
    val targetDate: LocalDate? = null
)

data class LinkPdpGoalRequest(
    @field:NotBlank(message = "PDP Goal ID must not be blank")
    val pdpGoalId: String?,
    @field:NotBlank(message = "Person ID must not be blank")
    val personId: String?
)
