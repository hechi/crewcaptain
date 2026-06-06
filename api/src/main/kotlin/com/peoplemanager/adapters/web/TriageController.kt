package com.peoplemanager.adapters.web

import com.peoplemanager.adapters.auth.AuthenticatedUser
import com.peoplemanager.adapters.web.dto.SnoozeActionItemRequest
import com.peoplemanager.adapters.web.dto.TriageHintResponse
import com.peoplemanager.adapters.web.dto.TriageQueueResponse
import com.peoplemanager.application.AiTriageHintService
import com.peoplemanager.application.commands.SnoozeActionItemCommand
import com.peoplemanager.application.port.input.TriageCommandPort
import com.peoplemanager.application.port.input.TriageQueryPort
import com.peoplemanager.application.queries.GetTriageQueueQuery
import com.peoplemanager.application.queries.OwnerScope
import com.peoplemanager.domain.*
import com.peoplemanager.adapters.web.dto.ActionItemResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

@RestController
@RequestMapping("/api/v1/triage")
class TriageController(
    private val triageQueryPort: TriageQueryPort,
    private val triageCommandPort: TriageCommandPort,
    private val aiTriageHintService: AiTriageHintService
) {

    @GetMapping
    fun getTriageQueue(
        @RequestParam(required = false) type: TriageItemType?,
        @RequestParam(required = false) workspaceId: List<UUID>?,
        @RequestParam(required = false) personId: UUID?,
        @RequestParam(defaultValue = "ALL") scope: OwnerScope
    ): ResponseEntity<TriageQueueResponse> {
        val userId = AuthenticatedUser.getUserId()
        val query = GetTriageQueueQuery(
            userId = userId,
            itemType = type,
            workspaceIds = workspaceId?.map { WorkspaceId(it) },
            personId = personId?.let { PersonId(it) },
            ownerScope = scope
        )
        val items = triageQueryPort.getTriageQueue(query)
        return ResponseEntity.ok(TriageQueueResponse.from(items))
    }

    @PostMapping("/items/{itemId}/hint")
    fun getTriageHint(
        @PathVariable itemId: String
    ): ResponseEntity<TriageHintResponse> {
        val userId = AuthenticatedUser.getUserId()
        // Fetch the queue to find the item
        val query = GetTriageQueueQuery(userId = userId)
        val items = triageQueryPort.getTriageQueue(query)
        val item = items.find { it.id == itemId }
            ?: return ResponseEntity.notFound().build()

        val result = aiTriageHintService.generateHint(userId, item)
        return ResponseEntity.ok(TriageHintResponse(hint = result.hint, error = result.error))
    }

    @PostMapping("/persons/{personId}/action-items/{actionItemId}/snooze")
    fun snoozeActionItem(
        @PathVariable personId: UUID,
        @PathVariable actionItemId: UUID,
        @RequestBody request: SnoozeActionItemRequest
    ): ResponseEntity<ActionItemResponse> {
        val userId = AuthenticatedUser.getUserId()
        val snoozedUntil = request.snoozedUntil
            ?: Instant.now().plus(request.days!!.toLong(), ChronoUnit.DAYS)

        val command = SnoozeActionItemCommand(
            userId = userId,
            personId = PersonId(personId),
            actionItemId = ActionItemId(actionItemId),
            snoozedUntil = snoozedUntil
        )
        val item = triageCommandPort.snoozeActionItem(command)
        return ResponseEntity.ok(ActionItemResponse.from(item))
    }
}
