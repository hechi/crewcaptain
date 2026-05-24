package com.peoplemanager.domain

import java.time.Instant

data class OneOnOneSeries(
    val id: OneOnOneSeriesId,
    val userId: UserId,
    val personId: PersonId,
    val cadenceType: CadenceType,
    val customIntervalDays: Int? = null,
    val templateMarkdown: String? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
) {
    init {
        if (cadenceType == CadenceType.CUSTOM) {
            require(customIntervalDays != null && customIntervalDays > 0) {
                "Custom cadence requires a positive interval in days"
            }
        }
    }
}
