package com.peoplemanager.application.port.output

import com.peoplemanager.application.queries.ExportPersonDataQuery

interface PersonExportPort {
    fun exportPersonMarkdown(query: ExportPersonDataQuery): String
}
