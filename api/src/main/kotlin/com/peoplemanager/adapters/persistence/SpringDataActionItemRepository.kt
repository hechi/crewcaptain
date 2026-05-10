package com.peoplemanager.adapters.persistence

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate
import java.util.UUID

interface SpringDataActionItemRepository : JpaRepository<ActionItemEntity, UUID> {

    fun findByIdAndUserIdAndPersonId(id: UUID, userId: UUID, personId: UUID): ActionItemEntity?

    fun findAllByUserIdAndPersonId(userId: UUID, personId: UUID, pageable: Pageable): Page<ActionItemEntity>

    @Query("SELECT a FROM ActionItemEntity a WHERE a.userId = :userId AND a.personId = :personId AND a.status = :status")
    fun findAllByUserIdAndPersonIdAndStatus(
        @Param("userId") userId: UUID,
        @Param("personId") personId: UUID,
        @Param("status") status: String,
        pageable: Pageable
    ): Page<ActionItemEntity>

    fun findAllByUserId(userId: UUID, pageable: Pageable): Page<ActionItemEntity>

    @Query("SELECT a FROM ActionItemEntity a WHERE a.userId = :userId AND a.status = :status")
    fun findAllByUserIdAndStatus(
        @Param("userId") userId: UUID,
        @Param("status") status: String,
        pageable: Pageable
    ): Page<ActionItemEntity>

    @Query("SELECT a FROM ActionItemEntity a WHERE a.userId = :userId AND a.status = 'OPEN' AND a.dueDate < :referenceDate")
    fun findOverdueByUserId(
        @Param("userId") userId: UUID,
        @Param("referenceDate") referenceDate: LocalDate,
        pageable: Pageable
    ): Page<ActionItemEntity>

    fun deleteByIdAndUserIdAndPersonId(id: UUID, userId: UUID, personId: UUID): Long

    @Query("SELECT a FROM ActionItemEntity a WHERE a.userId = :userId AND a.status = 'OPEN' AND a.dueDate >= :fromDate AND a.dueDate <= :toDate ORDER BY a.dueDate ASC")
    fun findDueSoonByUserId(
        @Param("userId") userId: UUID,
        @Param("fromDate") fromDate: LocalDate,
        @Param("toDate") toDate: LocalDate
    ): List<ActionItemEntity>

    @Query("SELECT COUNT(a) FROM ActionItemEntity a WHERE a.userId = :userId AND a.personId = :personId AND a.status = 'OPEN'")
    fun countOpenByUserIdAndPersonId(
        @Param("userId") userId: UUID,
        @Param("personId") personId: UUID
    ): Long
}
