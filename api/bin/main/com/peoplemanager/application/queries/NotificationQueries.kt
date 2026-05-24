package com.peoplemanager.application.queries

import com.peoplemanager.domain.UserId
import org.springframework.data.domain.Pageable

data class GetNotificationsQuery(
    val userId: UserId,
    val unreadOnly: Boolean = false,
    val pageable: Pageable
)

data class GetUnreadCountQuery(
    val userId: UserId
)
