package com.peoplemanager.application.ports

import com.peoplemanager.domain.PdpGoalId
import com.peoplemanager.domain.PdpUpdate
import com.peoplemanager.domain.PdpUpdateId
import com.peoplemanager.domain.UserId
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface PdpUpdateRepository {
    fun save(update: PdpUpdate): PdpUpdate
    fun findAllByGoalIdAndUserId(goalId: PdpGoalId, userId: UserId, pageable: Pageable): Page<PdpUpdate>
    fun deleteByIdAndGoalIdAndUserId(updateId: PdpUpdateId, goalId: PdpGoalId, userId: UserId): Boolean
}
