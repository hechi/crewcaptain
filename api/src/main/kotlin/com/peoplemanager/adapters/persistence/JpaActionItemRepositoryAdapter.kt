package com.peoplemanager.adapters.persistence

import com.peoplemanager.application.port.output.ActionItemRepository
import com.peoplemanager.domain.ActionItem
import com.peoplemanager.domain.ActionItemId
import com.peoplemanager.domain.ActionItemOwnerType
import com.peoplemanager.domain.ActionItemStatus
import com.peoplemanager.domain.OneOnOneEntryId
import com.peoplemanager.domain.PersonId
import com.peoplemanager.domain.UserId
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Repository
@Transactional
class JpaActionItemRepositoryAdapter(
    private val springDataRepository: SpringDataActionItemRepository
) : ActionItemRepository {

    override fun save(actionItem: ActionItem): ActionItem {
        val entity = actionItem.toEntity()
        return springDataRepository.save(entity).toDomain()
    }

    override fun findByIdAndUserIdAndPersonId(
        actionItemId: ActionItemId,
        userId: UserId,
        personId: PersonId
    ): ActionItem? {
        return springDataRepository.findByIdAndUserIdAndPersonId(
            actionItemId.value, userId.value, personId.value
        )?.toDomain()
    }

    override fun findAllByUserIdAndPersonId(
        userId: UserId,
        personId: PersonId,
        status: ActionItemStatus?,
        pageable: Pageable
    ): Page<ActionItem> {
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

    override fun findAllByUserIdAndPersonIdAndOriginatingEntryId(
        userId: UserId,
        personId: PersonId,
        originatingEntryId: OneOnOneEntryId,
        pageable: Pageable
    ): Page<ActionItem> {
        return springDataRepository.findAllByUserIdAndPersonIdAndOriginatingEntryId(
            userId.value, personId.value, originatingEntryId.value, pageable
        ).map { it.toDomain() }
    }

    override fun findAllByUserId(
        userId: UserId,
        status: ActionItemStatus?,
        pageable: Pageable
    ): Page<ActionItem> {
        return if (status != null) {
            springDataRepository.findAllByUserIdAndStatus(
                userId.value, status.name, pageable
            ).map { it.toDomain() }
        } else {
            springDataRepository.findAllByUserId(userId.value, pageable).map { it.toDomain() }
        }
    }

    override fun findOverdueByUserId(
        userId: UserId,
        referenceDate: LocalDate,
        pageable: Pageable
    ): Page<ActionItem> {
        return springDataRepository.findOverdueByUserId(
            userId.value, referenceDate, pageable
        ).map { it.toDomain() }
    }

    override fun findDueSoonByUserId(
        userId: UserId,
        fromDate: LocalDate,
        toDate: LocalDate
    ): List<ActionItem> {
        return springDataRepository.findDueSoonByUserId(
            userId.value, fromDate, toDate
        ).map { it.toDomain() }
    }

    override fun deleteByIdAndUserIdAndPersonId(
        actionItemId: ActionItemId,
        userId: UserId,
        personId: PersonId
    ): Boolean {
        val deleted = springDataRepository.deleteByIdAndUserIdAndPersonId(
            actionItemId.value, userId.value, personId.value
        )
        return deleted > 0
    }

    override fun countOpenByUserIdAndPersonId(userId: UserId, personId: PersonId): Long {
        return springDataRepository.countOpenByUserIdAndPersonId(userId.value, personId.value)
    }

    override fun countByUserIdAndStatus(userId: UserId, status: ActionItemStatus): Long {
        return springDataRepository.countByUserIdAndStatus(userId.value, status.name)
    }

    private fun ActionItemEntity.toDomain(): ActionItem = ActionItem(
        id = ActionItemId(this.id),
        userId = UserId(this.userId),
        personId = PersonId(this.personId),
        title = this.title,
        description = this.description,
        ownerType = ActionItemOwnerType.valueOf(this.ownerType),
        dueDate = this.dueDate,
        status = ActionItemStatus.valueOf(this.status),
        originatingEntryId = this.originatingEntryId?.let { OneOnOneEntryId(it) },
        snoozedUntil = this.snoozedUntil,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )

    private fun ActionItem.toEntity(): ActionItemEntity = ActionItemEntity(
        id = this.id.value,
        userId = this.userId.value,
        personId = this.personId.value,
        title = this.title,
        description = this.description,
        ownerType = this.ownerType.name,
        dueDate = this.dueDate,
        status = this.status.name,
        originatingEntryId = this.originatingEntryId?.value,
        snoozedUntil = this.snoozedUntil,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )
}
