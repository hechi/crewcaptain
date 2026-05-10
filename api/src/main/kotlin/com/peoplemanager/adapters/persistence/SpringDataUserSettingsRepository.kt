package com.peoplemanager.adapters.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface SpringDataUserSettingsRepository : JpaRepository<UserSettingsEntity, UUID>
