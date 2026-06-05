package com.peoplemanager.application.port.output

import com.peoplemanager.domain.PdpGoal
import com.peoplemanager.domain.PdpGoalId
import com.peoplemanager.domain.PdpGoalStatus
import com.peoplemanager.domain.PersonId
import com.peoplemanager.domain.UserId
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface PdpGoalRepository {
    fun save(goal: PdpGoal): PdpGoal
    fun findByIdAndUserIdAndPersonId(goalId: PdpGoalId, userId: UserId, personId: PersonId): PdpGoal?
    fun findAllByUserIdAndPersonId(userId: UserId, personId: PersonId, status: PdpGoalStatus?, pageable: Pageable): Page<PdpGoal>
    fun deleteByIdAndUserIdAndPersonId(goalId: PdpGoalId, userId: UserId, personId: PersonId): Boolean
    fun countActiveByUserIdAndPersonId(userId: UserId, personId: PersonId): Long
    fun countByUserIdAndStatus(userId: UserId, status: PdpGoalStatus): Long
}
