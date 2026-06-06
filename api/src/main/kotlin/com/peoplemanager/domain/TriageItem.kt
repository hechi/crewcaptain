package com.peoplemanager.domain

import java.time.Instant
import java.time.LocalDate

/**
 * Unified triage item representing an actionable signal from various data sources.
 * This is a read-only projection used by the Triage Queue.
 */
data class TriageItem(
    val id: String,
    val type: TriageItemType,
    val criticality: TriageCriticality,
    val title: String,
    val personId: PersonId,
    val personName: String,
    val workspaceId: WorkspaceId? = null,
    val workspaceName: String? = null,
    val sensitive: Boolean = false,
    val dueDate: LocalDate? = null,
    val daysOverdue: Long? = null,
    val daysUntilDue: Long? = null,
    val ownerType: ActionItemOwnerType? = null,
    val sourceActionItemId: ActionItemId? = null,
    val snoozedUntil: Instant? = null,
    val createdAt: Instant = Instant.now()
) {
    val isSnoozed: Boolean
        get() = snoozedUntil != null && snoozedUntil.isAfter(Instant.now())
}

enum class TriageItemType {
    ACTION_ITEM_OVERDUE,
    ACTION_ITEM_DUE_SOON,
    STALE_ONE_ON_ONE,
    UPCOMING_ANNIVERSARY
}

enum class TriageCriticality(val sortOrder: Int) {
    OVERDUE(0),
    DUE_SOON(1),
    STALE(2),
    INFORMATIONAL(3)
}
