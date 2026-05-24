package com.peoplemanager.application.queries

import com.peoplemanager.domain.UserId
import com.peoplemanager.domain.WorkspaceId

data class GetWorkspaceQuery(
    val userId: UserId,
    val workspaceId: WorkspaceId
)

data class ListWorkspacesQuery(
    val userId: UserId
)
