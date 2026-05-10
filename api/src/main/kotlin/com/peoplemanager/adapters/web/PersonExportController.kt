package com.peoplemanager.adapters.web

import com.peoplemanager.adapters.auth.AuthenticatedUser
import com.peoplemanager.application.ports.PersonExportPort
import com.peoplemanager.application.queries.ExportPersonDataQuery
import com.peoplemanager.domain.PersonId
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.UUID

@RestController
@RequestMapping("/api/v1/persons")
class PersonExportController(
    private val personExportPort: PersonExportPort
) {

    @GetMapping("/{id}/export")
    fun exportPerson(
        @PathVariable id: UUID,
        @RequestParam(required = false) dateFrom: LocalDate?,
        @RequestParam(required = false) dateTo: LocalDate?
    ): ResponseEntity<ByteArray> {
        val userId = AuthenticatedUser.getUserId()
        val query = ExportPersonDataQuery(
            userId = userId,
            personId = PersonId(id),
            dateFrom = dateFrom,
            dateTo = dateTo
        )
        val markdown = personExportPort.exportPersonMarkdown(query)
        val bytes = markdown.toByteArray(Charsets.UTF_8)

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"export.md\"")
            .header(HttpHeaders.CONTENT_TYPE, "text/markdown; charset=UTF-8")
            .contentLength(bytes.size.toLong())
            .body(bytes)
    }
}
