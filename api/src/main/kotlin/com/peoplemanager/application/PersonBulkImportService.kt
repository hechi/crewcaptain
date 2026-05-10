package com.peoplemanager.application

import com.peoplemanager.application.commands.BulkImportPersonsCommand
import com.peoplemanager.application.ports.BulkImportResult
import com.peoplemanager.application.ports.PersonBulkImportPort
import com.peoplemanager.application.ports.PersonRepository
import com.peoplemanager.domain.CsvParser
import com.peoplemanager.domain.CsvPersonRow
import com.peoplemanager.domain.MoraleStatus
import com.peoplemanager.domain.Person
import com.peoplemanager.domain.PersonId
import org.springframework.stereotype.Service

@Service
class PersonBulkImportService(
    private val personRepository: PersonRepository
) : PersonBulkImportPort {

    companion object {
        const val MAX_ROWS = 500
    }

    override fun importPersonsFromCsv(command: BulkImportPersonsCommand): BulkImportResult {
        val parsed = CsvParser.parse(command.csvInputStream)

        if (parsed.errors.isNotEmpty() && parsed.rows.isEmpty()) {
            return BulkImportResult(
                successCount = 0,
                errorCount = parsed.errors.size,
                createdPersonIds = emptyList(),
                errors = parsed.errors
            )
        }

        if (parsed.rows.size > MAX_ROWS) {
            return BulkImportResult(
                successCount = 0,
                errorCount = 1,
                createdPersonIds = emptyList(),
                errors = listOf("CSV contains ${parsed.rows.size} rows, maximum allowed is $MAX_ROWS")
            )
        }

        val createdIds = mutableListOf<PersonId>()
        val errors = parsed.errors.toMutableList()

        for (row in parsed.rows) {
            try {
                val person = createPersonFromRow(command, row)
                val saved = personRepository.save(person)
                createdIds.add(saved.id)
            } catch (e: Exception) {
                errors.add("Row ${row.rowNumber}: Failed to create person '${row.name}' - ${e.message}")
            }
        }

        return BulkImportResult(
            successCount = createdIds.size,
            errorCount = errors.size,
            createdPersonIds = createdIds,
            errors = errors
        )
    }

    private fun createPersonFromRow(command: BulkImportPersonsCommand, row: CsvPersonRow): Person {
        return Person(
            id = PersonId.generate(),
            userId = command.userId,
            name = row.name,
            preferredName = row.preferredName,
            roleTitle = row.roleTitle,
            timezone = row.timezone,
            startDate = row.startDate,
            email = row.email,
            tags = row.tags,
            moraleStatus = MoraleStatus.UNKNOWN,
            moraleNote = null,
            pinnedRememberItems = emptyList()
        )
    }
}
