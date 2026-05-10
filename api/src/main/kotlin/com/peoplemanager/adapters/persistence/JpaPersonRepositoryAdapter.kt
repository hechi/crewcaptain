package com.peoplemanager.adapters.persistence

import com.peoplemanager.application.ports.PersonRepository
import com.peoplemanager.domain.*
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Repository
@Transactional
class JpaPersonRepositoryAdapter(
    private val springDataPersonRepository: SpringDataPersonRepository
) : PersonRepository {

    override fun save(person: Person): Person {
        val entity = person.toEntity()
        return springDataPersonRepository.save(entity).toDomain()
    }

    override fun findByIdAndUserId(personId: PersonId, userId: UserId): Person? {
        return springDataPersonRepository.findByIdAndUserId(personId.value, userId.value)?.toDomain()
    }

    override fun findAllByUserId(
        userId: UserId,
        pageable: Pageable,
        tagFilter: String?,
        moraleFilter: MoraleStatus?
    ): Page<Person> {
        val page = when {
            tagFilter != null && moraleFilter != null ->
                springDataPersonRepository.findAllByUserIdAndTagAndMoraleStatus(
                    userId.value, tagFilter, moraleFilter.name, pageable
                )
            tagFilter != null ->
                springDataPersonRepository.findAllByUserIdAndTag(userId.value, tagFilter, pageable)
            moraleFilter != null ->
                springDataPersonRepository.findAllByUserIdAndMoraleStatus(
                    userId.value, moraleFilter.name, pageable
                )
            else ->
                springDataPersonRepository.findAllByUserId(userId.value, pageable)
        }
        return page.map { it.toDomain() }
    }

    override fun findAllByUserIdUnpaged(userId: UserId): List<Person> {
        return springDataPersonRepository.findAllByUserId(userId.value).map { it.toDomain() }
    }

    override fun deleteByIdAndUserId(personId: PersonId, userId: UserId): Boolean {
        val deleted = springDataPersonRepository.deleteByIdAndUserId(personId.value, userId.value)
        return deleted > 0
    }

    override fun softDeleteByIdAndUserId(personId: PersonId, userId: UserId): Boolean {
        val updated = springDataPersonRepository.softDeleteByIdAndUserId(personId.value, userId.value, Instant.now())
        return updated > 0
    }

    override fun restoreByIdAndUserId(personId: PersonId, userId: UserId): Boolean {
        val updated = springDataPersonRepository.restoreByIdAndUserId(personId.value, userId.value, Instant.now())
        return updated > 0
    }

    override fun findDeletedByIdAndUserId(personId: PersonId, userId: UserId): Person? {
        return springDataPersonRepository.findDeletedByIdAndUserId(personId.value, userId.value)?.toDomain()
    }

    override fun findAllDeletedByUserId(userId: UserId, pageable: Pageable): Page<Person> {
        return springDataPersonRepository.findAllDeletedByUserId(userId.value, pageable).map { it.toDomain() }
    }

    private fun PersonEntity.toDomain(): Person = Person(
        id = PersonId(this.id),
        userId = UserId(this.userId),
        name = this.name,
        preferredName = this.preferredName,
        roleTitle = this.roleTitle,
        timezone = this.timezone,
        startDate = this.startDate,
        email = this.email,
        tags = this.tags.toList(),
        moraleStatus = MoraleStatus.valueOf(this.moraleStatus),
        moraleNote = this.moraleNote,
        pinnedRememberItems = this.pinnedRememberItems.map { it.toDomain() },
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        deletedAt = this.deletedAt
    )

    private fun PinnedRememberItemEntity.toDomain(): PinnedRememberItem = PinnedRememberItem(
        id = RememberItemId(this.id),
        text = this.text,
        displayOrder = this.displayOrder,
        createdAt = this.createdAt
    )

    private fun Person.toEntity(): PersonEntity {
        val personEntity = PersonEntity(
            id = this.id.value,
            userId = this.userId.value,
            name = this.name,
            preferredName = this.preferredName,
            roleTitle = this.roleTitle,
            timezone = this.timezone,
            startDate = this.startDate,
            email = this.email,
            tags = this.tags.toTypedArray(),
            moraleStatus = this.moraleStatus.name,
            moraleNote = this.moraleNote,
            createdAt = this.createdAt,
            updatedAt = this.updatedAt,
            deletedAt = this.deletedAt
        )
        personEntity.pinnedRememberItems = this.pinnedRememberItems.map { item ->
            PinnedRememberItemEntity(
                id = item.id.value,
                person = personEntity,
                text = item.text,
                displayOrder = item.displayOrder,
                createdAt = item.createdAt
            )
        }.toMutableList()
        return personEntity
    }
}
