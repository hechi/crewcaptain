package com.peoplemanager.application.port.output

import com.peoplemanager.domain.UserId
import com.peoplemanager.domain.Workspace
import com.peoplemanager.domain.WorkspaceId

interface WorkspaceRepository {
    fun save(workspace: Workspace): Workspace
    fun findByIdAndUserId(workspaceId: WorkspaceId, userId: UserId): Workspace?
    fun findAllByUserId(userId: UserId): List<Workspace>
    fun deleteByIdAndUserId(workspaceId: WorkspaceId, userId: UserId): Boolean
    fun countByUserId(userId: UserId): Int
}
