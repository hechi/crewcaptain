package com.peoplemanager.adapters.web

import com.peoplemanager.adapters.auth.AuthenticatedUser
import com.peoplemanager.adapters.web.dto.*
import com.peoplemanager.application.commands.*
import com.peoplemanager.application.ports.PdpGoalCommandPort
import com.peoplemanager.application.ports.PdpGoalQueryPort
import com.peoplemanager.application.queries.CountActivePdpGoalsQuery
import com.peoplemanager.application.queries.GetPdpGoalQuery
import com.peoplemanager.application.queries.ListPdpGoalsByPersonQuery
import com.peoplemanager.application.queries.ListPdpUpdatesByGoalQuery
import com.peoplemanager.domain.PdpGoalId
import com.peoplemanager.domain.PdpGoalStatus
import com.peoplemanager.domain.PdpUpdateId
import com.peoplemanager.domain.PersonId
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1")
class PdpGoalController(
    private val pdpGoalCommandPort: PdpGoalCommandPort,
    private val pdpGoalQueryPort: PdpGoalQueryPort
) {

    // ===== PDP Goals =====

    @PostMapping("/persons/{personId}/pdp-goals")
    fun createPdpGoal(
        @PathVariable personId: UUID,
        @Valid @RequestBody request: CreatePdpGoalRequest
    ): ResponseEntity<PdpGoalResponse> {
        val userId = AuthenticatedUser.getUserId()
        val command = CreatePdpGoalCommand(
            userId = userId,
            personId = PersonId(personId),
            title = request.title!!,
            description = request.description,
            targetDate = request.targetDate
        )
        val goal = pdpGoalCommandPort.createPdpGoal(command)
        return ResponseEntity.status(HttpStatus.CREATED).body(PdpGoalResponse.from(goal))
    }

    @GetMapping("/persons/{personId}/pdp-goals")
    fun listPdpGoalsByPerson(
        @PathVariable personId: UUID,
        @RequestParam(required = false) status: PdpGoalStatus?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<PaginatedPdpGoalResponse> {
        val userId = AuthenticatedUser.getUserId()
        val query = ListPdpGoalsByPersonQuery(
            userId = userId,
            personId = PersonId(personId),
            status = status,
            page = page,
            size = size
        )
        val result = pdpGoalQueryPort.listPdpGoalsByPerson(query)
        return ResponseEntity.ok(PaginatedPdpGoalResponse.from(result))
    }

    @GetMapping("/persons/{personId}/pdp-goals/{goalId}")
    fun getPdpGoal(
        @PathVariable personId: UUID,
        @PathVariable goalId: UUID
    ): ResponseEntity<PdpGoalResponse> {
        val userId = AuthenticatedUser.getUserId()
        val query = GetPdpGoalQuery(
            userId = userId,
            personId = PersonId(personId),
            goalId = PdpGoalId(goalId)
        )
        val goal = pdpGoalQueryPort.getPdpGoal(query)
        return ResponseEntity.ok(PdpGoalResponse.from(goal))
    }

    @PutMapping("/persons/{personId}/pdp-goals/{goalId}")
    fun updatePdpGoal(
        @PathVariable personId: UUID,
        @PathVariable goalId: UUID,
        @Valid @RequestBody request: UpdatePdpGoalRequest
    ): ResponseEntity<PdpGoalResponse> {
        val userId = AuthenticatedUser.getUserId()
        val command = UpdatePdpGoalCommand(
            userId = userId,
            personId = PersonId(personId),
            goalId = PdpGoalId(goalId),
            title = request.title,
            description = request.description,
            targetDate = request.targetDate
        )
        val goal = pdpGoalCommandPort.updatePdpGoal(command)
        return ResponseEntity.ok(PdpGoalResponse.from(goal))
    }

    @PostMapping("/persons/{personId}/pdp-goals/{goalId}/achieve")
    fun achievePdpGoal(
        @PathVariable personId: UUID,
        @PathVariable goalId: UUID
    ): ResponseEntity<PdpGoalResponse> {
        val userId = AuthenticatedUser.getUserId()
        val command = AchievePdpGoalCommand(
            userId = userId,
            personId = PersonId(personId),
            goalId = PdpGoalId(goalId)
        )
        val goal = pdpGoalCommandPort.achievePdpGoal(command)
        return ResponseEntity.ok(PdpGoalResponse.from(goal))
    }

    @PostMapping("/persons/{personId}/pdp-goals/{goalId}/pause")
    fun pausePdpGoal(
        @PathVariable personId: UUID,
        @PathVariable goalId: UUID
    ): ResponseEntity<PdpGoalResponse> {
        val userId = AuthenticatedUser.getUserId()
        val command = PausePdpGoalCommand(
            userId = userId,
            personId = PersonId(personId),
            goalId = PdpGoalId(goalId)
        )
        val goal = pdpGoalCommandPort.pausePdpGoal(command)
        return ResponseEntity.ok(PdpGoalResponse.from(goal))
    }

    @PostMapping("/persons/{personId}/pdp-goals/{goalId}/drop")
    fun dropPdpGoal(
        @PathVariable personId: UUID,
        @PathVariable goalId: UUID
    ): ResponseEntity<PdpGoalResponse> {
        val userId = AuthenticatedUser.getUserId()
        val command = DropPdpGoalCommand(
            userId = userId,
            personId = PersonId(personId),
            goalId = PdpGoalId(goalId)
        )
        val goal = pdpGoalCommandPort.dropPdpGoal(command)
        return ResponseEntity.ok(PdpGoalResponse.from(goal))
    }

    @PostMapping("/persons/{personId}/pdp-goals/{goalId}/resume")
    fun resumePdpGoal(
        @PathVariable personId: UUID,
        @PathVariable goalId: UUID
    ): ResponseEntity<PdpGoalResponse> {
        val userId = AuthenticatedUser.getUserId()
        val command = ResumePdpGoalCommand(
            userId = userId,
            personId = PersonId(personId),
            goalId = PdpGoalId(goalId)
        )
        val goal = pdpGoalCommandPort.resumePdpGoal(command)
        return ResponseEntity.ok(PdpGoalResponse.from(goal))
    }

    @DeleteMapping("/persons/{personId}/pdp-goals/{goalId}")
    fun deletePdpGoal(
        @PathVariable personId: UUID,
        @PathVariable goalId: UUID
    ): ResponseEntity<Void> {
        val userId = AuthenticatedUser.getUserId()
        val command = DeletePdpGoalCommand(
            userId = userId,
            personId = PersonId(personId),
            goalId = PdpGoalId(goalId)
        )
        pdpGoalCommandPort.deletePdpGoal(command)
        return ResponseEntity.noContent().build()
    }

    // ===== PDP Updates (Progress Notes) =====

    @PostMapping("/persons/{personId}/pdp-goals/{goalId}/updates")
    fun addPdpUpdate(
        @PathVariable personId: UUID,
        @PathVariable goalId: UUID,
        @Valid @RequestBody request: CreatePdpUpdateRequest
    ): ResponseEntity<PdpUpdateResponse> {
        val userId = AuthenticatedUser.getUserId()
        val command = AddPdpUpdateCommand(
            userId = userId,
            personId = PersonId(personId),
            goalId = PdpGoalId(goalId),
            textMarkdown = request.textMarkdown!!,
            sensitive = request.sensitive ?: false
        )
        val update = pdpGoalCommandPort.addPdpUpdate(command)
        return ResponseEntity.status(HttpStatus.CREATED).body(PdpUpdateResponse.from(update))
    }

    @GetMapping("/persons/{personId}/pdp-goals/{goalId}/updates")
    fun listPdpUpdates(
        @PathVariable personId: UUID,
        @PathVariable goalId: UUID,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<PaginatedPdpUpdateResponse> {
        val userId = AuthenticatedUser.getUserId()
        val query = ListPdpUpdatesByGoalQuery(
            userId = userId,
            personId = PersonId(personId),
            goalId = PdpGoalId(goalId),
            page = page,
            size = size
        )
        val result = pdpGoalQueryPort.listPdpUpdatesByGoal(query)
        return ResponseEntity.ok(PaginatedPdpUpdateResponse.from(result))
    }

    @DeleteMapping("/persons/{personId}/pdp-goals/{goalId}/updates/{updateId}")
    fun deletePdpUpdate(
        @PathVariable personId: UUID,
        @PathVariable goalId: UUID,
        @PathVariable updateId: UUID
    ): ResponseEntity<Void> {
        val userId = AuthenticatedUser.getUserId()
        val command = DeletePdpUpdateCommand(
            userId = userId,
            personId = PersonId(personId),
            goalId = PdpGoalId(goalId),
            updateId = PdpUpdateId(updateId)
        )
        pdpGoalCommandPort.deletePdpUpdate(command)
        return ResponseEntity.noContent().build()
    }
}
