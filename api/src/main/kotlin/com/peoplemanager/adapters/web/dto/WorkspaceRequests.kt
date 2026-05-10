package com.peoplemanager.adapters.web.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.util.UUID

data class CreateWorkspaceRequest(
    @field:NotBlank(message = "Name must not be blank")
    @field:Size(max = 100, message = "Name must not exceed 100 characters")
    val name: String,
    @field:Size(max = 500, message = "Description must not exceed 500 characters")
    val description: String? = null
)

data class UpdateWorkspaceRequest(
    @field:Size(max = 100, message = "Name must not exceed 100 characters")
    val name: String? = null,
    @field:Size(max = 500, message = "Description must not exceed 500 characters")
    val description: String? = null
)

data class AssignPersonToWorkspaceRequest(
    val workspaceId: UUID? = null
)
