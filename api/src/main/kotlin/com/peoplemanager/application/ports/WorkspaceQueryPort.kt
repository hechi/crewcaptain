package com.peoplemanager.application.ports

import com.peoplemanager.application.queries.GetWorkspaceQuery
import com.peoplemanager.application.queries.ListWorkspacesQuery
import com.peoplemanager.domain.Workspace

interface WorkspaceQueryPort {
    fun getWorkspace(query: GetWorkspaceQuery): Workspace
    fun listWorkspaces(query: ListWorkspacesQuery): List<Workspace>
}
