package com.peoplemanager.application.queries

import com.peoplemanager.domain.PersonId
import com.peoplemanager.domain.UserId
import java.time.LocalDate

data class ExportPersonDataQuery(
    val userId: UserId,
    val personId: PersonId,
    val dateFrom: LocalDate? = null,
    val dateTo: LocalDate? = null
)
