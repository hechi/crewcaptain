package com.peoplemanager.adapters.persistence

import com.peoplemanager.application.ports.EncryptionPort
import com.peoplemanager.application.ports.OneOnOneEntryRepository
import com.peoplemanager.domain.AgendaItem
import com.peoplemanager.domain.AgendaItemId
import com.peoplemanager.domain.OneOnOneEntry
import com.peoplemanager.domain.OneOnOneEntryId
import com.peoplemanager.domain.PersonId
import com.peoplemanager.domain.UserId
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Repository
@Transactional
class JpaOneOnOneEntryRepositoryAdapter(
    private val springDataRepository: SpringDataOneOnOneEntryRepository,
    private val encryptionPort: EncryptionPort
) : OneOnOneEntryRepository {

    private val logger = LoggerFactory.getLogger(JpaOneOnOneEntryRepositoryAdapter::class.java)

    override fun save(entry: OneOnOneEntry): OneOnOneEntry {
        val entity = entry.toEntity()
        return springDataRepository.save(entity).toDomain()
    }

    override fun findByIdAndUserId(
        entryId: OneOnOneEntryId,
        userId: UserId
    ): OneOnOneEntry? {
        return springDataRepository.findByIdAndUserId(entryId.value, userId.value)?.toDomain()
    }

    override fun findByIdAndUserIdAndPersonId(
        entryId: OneOnOneEntryId,
        userId: UserId,
        personId: PersonId
    ): OneOnOneEntry? {
        return springDataRepository.findByIdAndUserIdAndPersonId(
            entryId.value, userId.value, personId.value
        )?.toDomain()
    }

    override fun findAllByUserIdAndPersonId(
        userId: UserId,
        personId: PersonId,
        pageable: Pageable
    ): Page<OneOnOneEntry> {
        return springDataRepository.findAllByUserIdAndPersonId(
            userId.value, personId.value, pageable
        ).map { it.toDomain() }
    }

    override fun deleteByIdAndUserIdAndPersonId(
        entryId: OneOnOneEntryId,
        userId: UserId,
        personId: PersonId
    ): Boolean {
        val deleted = springDataRepository.deleteByIdAndUserIdAndPersonId(
            entryId.value, userId.value, personId.value
        )
        return deleted > 0
    }

    override fun findLatestMeetingDate(userId: UserId, personId: PersonId): Instant? {
        return springDataRepository.findLatestMeetingDate(userId.value, personId.value)
    }

    private fun OneOnOneEntryEntity.toDomain(): OneOnOneEntry {
        val decryptedNotes = if (this.sensitive) {
            try {
                encryptionPort.decrypt(this.notesMarkdown)
            } catch (e: Exception) {
                logger.error("Failed to decrypt notes for entry ${this.id}: ${e.javaClass.simpleName}: ${e.message}")
                "[encrypted content - unable to decrypt]"
            }
        } else {
            this.notesMarkdown
        }

        val decryptedOutcomes = if (this.sensitive) {
            try {
                encryptionPort.decrypt(this.outcomesMarkdown)
            } catch (e: Exception) {
                logger.error("Failed to decrypt outcomes for entry ${this.id}: ${e.javaClass.simpleName}: ${e.message}")
                "[encrypted content - unable to decrypt]"
            }
        } else {
            this.outcomesMarkdown
        }

        return OneOnOneEntry(
            id = OneOnOneEntryId(this.id),
            userId = UserId(this.userId),
            personId = PersonId(this.personId),
            meetingDate = this.meetingDate,
            agendaItems = this.agendaItems.map { it.toDomain() },
            notesMarkdown = decryptedNotes,
            outcomesMarkdown = decryptedOutcomes,
            sensitive = this.sensitive,
            createdAt = this.createdAt,
            updatedAt = this.updatedAt
        )
    }

    private fun AgendaItemEntity.toDomain(): AgendaItem = AgendaItem(
        id = AgendaItemId(this.id),
        text = this.text,
        checked = this.checked,
        displayOrder = this.displayOrder,
        createdAt = this.createdAt
    )

    private fun OneOnOneEntry.toEntity(): OneOnOneEntryEntity {
        val entryEntity = OneOnOneEntryEntity(
            id = this.id.value,
            userId = this.userId.value,
            personId = this.personId.value,
            meetingDate = this.meetingDate,
            notesMarkdown = if (this.sensitive) encryptionPort.encrypt(this.notesMarkdown) else this.notesMarkdown,
            outcomesMarkdown = if (this.sensitive) encryptionPort.encrypt(this.outcomesMarkdown) else this.outcomesMarkdown,
            sensitive = this.sensitive,
            createdAt = this.createdAt,
            updatedAt = this.updatedAt
        )
        entryEntity.agendaItems = this.agendaItems.map { item ->
            AgendaItemEntity(
                id = item.id.value,
                entry = entryEntity,
                text = item.text,
                checked = item.checked,
                displayOrder = item.displayOrder,
                createdAt = item.createdAt
            )
        }.toMutableList()
        return entryEntity
    }
}
