package com.peoplemanager.adapters.persistence

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface SpringDataPdpUpdateRepository : JpaRepository<PdpUpdateEntity, UUID> {

    fun findAllByGoalIdAndUserId(goalId: UUID, userId: UUID, pageable: Pageable): Page<PdpUpdateEntity>

    fun deleteByIdAndGoalIdAndUserId(id: UUID, goalId: UUID, userId: UUID): Long
}
