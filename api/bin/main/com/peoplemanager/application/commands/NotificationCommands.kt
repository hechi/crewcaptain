package com.peoplemanager.application.commands

import com.peoplemanager.domain.NotificationId
import com.peoplemanager.domain.UserId

data class MarkNotificationReadCommand(
    val userId: UserId,
    val notificationId: NotificationId
)

data class MarkAllNotificationsReadCommand(
    val userId: UserId
)
