package com.peoplemanager.adapters.web

import com.peoplemanager.adapters.auth.AuthenticatedUser
import com.peoplemanager.adapters.web.dto.ActionItemResponse
import com.peoplemanager.adapters.web.dto.CreateActionItemRequest
import com.peoplemanager.adapters.web.dto.PaginatedActionItemResponse
import com.peoplemanager.adapters.web.dto.UpdateActionItemRequest
import com.peoplemanager.application.commands.CancelActionItemCommand
import com.peoplemanager.application.commands.CompleteActionItemCommand
import com.peoplemanager.application.commands.CreateActionItemCommand
import com.peoplemanager.application.commands.DeleteActionItemCommand
import com.peoplemanager.application.commands.UpdateActionItemCommand
import com.peoplemanager.application.ports.ActionItemCommandPort
import com.peoplemanager.application.ports.ActionItemQueryPort
import com.peoplemanager.application.queries.GetActionItemQuery
import com.peoplemanager.application.queries.ListActionItemsByPersonQuery
import com.peoplemanager.application.queries.ListAllActionItemsQuery
import com.peoplemanager.application.queries.CountOpenActionItemsQuery
import com.peoplemanager.domain.ActionItemId
import com.peoplemanager.domain.ActionItemOwnerType
import com.peoplemanager.domain.ActionItemStatus
import com.peoplemanager.domain.OneOnOneEntryId
import com.peoplemanager.domain.PersonId
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
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1")
class ActionItemController(
    private val actionItemCommandPort: ActionItemCommandPort,
    private val actionItemQueryPort: ActionItemQueryPort
) {

    // ===== Per-Person Action Items =====

    @PostMapping("/persons/{personId}/action-items")
    fun createActionItem(
        @PathVariable personId: UUID,
        @Valid @RequestBody request: CreateActionItemRequest
    ): ResponseEntity<ActionItemResponse> {
        val userId = AuthenticatedUser.getUserId()
        val command = CreateActionItemCommand(
            userId = userId,
            personId = PersonId(personId),
            title = request.title!!,
            description = request.description,
            ownerType = request.ownerType ?: ActionItemOwnerType.MANAGER,
            dueDate = request.dueDate,
            originatingEntryId = request.originatingEntryId?.let { OneOnOneEntryId(it) }
        )
        val item = actionItemCommandPort.createActionItem(command)
        return ResponseEntity.status(HttpStatus.CREATED).body(ActionItemResponse.from(item))
    }

    @GetMapping("/persons/{personId}/action-items")
    fun listActionItemsByPerson(
        @PathVariable personId: UUID,
        @RequestParam(required = false) status: ActionItemStatus?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<PaginatedActionItemResponse> {
        val userId = AuthenticatedUser.getUserId()
        val query = ListActionItemsByPersonQuery(
            userId = userId,
            personId = PersonId(personId),
            status = status,
            page = page,
            size = size
        )
        val result = actionItemQueryPort.listActionItemsByPerson(query)
        return ResponseEntity.ok(PaginatedActionItemResponse.from(result))
    }

    @GetMapping("/persons/{personId}/action-items/{actionItemId}")
    fun getActionItem(
        @PathVariable personId: UUID,
        @PathVariable actionItemId: UUID
    ): ResponseEntity<ActionItemResponse> {
        val userId = AuthenticatedUser.getUserId()
        val query = GetActionItemQuery(
            userId = userId,
            personId = PersonId(personId),
            actionItemId = ActionItemId(actionItemId)
        )
        val item = actionItemQueryPort.getActionItem(query)
        return ResponseEntity.ok(ActionItemResponse.from(item))
    }

    @PutMapping("/persons/{personId}/action-items/{actionItemId}")
    fun updateActionItem(
        @PathVariable personId: UUID,
        @PathVariable actionItemId: UUID,
        @Valid @RequestBody request: UpdateActionItemRequest
    ): ResponseEntity<ActionItemResponse> {
        val userId = AuthenticatedUser.getUserId()
        val command = UpdateActionItemCommand(
            userId = userId,
            personId = PersonId(personId),
            actionItemId = ActionItemId(actionItemId),
            title = request.title,
            description = request.description,
            ownerType = request.ownerType,
            dueDate = request.dueDate
        )
        val item = actionItemCommandPort.updateActionItem(command)
        return ResponseEntity.ok(ActionItemResponse.from(item))
    }

    @PostMapping("/persons/{personId}/action-items/{actionItemId}/complete")
    fun completeActionItem(
        @PathVariable personId: UUID,
        @PathVariable actionItemId: UUID
    ): ResponseEntity<ActionItemResponse> {
        val userId = AuthenticatedUser.getUserId()
        val command = CompleteActionItemCommand(
            userId = userId,
            personId = PersonId(personId),
            actionItemId = ActionItemId(actionItemId)
        )
        val item = actionItemCommandPort.completeActionItem(command)
        return ResponseEntity.ok(ActionItemResponse.from(item))
    }

    @PostMapping("/persons/{personId}/action-items/{actionItemId}/cancel")
    fun cancelActionItem(
        @PathVariable personId: UUID,
        @PathVariable actionItemId: UUID
    ): ResponseEntity<ActionItemResponse> {
        val userId = AuthenticatedUser.getUserId()
        val command = CancelActionItemCommand(
            userId = userId,
            personId = PersonId(personId),
            actionItemId = ActionItemId(actionItemId)
        )
        val item = actionItemCommandPort.cancelActionItem(command)
        return ResponseEntity.ok(ActionItemResponse.from(item))
    }

    @DeleteMapping("/persons/{personId}/action-items/{actionItemId}")
    fun deleteActionItem(
        @PathVariable personId: UUID,
        @PathVariable actionItemId: UUID
    ): ResponseEntity<Void> {
        val userId = AuthenticatedUser.getUserId()
        val command = DeleteActionItemCommand(
            userId = userId,
            personId = PersonId(personId),
            actionItemId = ActionItemId(actionItemId)
        )
        actionItemCommandPort.deleteActionItem(command)
        return ResponseEntity.noContent().build()
    }

    // ===== Cross-Person Action Items (Manager-wide) =====

    @GetMapping("/action-items")
    fun listAllActionItems(
        @RequestParam(required = false) status: ActionItemStatus?,
        @RequestParam(defaultValue = "false") overdueOnly: Boolean,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<PaginatedActionItemResponse> {
        val userId = AuthenticatedUser.getUserId()
        val query = ListAllActionItemsQuery(
            userId = userId,
            status = status,
            overdueOnly = overdueOnly,
            page = page,
            size = size
        )
        val result = actionItemQueryPort.listAllActionItems(query)
        return ResponseEntity.ok(PaginatedActionItemResponse.from(result))
    }
}
