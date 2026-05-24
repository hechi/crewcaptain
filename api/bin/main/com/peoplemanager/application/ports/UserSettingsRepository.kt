package com.peoplemanager.application.ports

import com.peoplemanager.domain.UserId
import com.peoplemanager.domain.UserSettings

interface UserSettingsRepository {
    fun findByUserId(userId: UserId): UserSettings?
    fun save(settings: UserSettings): UserSettings
}
