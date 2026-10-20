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
    val upcomingAnniversaries: List<UpcomingAnniversary>,
    val todayPriorities: List<TodayPriorityItem> = emptyList()
)

/**
 * The kind of item surfaced in the "Today's Priorities" focus list, in priority order.
 */
enum class PriorityKind {
    OVERDUE,
    DUE_SOON,
    INBOX_NOTE
}

/**
 * A single ranked entry in the dashboard "Today's Priorities" widget. Unifies overdue and
 * due-soon action items with self-assigned INBOX quick notes. For INBOX_NOTE entries there is
 * no person and no due date; [id] is the quick note id, otherwise it is the action item id.
 */
data class TodayPriorityItem(
    val kind: PriorityKind,
    val id: java.util.UUID,
    val title: String,
    val personId: PersonId?,
    val personName: String?,
    val dueDate: LocalDate?
)

data class DashboardActionItem(
    val id: ActionItemId,
    val personId: PersonId?,
    val personName: String?,
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
