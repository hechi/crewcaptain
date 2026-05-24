package com.peoplemanager.application.ports

import com.peoplemanager.application.queries.ExportPersonDataQuery

interface PersonExportPort {
    fun exportPersonMarkdown(query: ExportPersonDataQuery): String
}
