package com.peoplemanager.adapters.persistence

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface SpringDataPdpGoalRepository : JpaRepository<PdpGoalEntity, UUID> {

    fun findByIdAndUserIdAndPersonId(id: UUID, userId: UUID, personId: UUID): PdpGoalEntity?

    fun findAllByUserIdAndPersonId(userId: UUID, personId: UUID, pageable: Pageable): Page<PdpGoalEntity>

    @Query("SELECT g FROM PdpGoalEntity g WHERE g.userId = :userId AND g.personId = :personId AND g.status = :status")
    fun findAllByUserIdAndPersonIdAndStatus(
        @Param("userId") userId: UUID,
        @Param("personId") personId: UUID,
        @Param("status") status: String,
        pageable: Pageable
    ): Page<PdpGoalEntity>

    fun deleteByIdAndUserIdAndPersonId(id: UUID, userId: UUID, personId: UUID): Long

    @Query("SELECT COUNT(g) FROM PdpGoalEntity g WHERE g.userId = :userId AND g.personId = :personId AND g.status = 'ACTIVE'")
    fun countActiveByUserIdAndPersonId(
        @Param("userId") userId: UUID,
        @Param("personId") personId: UUID
    ): Long

    @Query("SELECT COUNT(g) FROM PdpGoalEntity g WHERE g.userId = :userId AND g.status = :status")
    fun countByUserIdAndStatus(
        @Param("userId") userId: UUID,
        @Param("status") status: String
    ): Long
}
