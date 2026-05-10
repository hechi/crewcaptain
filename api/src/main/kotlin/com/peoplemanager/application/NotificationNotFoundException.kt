package com.peoplemanager.application

import com.peoplemanager.domain.NotificationId

class NotificationNotFoundException(notificationId: NotificationId) :
    RuntimeException("Notification not found: ${notificationId.value}")
