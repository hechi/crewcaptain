package com.peoplemanager.adapters.web.dto

import com.peoplemanager.domain.Workspace
import java.time.Instant
import java.util.UUID

data class WorkspaceResponse(
    val id: UUID,
    val name: String,
    val description: String?,
    val displayOrder: Int,
    val createdAt: Instant,
    val updatedAt: Instant
) {
    companion object {
        fun from(workspace: Workspace): WorkspaceResponse = WorkspaceResponse(
            id = workspace.id.value,
            name = workspace.name,
            description = workspace.description,
            displayOrder = workspace.displayOrder,
            createdAt = workspace.createdAt,
            updatedAt = workspace.updatedAt
        )
    }
}
