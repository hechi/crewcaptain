package com.peoplemanager.adapters.persistence

import com.peoplemanager.application.ports.PdpUpdateRepository
import com.peoplemanager.domain.PdpGoalId
import com.peoplemanager.domain.PdpUpdate
import com.peoplemanager.domain.PdpUpdateId
import com.peoplemanager.domain.UserId
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Transactional
class JpaPdpUpdateRepositoryAdapter(
    private val springDataRepository: SpringDataPdpUpdateRepository
) : PdpUpdateRepository {

    override fun save(update: PdpUpdate): PdpUpdate {
        val entity = update.toEntity()
        return springDataRepository.save(entity).toDomain()
    }

    override fun findAllByGoalIdAndUserId(
        goalId: PdpGoalId,
        userId: UserId,
        pageable: Pageable
    ): Page<PdpUpdate> {
        return springDataRepository.findAllByGoalIdAndUserId(
            goalId.value, userId.value, pageable
        ).map { it.toDomain() }
    }

    override fun deleteByIdAndGoalIdAndUserId(
        updateId: PdpUpdateId,
        goalId: PdpGoalId,
        userId: UserId
    ): Boolean {
        val deleted = springDataRepository.deleteByIdAndGoalIdAndUserId(
            updateId.value, goalId.value, userId.value
        )
        return deleted > 0
    }

    private fun PdpUpdateEntity.toDomain(): PdpUpdate = PdpUpdate(
        id = PdpUpdateId(this.id),
        goalId = PdpGoalId(this.goalId),
        userId = UserId(this.userId),
        textMarkdown = this.textMarkdown,
        sensitive = this.sensitive,
        createdAt = this.createdAt
    )

    private fun PdpUpdate.toEntity(): PdpUpdateEntity = PdpUpdateEntity(
        id = this.id.value,
        goalId = this.goalId.value,
        userId = this.userId.value,
        textMarkdown = this.textMarkdown,
        sensitive = this.sensitive,
        createdAt = this.createdAt
    )
}
