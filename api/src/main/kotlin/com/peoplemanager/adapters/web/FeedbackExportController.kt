package com.peoplemanager.adapters.web

import com.peoplemanager.adapters.auth.AuthenticatedUser
import com.peoplemanager.application.FeedbackExportService
import com.peoplemanager.domain.FeedbackResponseId
import com.peoplemanager.domain.PersonId
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/persons")
class FeedbackExportController(
    private val feedbackExportService: FeedbackExportService
) {

    @GetMapping("/{id}/feedback-export")
    fun export(
        @PathVariable id: UUID,
        @RequestParam(defaultValue = "md") format: String,
        @RequestParam(required = false) ids: String?
    ): ResponseEntity<ByteArray> {
        val userId = AuthenticatedUser.getUserId()
        val personId = PersonId(id)
        val responseIds = ids?.split(",")
            ?.mapNotNull { it.trim().takeIf { s -> s.isNotBlank() } }
            ?.map { FeedbackResponseId(UUID.fromString(it)) }
            ?.takeIf { it.isNotEmpty() }

        return when (format.lowercase()) {
            "pdf" -> {
                val bytes = feedbackExportService.exportPdf(userId, personId, responseIds)
                ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"peer-feedback.pdf\"")
                    .header(HttpHeaders.CONTENT_TYPE, "application/pdf")
                    .contentLength(bytes.size.toLong())
                    .body(bytes)
            }
            "md", "markdown" -> {
                val bytes = feedbackExportService.exportMarkdown(userId, personId, responseIds).toByteArray(Charsets.UTF_8)
                ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"peer-feedback.md\"")
                    .header(HttpHeaders.CONTENT_TYPE, "text/markdown; charset=UTF-8")
                    .contentLength(bytes.size.toLong())
                    .body(bytes)
            }
            else -> throw IllegalArgumentException("format must be 'md' or 'pdf'")
        }
    }
}
