package com.peoplemanager.adapters.persistence

import com.peoplemanager.application.port.output.PdpGoalRepository
import com.peoplemanager.domain.PdpGoal
import com.peoplemanager.domain.PdpGoalId
import com.peoplemanager.domain.PdpGoalStatus
import com.peoplemanager.domain.PersonId
import com.peoplemanager.domain.UserId
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Transactional
class JpaPdpGoalRepositoryAdapter(
    private val springDataRepository: SpringDataPdpGoalRepository
) : PdpGoalRepository {

    override fun save(goal: PdpGoal): PdpGoal {
        val entity = goal.toEntity()
        return springDataRepository.save(entity).toDomain()
    }

    override fun findByIdAndUserIdAndPersonId(
        goalId: PdpGoalId,
        userId: UserId,
        personId: PersonId
    ): PdpGoal? {
        return springDataRepository.findByIdAndUserIdAndPersonId(
            goalId.value, userId.value, personId.value
        )?.toDomain()
    }

    override fun findAllByUserIdAndPersonId(
        userId: UserId,
        personId: PersonId,
        status: PdpGoalStatus?,
        pageable: Pageable
    ): Page<PdpGoal> {
        return if (status != null) {
            springDataRepository.findAllByUserIdAndPersonIdAndStatus(
                userId.value, personId.value, status.name, pageable
            ).map { it.toDomain() }
        } else {
            springDataRepository.findAllByUserIdAndPersonId(
                userId.value, personId.value, pageable
            ).map { it.toDomain() }
        }
    }

    override fun deleteByIdAndUserIdAndPersonId(
        goalId: PdpGoalId,
        userId: UserId,
        personId: PersonId
    ): Boolean {
        val deleted = springDataRepository.deleteByIdAndUserIdAndPersonId(
            goalId.value, userId.value, personId.value
        )
        return deleted > 0
    }

    override fun countActiveByUserIdAndPersonId(userId: UserId, personId: PersonId): Long {
        return springDataRepository.countActiveByUserIdAndPersonId(userId.value, personId.value)
    }

    override fun countByUserIdAndStatus(userId: UserId, status: PdpGoalStatus): Long {
        return springDataRepository.countByUserIdAndStatus(userId.value, status.name)
    }

    private fun PdpGoalEntity.toDomain(): PdpGoal = PdpGoal(
        id = PdpGoalId(this.id),
        userId = UserId(this.userId),
        personId = PersonId(this.personId),
        title = this.title,
        description = this.description,
        targetDate = this.targetDate,
        status = PdpGoalStatus.valueOf(this.status),
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )

    private fun PdpGoal.toEntity(): PdpGoalEntity = PdpGoalEntity(
        id = this.id.value,
        userId = this.userId.value,
        personId = this.personId.value,
        title = this.title,
        description = this.description,
        targetDate = this.targetDate,
        status = this.status.name,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )
}
