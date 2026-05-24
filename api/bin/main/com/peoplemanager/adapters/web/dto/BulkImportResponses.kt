package com.peoplemanager.adapters.web.dto

data class BulkImportResponse(
    val successCount: Int,
    val errorCount: Int,
    val errors: List<String>
)
