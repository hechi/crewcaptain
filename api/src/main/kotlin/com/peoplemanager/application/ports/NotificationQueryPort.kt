package com.peoplemanager.application.ports

import com.peoplemanager.application.queries.GetNotificationsQuery
import com.peoplemanager.application.queries.GetUnreadCountQuery
import com.peoplemanager.domain.Notification
import org.springframework.data.domain.Page

interface NotificationQueryPort {
    fun getNotifications(query: GetNotificationsQuery): Page<Notification>
    fun getUnreadCount(query: GetUnreadCountQuery): Long
}
