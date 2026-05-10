package com.peoplemanager.adapters.web.dto

import jakarta.validation.constraints.NotBlank
import java.time.LocalDate

data class CreateKudosRequest(
    val date: LocalDate? = null,
    @field:NotBlank(message = "Text must not be blank")
    val text: String?,
    val tags: List<String>? = null
)
