package com.peoplemanager.domain.service

import com.peoplemanager.domain.*

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

/**
 * Domain service for parsing CSV content into CsvPersonRow instances.
 * Pure function, no framework dependencies.
 */
object CsvParser {

    data class ParsedCsv(
        val rows: List<CsvPersonRow>,
        val errors: List<String>
    )

    /**
     * Parse a CSV input stream into person rows.
     * Supports comma-separated values with optional quoting.
     * Header row is required.
     */
    fun parse(input: InputStream): ParsedCsv {
        val reader = BufferedReader(InputStreamReader(input, Charsets.UTF_8))
        val lines = reader.readLines()

        if (lines.isEmpty()) {
            return ParsedCsv(emptyList(), listOf("CSV file is empty"))
        }

        val headerLine = lines.first()
        val headers = parseCsvLine(headerLine).map { it.lowercase().trim() }

        // Validate headers
        val headerErrors = mutableListOf<String>()
        if (!headers.contains("name")) {
            headerErrors.add("Missing required header: 'name'")
        }

        val unknownHeaders = headers.filter { it.isNotBlank() && it !in CsvPersonRow.SUPPORTED_HEADERS }
        if (unknownHeaders.isNotEmpty()) {
            // Unknown headers are warnings, not errors — we just ignore them
        }

        if (headerErrors.isNotEmpty()) {
            return ParsedCsv(emptyList(), headerErrors)
        }

        val rows = mutableListOf<CsvPersonRow>()
        val errors = mutableListOf<String>()

        for ((index, line) in lines.drop(1).withIndex()) {
            val rowNumber = index + 2 // 1-indexed, header is row 1
            if (line.isBlank()) continue

            val values = parseCsvLine(line)
            val rowMap = headers.zip(values).toMap()

            when (val result = CsvPersonRow.parse(rowNumber, rowMap)) {
                is CsvParseResult.Success -> rows.add(result.row)
                is CsvParseResult.Failure -> errors.addAll(result.errors)
            }
        }

        if (rows.isEmpty() && errors.isEmpty()) {
            errors.add("CSV file contains no data rows")
        }

        return ParsedCsv(rows, errors)
    }

    /**
     * Parse a single CSV line respecting quoted fields.
     * Handles: commas inside quotes, escaped quotes (""), newlines NOT supported inside fields.
     */
    internal fun parseCsvLine(line: String): List<String> {
        val fields = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0

        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' && !inQuotes -> {
                    inQuotes = true
                }
                c == '"' && inQuotes -> {
                    // Check for escaped quote ""
                    if (i + 1 < line.length && line[i + 1] == '"') {
                        current.append('"')
                        i++ // skip next quote
                    } else {
                        inQuotes = false
                    }
                }
                c == ',' && !inQuotes -> {
                    fields.add(current.toString())
                    current.clear()
                }
                else -> {
                    current.append(c)
                }
            }
            i++
        }
        fields.add(current.toString())

        return fields
    }
}
