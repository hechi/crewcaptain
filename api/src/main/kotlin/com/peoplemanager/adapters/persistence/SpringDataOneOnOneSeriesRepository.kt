package com.peoplemanager.adapters.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface SpringDataOneOnOneSeriesRepository : JpaRepository<OneOnOneSeriesEntity, UUID> {
    fun findByUserIdAndPersonId(userId: UUID, personId: UUID): OneOnOneSeriesEntity?
    fun findAllByUserId(userId: UUID): List<OneOnOneSeriesEntity>
}
