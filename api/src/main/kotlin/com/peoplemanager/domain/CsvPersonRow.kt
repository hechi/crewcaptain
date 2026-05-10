package com.peoplemanager.domain

import java.time.LocalDate
import java.time.format.DateTimeParseException

/**
 * Represents a single parsed row from a CSV import file.
 * Validation is performed at construction time.
 */
data class CsvPersonRow(
    val rowNumber: Int,
    val name: String,
    val preferredName: String? = null,
    val roleTitle: String? = null,
    val timezone: String? = null,
    val startDate: LocalDate? = null,
    val email: String? = null,
    val tags: List<String> = emptyList()
) {
    init {
        require(name.isNotBlank()) { "Row $rowNumber: Name must not be blank" }
    }

    companion object {
        val REQUIRED_HEADERS = setOf("name")
        val SUPPORTED_HEADERS = setOf(
            "name", "preferred_name", "role_title", "timezone",
            "start_date", "email", "tags"
        )

        /**
         * Parse a CSV row (map of header→value) into a CsvPersonRow.
         * Returns either a valid row or a list of validation errors.
         */
        fun parse(rowNumber: Int, values: Map<String, String>): CsvParseResult {
            val errors = mutableListOf<String>()

            val name = values["name"]?.trim() ?: ""
            if (name.isBlank()) {
                errors.add("Row $rowNumber: Name must not be blank")
            }

            val startDate: LocalDate? = values["start_date"]?.trim()?.takeIf { it.isNotBlank() }?.let { dateStr ->
                try {
                    LocalDate.parse(dateStr)
                } catch (e: DateTimeParseException) {
                    errors.add("Row $rowNumber: Invalid start_date format '$dateStr' (expected YYYY-MM-DD)")
                    null
                }
            }

            val tags = values["tags"]?.trim()?.takeIf { it.isNotBlank() }
                ?.split("|")
                ?.map { it.trim() }
                ?.filter { it.isNotBlank() }
                ?: emptyList()

            if (errors.isNotEmpty()) {
                return CsvParseResult.Failure(rowNumber, errors)
            }

            return CsvParseResult.Success(
                CsvPersonRow(
                    rowNumber = rowNumber,
                    name = name,
                    preferredName = values["preferred_name"]?.trim()?.takeIf { it.isNotBlank() },
                    roleTitle = values["role_title"]?.trim()?.takeIf { it.isNotBlank() },
                    timezone = values["timezone"]?.trim()?.takeIf { it.isNotBlank() },
                    startDate = startDate,
                    email = values["email"]?.trim()?.takeIf { it.isNotBlank() },
                    tags = tags
                )
            )
        }
    }
}

sealed class CsvParseResult {
    data class Success(val row: CsvPersonRow) : CsvParseResult()
    data class Failure(val rowNumber: Int, val errors: List<String>) : CsvParseResult()
}
