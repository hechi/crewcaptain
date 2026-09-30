package com.peoplemanager.domain

import java.time.Instant
import java.time.LocalDate

/**
 * A saved, manager-editable AI narrative summarizing feedback for a person over a period.
 * The most recent summary for a person is included in their Review Packet.
 *
 * Always scoped to the owning manager ([userId]).
 */
data class FeedbackSummary(
    val id: FeedbackSummaryId,
    val userId: UserId,
    val personId: PersonId,
    val periodFrom: LocalDate,
    val periodTo: LocalDate,
    val content: String,
    /** Number of responses the summary was generated from. */
    val responseCount: Int,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
) {
    init {
        require(content.isNotBlank()) { "Summary content must not be blank" }
        require(!periodTo.isBefore(periodFrom)) { "periodTo must not be before periodFrom" }
        require(responseCount >= 0) { "responseCount must not be negative" }
    }

    fun editContent(newContent: String): FeedbackSummary {
        require(newContent.isNotBlank()) { "Summary content must not be blank" }
        return copy(content = newContent, updatedAt = Instant.now())
    }
}
