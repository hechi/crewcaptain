package com.peoplemanager.application.port.input

import com.peoplemanager.application.commands.MarkNotificationReadCommand
import com.peoplemanager.application.commands.MarkAllNotificationsReadCommand
import com.peoplemanager.domain.Notification

interface NotificationCommandPort {
    fun markAsRead(command: MarkNotificationReadCommand): Notification
    fun markAllAsRead(command: MarkAllNotificationsReadCommand): Int
}
