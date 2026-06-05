package com.peoplemanager.adapters.web

import com.peoplemanager.adapters.auth.AuthenticatedUser
import com.peoplemanager.adapters.web.dto.CreateKudosRequest
import com.peoplemanager.adapters.web.dto.KudosResponse
import com.peoplemanager.adapters.web.dto.PaginatedKudosResponse
import com.peoplemanager.application.commands.CreateKudosCommand
import com.peoplemanager.application.commands.DeleteKudosCommand
import com.peoplemanager.application.port.input.KudosCommandPort
import com.peoplemanager.application.port.input.KudosQueryPort
import com.peoplemanager.application.queries.GetKudosQuery
import com.peoplemanager.application.queries.ListAllKudosQuery
import com.peoplemanager.application.queries.ListKudosByPersonQuery
import com.peoplemanager.domain.KudosId
import com.peoplemanager.domain.PersonId
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.UUID

@RestController
@RequestMapping("/api/v1")
class KudosController(
    private val kudosCommandPort: KudosCommandPort,
    private val kudosQueryPort: KudosQueryPort
) {

    // ===== Per-Person Kudos =====

    @PostMapping("/persons/{personId}/kudos")
    fun createKudos(
        @PathVariable personId: UUID,
        @Valid @RequestBody request: CreateKudosRequest
    ): ResponseEntity<KudosResponse> {
        val userId = AuthenticatedUser.getUserId()
        val command = CreateKudosCommand(
            userId = userId,
            personId = PersonId(personId),
            date = request.date ?: LocalDate.now(),
            text = request.text!!,
            tags = request.tags ?: emptyList()
        )
        val kudos = kudosCommandPort.createKudos(command)
        return ResponseEntity.status(HttpStatus.CREATED).body(KudosResponse.from(kudos))
    }

    @GetMapping("/persons/{personId}/kudos")
    fun listKudosByPerson(
        @PathVariable personId: UUID,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<PaginatedKudosResponse> {
        val userId = AuthenticatedUser.getUserId()
        val query = ListKudosByPersonQuery(
            userId = userId,
            personId = PersonId(personId),
            page = page,
            size = size
        )
        val result = kudosQueryPort.listKudosByPerson(query)
        return ResponseEntity.ok(PaginatedKudosResponse.from(result))
    }

    @GetMapping("/persons/{personId}/kudos/{kudosId}")
    fun getKudos(
        @PathVariable personId: UUID,
        @PathVariable kudosId: UUID
    ): ResponseEntity<KudosResponse> {
        val userId = AuthenticatedUser.getUserId()
        val query = GetKudosQuery(
            userId = userId,
            personId = PersonId(personId),
            kudosId = KudosId(kudosId)
        )
        val kudos = kudosQueryPort.getKudos(query)
        return ResponseEntity.ok(KudosResponse.from(kudos))
    }

    @DeleteMapping("/persons/{personId}/kudos/{kudosId}")
    fun deleteKudos(
        @PathVariable personId: UUID,
        @PathVariable kudosId: UUID
    ): ResponseEntity<Void> {
        val userId = AuthenticatedUser.getUserId()
        val command = DeleteKudosCommand(
            userId = userId,
            personId = PersonId(personId),
            kudosId = KudosId(kudosId)
        )
        kudosCommandPort.deleteKudos(command)
        return ResponseEntity.noContent().build()
    }

    // ===== Cross-Person Kudos (Manager-wide) =====

    @GetMapping("/kudos")
    fun listAllKudos(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<PaginatedKudosResponse> {
        val userId = AuthenticatedUser.getUserId()
        val query = ListAllKudosQuery(
            userId = userId,
            page = page,
            size = size
        )
        val result = kudosQueryPort.listAllKudos(query)
        return ResponseEntity.ok(PaginatedKudosResponse.from(result))
    }
}
