package com.peoplemanager.application.commands

import com.peoplemanager.domain.UserId
import java.io.InputStream

data class BulkImportPersonsCommand(
    val userId: UserId,
    val csvInputStream: InputStream
)
