package com.peoplemanager.adapters.web

import com.peoplemanager.adapters.auth.AuthenticatedUser
import com.peoplemanager.adapters.web.dto.AssignPersonToWorkspaceRequest
import com.peoplemanager.adapters.web.dto.CreateWorkspaceRequest
import com.peoplemanager.adapters.web.dto.PersonResponse
import com.peoplemanager.adapters.web.dto.UpdateWorkspaceRequest
import com.peoplemanager.adapters.web.dto.WorkspaceResponse
import com.peoplemanager.application.commands.AssignPersonToWorkspaceCommand
import com.peoplemanager.application.commands.CreateWorkspaceCommand
import com.peoplemanager.application.commands.DeleteWorkspaceCommand
import com.peoplemanager.application.commands.UpdateWorkspaceCommand
import com.peoplemanager.application.port.input.WorkspaceCommandPort
import com.peoplemanager.application.port.input.WorkspaceQueryPort
import com.peoplemanager.application.queries.GetWorkspaceQuery
import com.peoplemanager.application.queries.ListWorkspacesQuery
import com.peoplemanager.domain.PersonId
import com.peoplemanager.domain.WorkspaceId
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/workspaces")
class WorkspaceController(
    private val workspaceCommandPort: WorkspaceCommandPort,
    private val workspaceQueryPort: WorkspaceQueryPort
) {

    @PostMapping
    fun createWorkspace(
        @Valid @RequestBody request: CreateWorkspaceRequest
    ): ResponseEntity<WorkspaceResponse> {
        val userId = AuthenticatedUser.getUserId()
        val command = CreateWorkspaceCommand(
            userId = userId,
            name = request.name,
            description = request.description
        )
        val workspace = workspaceCommandPort.createWorkspace(command)
        return ResponseEntity.status(HttpStatus.CREATED).body(WorkspaceResponse.from(workspace))
    }

    @GetMapping
    fun listWorkspaces(): ResponseEntity<List<WorkspaceResponse>> {
        val userId = AuthenticatedUser.getUserId()
        val query = ListWorkspacesQuery(userId = userId)
        val workspaces = workspaceQueryPort.listWorkspaces(query)
        return ResponseEntity.ok(workspaces.map { WorkspaceResponse.from(it) })
    }

    @GetMapping("/{workspaceId}")
    fun getWorkspace(@PathVariable workspaceId: UUID): ResponseEntity<WorkspaceResponse> {
        val userId = AuthenticatedUser.getUserId()
        val query = GetWorkspaceQuery(userId = userId, workspaceId = WorkspaceId(workspaceId))
        val workspace = workspaceQueryPort.getWorkspace(query)
        return ResponseEntity.ok(WorkspaceResponse.from(workspace))
    }

    @PutMapping("/{workspaceId}")
    fun updateWorkspace(
        @PathVariable workspaceId: UUID,
        @Valid @RequestBody request: UpdateWorkspaceRequest
    ): ResponseEntity<WorkspaceResponse> {
        val userId = AuthenticatedUser.getUserId()
        val command = UpdateWorkspaceCommand(
            userId = userId,
            workspaceId = WorkspaceId(workspaceId),
            name = request.name,
            description = request.description
        )
        val workspace = workspaceCommandPort.updateWorkspace(command)
        return ResponseEntity.ok(WorkspaceResponse.from(workspace))
    }

    @DeleteMapping("/{workspaceId}")
    fun deleteWorkspace(@PathVariable workspaceId: UUID): ResponseEntity<Void> {
        val userId = AuthenticatedUser.getUserId()
        val command = DeleteWorkspaceCommand(
            userId = userId,
            workspaceId = WorkspaceId(workspaceId)
        )
        workspaceCommandPort.deleteWorkspace(command)
        return ResponseEntity.noContent().build()
    }

    @PutMapping("/persons/{personId}/workspace")
    fun assignPersonToWorkspace(
        @PathVariable personId: UUID,
        @RequestBody request: AssignPersonToWorkspaceRequest
    ): ResponseEntity<PersonResponse> {
        val userId = AuthenticatedUser.getUserId()
        val command = AssignPersonToWorkspaceCommand(
            userId = userId,
            personId = PersonId(personId),
            workspaceId = request.workspaceId?.let { WorkspaceId(it) }
        )
        val person = workspaceCommandPort.assignPersonToWorkspace(command)
        return ResponseEntity.ok(PersonResponse.from(person))
    }
}
