package com.peoplemanager.adapters.persistence

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant
import java.util.UUID

interface SpringDataOneOnOneEntryRepository : JpaRepository<OneOnOneEntryEntity, UUID> {
    fun findByIdAndUserId(id: UUID, userId: UUID): OneOnOneEntryEntity?
    fun findByIdAndUserIdAndPersonId(id: UUID, userId: UUID, personId: UUID): OneOnOneEntryEntity?
    fun findAllByUserIdAndPersonId(userId: UUID, personId: UUID, pageable: Pageable): Page<OneOnOneEntryEntity>
    fun deleteByIdAndUserIdAndPersonId(id: UUID, userId: UUID, personId: UUID): Long

    @Query("SELECT MAX(e.meetingDate) FROM OneOnOneEntryEntity e WHERE e.userId = :userId AND e.personId = :personId")
    fun findLatestMeetingDate(@Param("userId") userId: UUID, @Param("personId") personId: UUID): Instant?

    fun countByUserId(userId: UUID): Long

    @Query("SELECT e.meetingDate FROM OneOnOneEntryEntity e WHERE e.userId = :userId ORDER BY e.meetingDate ASC")
    fun findAllMeetingDatesByUserId(@Param("userId") userId: UUID): List<Instant>
}
