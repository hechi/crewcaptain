package com.peoplemanager.application.ports

import com.peoplemanager.application.commands.AssignPersonToWorkspaceCommand
import com.peoplemanager.application.commands.CreateWorkspaceCommand
import com.peoplemanager.application.commands.DeleteWorkspaceCommand
import com.peoplemanager.application.commands.UpdateWorkspaceCommand
import com.peoplemanager.domain.Person
import com.peoplemanager.domain.Workspace

interface WorkspaceCommandPort {
    fun createWorkspace(command: CreateWorkspaceCommand): Workspace
    fun updateWorkspace(command: UpdateWorkspaceCommand): Workspace
    fun deleteWorkspace(command: DeleteWorkspaceCommand)
    fun assignPersonToWorkspace(command: AssignPersonToWorkspaceCommand): Person
}
