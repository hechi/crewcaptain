package com.peoplemanager.domain

import java.time.Instant
import java.time.LocalDate

/**
 * Read-only projection for the manager dashboard.
 * Aggregates data from multiple domain entities.
 */
data class DashboardData(
    val overdueActionItems: List<DashboardActionItem>,
    val dueSoonActionItems: List<DashboardActionItem>,
    val staleOneOnOnes: List<StaleOneOnOneReminder>,
    val upcomingAnniversaries: List<UpcomingAnniversary>
)

data class DashboardActionItem(
    val id: ActionItemId,
    val personId: PersonId,
    val personName: String,
    val title: String,
    val dueDate: LocalDate,
    val ownerType: ActionItemOwnerType
)

data class StaleOneOnOneReminder(
    val personId: PersonId,
    val personName: String,
    val cadenceType: CadenceType,
    val customIntervalDays: Int?,
    val lastMeetingDate: Instant?,
    val daysSinceLastMeeting: Long,
    val expectedIntervalDays: Int
)

data class UpcomingAnniversary(
    val personId: PersonId,
    val personName: String,
    val startDate: LocalDate,
    val anniversaryDate: LocalDate,
    val yearsCompleted: Int,
    val daysUntil: Long
)
