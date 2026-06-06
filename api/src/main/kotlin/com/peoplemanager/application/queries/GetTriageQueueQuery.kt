package com.peoplemanager.application.queries

import com.peoplemanager.domain.PersonId
import com.peoplemanager.domain.TriageItemType
import com.peoplemanager.domain.UserId
import com.peoplemanager.domain.WorkspaceId

data class GetTriageQueueQuery(
    val userId: UserId,
    val itemType: TriageItemType? = null,
    val workspaceIds: List<WorkspaceId>? = null,
    val personId: PersonId? = null,
    val ownerScope: OwnerScope = OwnerScope.ALL
)

enum class OwnerScope {
    ALL,
    MINE
}
