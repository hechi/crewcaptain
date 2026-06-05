package com.peoplemanager.application.port.output

import com.peoplemanager.domain.UserId
import com.peoplemanager.domain.UserSettings

interface UserSettingsRepository {
    fun findByUserId(userId: UserId): UserSettings?
    fun save(settings: UserSettings): UserSettings
}
