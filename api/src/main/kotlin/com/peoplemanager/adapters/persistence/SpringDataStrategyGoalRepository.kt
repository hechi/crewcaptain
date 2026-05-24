package com.peoplemanager.adapters.persistence

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface SpringDataStrategyGoalRepository : JpaRepository<StrategyGoalEntity, UUID> {

    fun findByIdAndUserId(id: UUID, userId: UUID): StrategyGoalEntity?

    fun findAllByUserId(userId: UUID, pageable: Pageable): Page<StrategyGoalEntity>

    @Query("SELECT sg FROM StrategyGoalEntity sg WHERE sg.userId = :userId AND sg.status = :status")
    fun findAllByUserIdAndStatus(
        @Param("userId") userId: UUID,
        @Param("status") status: String,
        pageable: Pageable
    ): Page<StrategyGoalEntity>

    fun deleteByIdAndUserId(id: UUID, userId: UUID): Long

    @Query("SELECT COUNT(sg) FROM StrategyGoalEntity sg WHERE sg.userId = :userId AND sg.status = 'ACTIVE'")
    fun countActiveByUserId(@Param("userId") userId: UUID): Long

    @Query("SELECT COUNT(sg) FROM StrategyGoalEntity sg WHERE sg.userId = :userId AND sg.status = :status")
    fun countByUserIdAndStatus(
        @Param("userId") userId: UUID,
        @Param("status") status: String
    ): Long
}
