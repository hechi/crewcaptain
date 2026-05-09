package com.peoplemanager.application.queries

import com.peoplemanager.domain.ActionItemId
import com.peoplemanager.domain.ActionItemStatus
import com.peoplemanager.domain.PersonId
import com.peoplemanager.domain.UserId

data class GetActionItemQuery(
    val userId: UserId,
    val personId: PersonId,
    val actionItemId: ActionItemId
)

data class ListActionItemsByPersonQuery(
    val userId: UserId,
    val personId: PersonId,
    val status: ActionItemStatus? = null,
    val page: Int = 0,
    val size: Int = 20
)

data class ListAllActionItemsQuery(
    val userId: UserId,
    val status: ActionItemStatus? = null,
    val overdueOnly: Boolean = false,
    val page: Int = 0,
    val size: Int = 20
)

data class CountOpenActionItemsQuery(
    val userId: UserId,
    val personId: PersonId
)
