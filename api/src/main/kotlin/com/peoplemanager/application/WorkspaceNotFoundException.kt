package com.peoplemanager.application

import com.peoplemanager.domain.WorkspaceId

class WorkspaceNotFoundException(val workspaceId: WorkspaceId) : RuntimeException("Workspace not found: ${workspaceId.value}")
