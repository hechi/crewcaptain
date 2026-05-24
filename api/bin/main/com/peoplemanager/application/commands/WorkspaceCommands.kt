package com.peoplemanager.application.commands

import com.peoplemanager.domain.PersonId
import com.peoplemanager.domain.UserId
import com.peoplemanager.domain.WorkspaceId

data class CreateWorkspaceCommand(
    val userId: UserId,
    val name: String,
    val description: String? = null
)

data class UpdateWorkspaceCommand(
    val userId: UserId,
    val workspaceId: WorkspaceId,
    val name: String? = null,
    val description: String? = null
)

data class DeleteWorkspaceCommand(
    val userId: UserId,
    val workspaceId: WorkspaceId
)

data class AssignPersonToWorkspaceCommand(
    val userId: UserId,
    val personId: PersonId,
    val workspaceId: WorkspaceId?
)
