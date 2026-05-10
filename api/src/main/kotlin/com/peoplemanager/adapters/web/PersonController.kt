package com.peoplemanager.adapters.web

import com.peoplemanager.adapters.auth.AuthenticatedUser
import com.peoplemanager.adapters.web.dto.AddRememberItemRequest
import com.peoplemanager.adapters.web.dto.CreatePersonRequest
import com.peoplemanager.adapters.web.dto.PaginatedPersonResponse
import com.peoplemanager.adapters.web.dto.PersonResponse
import com.peoplemanager.adapters.web.dto.RememberItemResponse
import com.peoplemanager.adapters.web.dto.ReorderRememberItemsRequest
import com.peoplemanager.adapters.web.dto.SetMoraleRequest
import com.peoplemanager.adapters.web.dto.UpdatePersonRequest
import com.peoplemanager.application.commands.AddRememberItemCommand
import com.peoplemanager.application.commands.CreatePersonCommand
import com.peoplemanager.application.commands.DeletePersonCommand
import com.peoplemanager.application.commands.PermanentDeletePersonCommand
import com.peoplemanager.application.commands.RemoveRememberItemCommand
import com.peoplemanager.application.commands.ReorderRememberItemsCommand
import com.peoplemanager.application.commands.RestorePersonCommand
import com.peoplemanager.application.commands.SetMoraleCommand
import com.peoplemanager.application.commands.UpdatePersonCommand
import com.peoplemanager.application.ports.ActionItemQueryPort
import com.peoplemanager.application.ports.OneOnOneQueryPort
import com.peoplemanager.application.ports.PdpGoalQueryPort
import com.peoplemanager.application.ports.PersonCommandPort
import com.peoplemanager.application.ports.PersonQueryPort
import com.peoplemanager.application.queries.CountOpenActionItemsQuery
import com.peoplemanager.application.queries.CountActivePdpGoalsQuery
import com.peoplemanager.application.queries.GetLastOneOnOneDateQuery
import com.peoplemanager.application.queries.GetPersonQuery
import com.peoplemanager.application.queries.ListDeletedPersonsQuery
import com.peoplemanager.application.queries.ListPersonsQuery
import com.peoplemanager.domain.MoraleStatus
import com.peoplemanager.domain.PersonId
import com.peoplemanager.domain.RememberItemId
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
@RequestMapping("/api/v1/persons")
class PersonController(
    private val personCommandPort: PersonCommandPort,
    private val personQueryPort: PersonQueryPort,
    private val oneOnOneQueryPort: OneOnOneQueryPort,
    private val actionItemQueryPort: ActionItemQueryPort,
    private val pdpGoalQueryPort: PdpGoalQueryPort
) {

    @PostMapping
    fun createPerson(@Valid @RequestBody request: CreatePersonRequest): ResponseEntity<PersonResponse> {
        val userId = AuthenticatedUser.getUserId()
        val command = CreatePersonCommand(
            userId = userId,
            name = request.name,
            preferredName = request.preferredName,
            roleTitle = request.roleTitle,
            timezone = request.timezone,
            startDate = request.startDate,
            email = request.email,
            tags = request.tags ?: emptyList()
        )
        val person = personCommandPort.createPerson(command)
        return ResponseEntity.status(HttpStatus.CREATED).body(PersonResponse.from(person))
    }

    @GetMapping("/{id}")
    fun getPerson(@PathVariable id: UUID): ResponseEntity<PersonResponse> {
        val userId = AuthenticatedUser.getUserId()
        val query = GetPersonQuery(userId = userId, personId = PersonId(id))
        val person = personQueryPort.getPerson(query)
        val last1on1Date = oneOnOneQueryPort.getLastOneOnOneDate(
            GetLastOneOnOneDateQuery(userId = userId, personId = PersonId(id))
        )
        val openActionItemsCount = actionItemQueryPort.countOpenActionItems(
            CountOpenActionItemsQuery(userId = userId, personId = PersonId(id))
        )
        val activePdpGoalsCount = pdpGoalQueryPort.countActivePdpGoals(
            CountActivePdpGoalsQuery(userId = userId, personId = PersonId(id))
        )
        return ResponseEntity.ok(PersonResponse.from(person, last1on1Date, openActionItemsCount.toInt(), activePdpGoalsCount.toInt()))
    }

    @PutMapping("/{id}")
    fun updatePerson(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdatePersonRequest
    ): ResponseEntity<PersonResponse> {
        val userId = AuthenticatedUser.getUserId()
        val command = UpdatePersonCommand(
            userId = userId,
            personId = PersonId(id),
            name = request.name,
            preferredName = request.preferredName,
            roleTitle = request.roleTitle,
            timezone = request.timezone,
            startDate = request.startDate,
            email = request.email,
            tags = request.tags ?: emptyList()
        )
        val person = personCommandPort.updatePerson(command)
        return ResponseEntity.ok(PersonResponse.from(person))
    }

    @DeleteMapping("/{id}")
    fun deletePerson(@PathVariable id: UUID): ResponseEntity<Void> {
        val userId = AuthenticatedUser.getUserId()
        val command = DeletePersonCommand(userId = userId, personId = PersonId(id))
        personCommandPort.deletePerson(command)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/{id}/restore")
    fun restorePerson(@PathVariable id: UUID): ResponseEntity<PersonResponse> {
        val userId = AuthenticatedUser.getUserId()
        val command = RestorePersonCommand(userId = userId, personId = PersonId(id))
        val person = personCommandPort.restorePerson(command)
        return ResponseEntity.ok(PersonResponse.from(person))
    }

    @DeleteMapping("/{id}/permanent")
    fun permanentDeletePerson(@PathVariable id: UUID): ResponseEntity<Void> {
        val userId = AuthenticatedUser.getUserId()
        val command = PermanentDeletePersonCommand(userId = userId, personId = PersonId(id))
        personCommandPort.permanentDeletePerson(command)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/trash")
    fun listDeletedPersons(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<PaginatedPersonResponse> {
        val userId = AuthenticatedUser.getUserId()
        val query = ListDeletedPersonsQuery(userId = userId, page = page, size = size)
        val result = personQueryPort.listDeletedPersons(query)
        return ResponseEntity.ok(PaginatedPersonResponse.from(result))
    }

    @GetMapping
    fun listPersons(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) tag: String?,
        @RequestParam(required = false) morale: MoraleStatus?
    ): ResponseEntity<PaginatedPersonResponse> {
        val userId = AuthenticatedUser.getUserId()
        val query = ListPersonsQuery(
            userId = userId,
            page = page,
            size = size,
            tagFilter = tag,
            moraleFilter = morale
        )
        val result = personQueryPort.listPersons(query)
        return ResponseEntity.ok(PaginatedPersonResponse.from(result))
    }

    @PutMapping("/{id}/morale")
    fun setMorale(
        @PathVariable id: UUID,
        @Valid @RequestBody request: SetMoraleRequest
    ): ResponseEntity<PersonResponse> {
        val userId = AuthenticatedUser.getUserId()
        val command = SetMoraleCommand(
            userId = userId,
            personId = PersonId(id),
            status = request.status!!,
            note = request.note
        )
        val person = personCommandPort.setMorale(command)
        return ResponseEntity.ok(PersonResponse.from(person))
    }

    @PostMapping("/{id}/remember-items")
    fun addRememberItem(
        @PathVariable id: UUID,
        @Valid @RequestBody request: AddRememberItemRequest
    ): ResponseEntity<List<RememberItemResponse>> {
        val userId = AuthenticatedUser.getUserId()
        val command = AddRememberItemCommand(
            userId = userId,
            personId = PersonId(id),
            text = request.text
        )
        val items = personCommandPort.addRememberItem(command)
        return ResponseEntity.status(HttpStatus.CREATED).body(items.map { RememberItemResponse.from(it) })
    }

    @DeleteMapping("/{id}/remember-items/{itemId}")
    fun removeRememberItem(
        @PathVariable id: UUID,
        @PathVariable itemId: UUID
    ): ResponseEntity<List<RememberItemResponse>> {
        val userId = AuthenticatedUser.getUserId()
        val command = RemoveRememberItemCommand(
            userId = userId,
            personId = PersonId(id),
            itemId = RememberItemId(itemId)
        )
        val items = personCommandPort.removeRememberItem(command)
        return ResponseEntity.ok(items.map { RememberItemResponse.from(it) })
    }

    @PutMapping("/{id}/remember-items/reorder")
    fun reorderRememberItems(
        @PathVariable id: UUID,
        @Valid @RequestBody request: ReorderRememberItemsRequest
    ): ResponseEntity<List<RememberItemResponse>> {
        val userId = AuthenticatedUser.getUserId()
        val command = ReorderRememberItemsCommand(
            userId = userId,
            personId = PersonId(id),
            orderedIds = request.orderedIds.map { RememberItemId(it) }
        )
        val items = personCommandPort.reorderRememberItems(command)
        return ResponseEntity.ok(items.map { RememberItemResponse.from(it) })
    }
}
