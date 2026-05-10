package com.peoplemanager.application

import com.peoplemanager.application.commands.AssignPersonToWorkspaceCommand
import com.peoplemanager.application.commands.CreateWorkspaceCommand
import com.peoplemanager.application.commands.DeleteWorkspaceCommand
import com.peoplemanager.application.commands.UpdateWorkspaceCommand
import com.peoplemanager.application.ports.PersonRepository
import com.peoplemanager.application.ports.WorkspaceCommandPort
import com.peoplemanager.application.ports.WorkspaceQueryPort
import com.peoplemanager.application.ports.WorkspaceRepository
import com.peoplemanager.application.queries.GetWorkspaceQuery
import com.peoplemanager.application.queries.ListWorkspacesQuery
import com.peoplemanager.domain.AuditLogEntry
import com.peoplemanager.domain.Person
import com.peoplemanager.domain.Workspace
import com.peoplemanager.domain.WorkspaceId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class WorkspaceService(
    private val workspaceRepository: WorkspaceRepository,
    private val personRepository: PersonRepository,
    private val auditLogService: AuditLogService
) : WorkspaceCommandPort, WorkspaceQueryPort {

    override fun createWorkspace(command: CreateWorkspaceCommand): Workspace {
        val existingCount = workspaceRepository.countByUserId(command.userId)
        val workspace = Workspace(
            id = WorkspaceId.generate(),
            userId = command.userId,
            name = command.name,
            description = command.description,
            displayOrder = existingCount
        )
        val saved = workspaceRepository.save(workspace)
        auditLogService.record(AuditLogEntry.workspaceCreated(command.userId, saved.id, saved.name))
        return saved
    }

    override fun updateWorkspace(command: UpdateWorkspaceCommand): Workspace {
        val existing = workspaceRepository.findByIdAndUserId(command.workspaceId, command.userId)
            ?: throw WorkspaceNotFoundException(command.workspaceId)

        val updated = existing.updateDetails(
            name = command.name,
            description = command.description
        )
        val saved = workspaceRepository.save(updated)
        auditLogService.record(AuditLogEntry.workspaceUpdated(command.userId, saved.id, saved.name))
        return saved
    }

    override fun deleteWorkspace(command: DeleteWorkspaceCommand) {
        val existing = workspaceRepository.findByIdAndUserId(command.workspaceId, command.userId)
            ?: throw WorkspaceNotFoundException(command.workspaceId)

        // Persons in this workspace will have workspace_id set to NULL (ON DELETE SET NULL in DB)
        val deleted = workspaceRepository.deleteByIdAndUserId(command.workspaceId, command.userId)
        if (!deleted) throw WorkspaceNotFoundException(command.workspaceId)
        auditLogService.record(AuditLogEntry.workspaceDeleted(command.userId, command.workspaceId, existing.name))
    }

    override fun assignPersonToWorkspace(command: AssignPersonToWorkspaceCommand): Person {
        val person = personRepository.findByIdAndUserId(command.personId, command.userId)
            ?: throw PersonNotFoundException(command.personId)

        // If assigning to a workspace, verify it belongs to the same user
        if (command.workspaceId != null) {
            workspaceRepository.findByIdAndUserId(command.workspaceId, command.userId)
                ?: throw WorkspaceNotFoundException(command.workspaceId)
        }

        val updated = person.assignToWorkspace(command.workspaceId)
        return personRepository.save(updated)
    }

    override fun getWorkspace(query: GetWorkspaceQuery): Workspace {
        return workspaceRepository.findByIdAndUserId(query.workspaceId, query.userId)
            ?: throw WorkspaceNotFoundException(query.workspaceId)
    }

    override fun listWorkspaces(query: ListWorkspacesQuery): List<Workspace> {
        return workspaceRepository.findAllByUserId(query.userId)
    }
}
