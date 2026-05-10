package com.peoplemanager.adapters.persistence

import com.peoplemanager.application.ports.EncryptionPort
import com.peoplemanager.application.ports.QuickNoteRepository
import com.peoplemanager.domain.*
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Transactional
class JpaQuickNoteRepositoryAdapter(
    private val springDataRepository: SpringDataQuickNoteRepository,
    private val encryptionPort: EncryptionPort
) : QuickNoteRepository {

    private val logger = LoggerFactory.getLogger(JpaQuickNoteRepositoryAdapter::class.java)

    override fun save(quickNote: QuickNote): QuickNote {
        val entity = quickNote.toEntity()
        return springDataRepository.save(entity).toDomain()
    }

    override fun findByIdAndUserId(quickNoteId: QuickNoteId, userId: UserId): QuickNote? {
        return springDataRepository.findByIdAndUserId(quickNoteId.value, userId.value)?.toDomain()
    }

    override fun findAllByUserId(userId: UserId, pageable: Pageable): Page<QuickNote> {
        return springDataRepository.findAllByUserId(userId.value, pageable).map { it.toDomain() }
    }

    override fun findAllByUserIdAndStatus(userId: UserId, status: QuickNoteStatus, pageable: Pageable): Page<QuickNote> {
        return springDataRepository.findAllByUserIdAndStatus(userId.value, status.name, pageable).map { it.toDomain() }
    }

    override fun findAllByUserIdAndPersonId(userId: UserId, personId: PersonId, pageable: Pageable): Page<QuickNote> {
        return springDataRepository.findAllByUserIdAndPersonId(userId.value, personId.value, pageable).map { it.toDomain() }
    }

    override fun findAllByUserIdAndStatusAndPersonId(
        userId: UserId,
        status: QuickNoteStatus,
        personId: PersonId,
        pageable: Pageable
    ): Page<QuickNote> {
        return springDataRepository.findAllByUserIdAndStatusAndPersonId(
            userId.value, status.name, personId.value, pageable
        ).map { it.toDomain() }
    }

    override fun deleteByIdAndUserId(quickNoteId: QuickNoteId, userId: UserId): Boolean {
        val deleted = springDataRepository.deleteByIdAndUserId(quickNoteId.value, userId.value)
        return deleted > 0
    }

    private fun QuickNoteEntity.toDomain(): QuickNote {
        val decryptedText = if (this.sensitive) {
            try {
                encryptionPort.decrypt(this.text) ?: this.text
            } catch (e: Exception) {
                logger.error("Failed to decrypt quick note ${this.id}: ${e.javaClass.simpleName}: ${e.message}")
                "[encrypted content - unable to decrypt]"
            }
        } else {
            this.text
        }

        return QuickNote(
            id = QuickNoteId(this.id),
            userId = UserId(this.userId),
            personId = this.personId?.let { PersonId(it) },
            text = decryptedText,
            sensitive = this.sensitive,
            status = QuickNoteStatus.valueOf(this.status),
            attachedEntryId = this.attachedEntryId?.let { OneOnOneEntryId(it) },
            createdAt = this.createdAt,
            updatedAt = this.updatedAt
        )
    }

    private fun QuickNote.toEntity(): QuickNoteEntity = QuickNoteEntity(
        id = this.id.value,
        userId = this.userId.value,
        personId = this.personId?.value,
        text = if (this.sensitive) encryptionPort.encrypt(this.text) ?: this.text else this.text,
        sensitive = this.sensitive,
        status = this.status.name,
        attachedEntryId = this.attachedEntryId?.value,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )
}
