package com.peoplemanager.application.queries

import com.peoplemanager.domain.PersonId
import com.peoplemanager.domain.UserId
import java.time.LocalDate

data class GenerateReviewPacketQuery(
    val userId: UserId,
    val personId: PersonId,
    val dateFrom: LocalDate,
    val dateTo: LocalDate
) {
    init {
        require(!dateTo.isBefore(dateFrom)) {
            "dateTo must not be before dateFrom"
        }
    }
}
