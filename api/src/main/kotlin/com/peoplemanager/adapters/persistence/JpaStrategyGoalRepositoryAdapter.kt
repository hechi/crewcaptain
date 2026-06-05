package com.peoplemanager.adapters.persistence

import com.peoplemanager.application.port.output.EncryptionPort
import com.peoplemanager.application.port.output.StrategyGoalRepository
import com.peoplemanager.domain.StrategyGoal
import com.peoplemanager.domain.StrategyGoalId
import com.peoplemanager.domain.StrategyGoalStatus
import com.peoplemanager.domain.UserId
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Transactional
class JpaStrategyGoalRepositoryAdapter(
    private val springDataRepository: SpringDataStrategyGoalRepository,
    private val encryptionPort: EncryptionPort
) : StrategyGoalRepository {

    private val logger = LoggerFactory.getLogger(JpaStrategyGoalRepositoryAdapter::class.java)

    override fun save(strategyGoal: StrategyGoal): StrategyGoal {
        val entity = strategyGoal.toEntity()
        return springDataRepository.save(entity).toDomain()
    }

    override fun findByIdAndUserId(strategyGoalId: StrategyGoalId, userId: UserId): StrategyGoal? {
        return springDataRepository.findByIdAndUserId(strategyGoalId.value, userId.value)?.toDomain()
    }

    override fun findAllByUserId(
        userId: UserId,
        status: StrategyGoalStatus?,
        pageable: Pageable
    ): Page<StrategyGoal> {
        return if (status != null) {
            springDataRepository.findAllByUserIdAndStatus(userId.value, status.name, pageable).map { it.toDomain() }
        } else {
            springDataRepository.findAllByUserId(userId.value, pageable).map { it.toDomain() }
        }
    }

    override fun deleteByIdAndUserId(strategyGoalId: StrategyGoalId, userId: UserId): Boolean {
        val deleted = springDataRepository.deleteByIdAndUserId(strategyGoalId.value, userId.value)
        return deleted > 0
    }

    override fun countActiveByUserId(userId: UserId): Long {
        return springDataRepository.countActiveByUserId(userId.value)
    }

    override fun countByUserIdAndStatus(userId: UserId, status: StrategyGoalStatus): Long {
        return springDataRepository.countByUserIdAndStatus(userId.value, status.name)
    }

    private fun StrategyGoalEntity.toDomain(): StrategyGoal {
        val decryptedTitle = if (this.sensitive) {
            try {
                encryptionPort.decrypt(this.title) ?: this.title
            } catch (e: Exception) {
                logger.error("Failed to decrypt strategy goal title ${this.id}: ${e.javaClass.simpleName}: ${e.message}")
                "[encrypted content - unable to decrypt]"
            }
        } else {
            this.title
        }

        val decryptedDescription = if (this.sensitive && this.description != null) {
            try {
                encryptionPort.decrypt(this.description) ?: this.description
            } catch (e: Exception) {
                logger.error("Failed to decrypt strategy goal description ${this.id}: ${e.javaClass.simpleName}: ${e.message}")
                "[encrypted content - unable to decrypt]"
            }
        } else {
            this.description
        }

        return StrategyGoal(
            id = StrategyGoalId(this.id),
            userId = UserId(this.userId),
            title = decryptedTitle,
            description = decryptedDescription,
            targetDate = this.targetDate,
            status = StrategyGoalStatus.valueOf(this.status),
            sensitive = this.sensitive,
            createdAt = this.createdAt,
            updatedAt = this.updatedAt
        )
    }

    private fun StrategyGoal.toEntity(): StrategyGoalEntity = StrategyGoalEntity(
        id = this.id.value,
        userId = this.userId.value,
        title = if (this.sensitive) encryptionPort.encrypt(this.title) ?: this.title else this.title,
        description = if (this.sensitive && this.description != null) {
            encryptionPort.encrypt(this.description) ?: this.description
        } else {
            this.description
        },
        targetDate = this.targetDate,
        status = this.status.name,
        sensitive = this.sensitive,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )
}
