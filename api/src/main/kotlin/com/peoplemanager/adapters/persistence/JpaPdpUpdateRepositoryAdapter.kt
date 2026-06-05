package com.peoplemanager.adapters.persistence

import com.peoplemanager.application.port.output.EncryptionPort
import com.peoplemanager.application.port.output.PdpUpdateRepository
import com.peoplemanager.domain.PdpGoalId
import com.peoplemanager.domain.PdpUpdate
import com.peoplemanager.domain.PdpUpdateId
import com.peoplemanager.domain.UserId
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Transactional
class JpaPdpUpdateRepositoryAdapter(
    private val springDataRepository: SpringDataPdpUpdateRepository,
    private val encryptionPort: EncryptionPort
) : PdpUpdateRepository {

    private val logger = LoggerFactory.getLogger(JpaPdpUpdateRepositoryAdapter::class.java)

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

    private fun PdpUpdateEntity.toDomain(): PdpUpdate {
        val decryptedText = if (this.sensitive) {
            try {
                encryptionPort.decrypt(this.textMarkdown) ?: this.textMarkdown
            } catch (e: Exception) {
                logger.error("Failed to decrypt PDP update ${this.id}: ${e.javaClass.simpleName}: ${e.message}")
                "[encrypted content - unable to decrypt]"
            }
        } else {
            this.textMarkdown
        }

        return PdpUpdate(
            id = PdpUpdateId(this.id),
            goalId = PdpGoalId(this.goalId),
            userId = UserId(this.userId),
            textMarkdown = decryptedText,
            sensitive = this.sensitive,
            createdAt = this.createdAt
        )
    }

    private fun PdpUpdate.toEntity(): PdpUpdateEntity = PdpUpdateEntity(
        id = this.id.value,
        goalId = this.goalId.value,
        userId = this.userId.value,
        textMarkdown = if (this.sensitive) encryptionPort.encrypt(this.textMarkdown) ?: this.textMarkdown else this.textMarkdown,
        sensitive = this.sensitive,
        createdAt = this.createdAt
    )
}
