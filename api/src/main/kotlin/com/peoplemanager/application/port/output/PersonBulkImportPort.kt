package com.peoplemanager.application.port.output

import com.peoplemanager.application.commands.BulkImportPersonsCommand
import com.peoplemanager.domain.PersonId

data class BulkImportResult(
    val successCount: Int,
    val errorCount: Int,
    val createdPersonIds: List<PersonId>,
    val errors: List<String>
)

interface PersonBulkImportPort {
    fun importPersonsFromCsv(command: BulkImportPersonsCommand): BulkImportResult
}
