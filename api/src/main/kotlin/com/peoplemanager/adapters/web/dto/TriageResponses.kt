package com.peoplemanager.adapters.web.dto

import com.peoplemanager.domain.TriageCriticality
import com.peoplemanager.domain.TriageItem
import com.peoplemanager.domain.TriageItemType
import com.peoplemanager.domain.ActionItemOwnerType
import java.time.Instant

data class TriageItemResponse(
    val id: String,
    val type: TriageItemType,
    val criticality: TriageCriticality,
    val title: String,
    val personId: String,
    val personName: String,
    val workspaceId: String?,
    val workspaceName: String?,
    val sensitive: Boolean,
    val dueDate: String?,
    val daysOverdue: Long?,
    val daysUntilDue: Long?,
    val ownerType: ActionItemOwnerType?,
    val sourceActionItemId: String?,
    val snoozedUntil: Instant?,
    val createdAt: Instant
) {
    companion object {
        fun from(item: TriageItem): TriageItemResponse = TriageItemResponse(
            id = item.id,
            type = item.type,
            criticality = item.criticality,
            title = item.title,
            personId = item.personId.value.toString(),
            personName = item.personName,
            workspaceId = item.workspaceId?.value?.toString(),
            workspaceName = item.workspaceName,
            sensitive = item.sensitive,
            dueDate = item.dueDate?.toString(),
            daysOverdue = item.daysOverdue,
            daysUntilDue = item.daysUntilDue,
            ownerType = item.ownerType,
            sourceActionItemId = item.sourceActionItemId?.value?.toString(),
            snoozedUntil = item.snoozedUntil,
            createdAt = item.createdAt
        )
    }
}

data class TriageQueueResponse(
    val items: List<TriageItemResponse>,
    val totalCount: Int
) {
    companion object {
        fun from(items: List<TriageItem>): TriageQueueResponse = TriageQueueResponse(
            items = items.map { TriageItemResponse.from(it) },
            totalCount = items.size
        )
    }
}

data class TriageHintResponse(
    val hint: String?,
    val error: String?
)

data class SnoozeActionItemRequest(
    val days: Int? = null,
    val snoozedUntil: Instant? = null
) {
    init {
        require(days != null || snoozedUntil != null) {
            "Either days or snoozedUntil must be provided"
        }
    }
}
