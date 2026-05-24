package com.peoplemanager.adapters.web

import com.peoplemanager.adapters.auth.AuthenticatedUser
import com.peoplemanager.adapters.web.dto.BulkImportResponse
import com.peoplemanager.application.commands.BulkImportPersonsCommand
import com.peoplemanager.application.ports.PersonBulkImportPort
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/v1/persons")
class PersonBulkImportController(
    private val personBulkImportPort: PersonBulkImportPort
) {

    @PostMapping("/import", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun importPersons(@RequestParam("file") file: MultipartFile): ResponseEntity<BulkImportResponse> {
        val userId = AuthenticatedUser.getUserId()

        if (file.isEmpty) {
            return ResponseEntity.badRequest().body(
                BulkImportResponse(
                    successCount = 0,
                    errorCount = 1,
                    errors = listOf("Uploaded file is empty")
                )
            )
        }

        val contentType = file.contentType ?: ""
        if (contentType.isNotBlank() && contentType != "text/csv" && !contentType.contains("csv")) {
            return ResponseEntity.badRequest().body(
                BulkImportResponse(
                    successCount = 0,
                    errorCount = 1,
                    errors = listOf("File must be a CSV (received: $contentType)")
                )
            )
        }

        val command = BulkImportPersonsCommand(
            userId = userId,
            csvInputStream = file.inputStream
        )

        val result = personBulkImportPort.importPersonsFromCsv(command)

        return ResponseEntity.ok(
            BulkImportResponse(
                successCount = result.successCount,
                errorCount = result.errorCount,
                errors = result.errors
            )
        )
    }
}
