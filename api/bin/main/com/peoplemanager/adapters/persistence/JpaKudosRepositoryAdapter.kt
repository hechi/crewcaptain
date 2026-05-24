package com.peoplemanager.adapters.persistence

import com.peoplemanager.application.ports.KudosRepository
import com.peoplemanager.domain.Kudos
import com.peoplemanager.domain.KudosId
import com.peoplemanager.domain.PersonId
import com.peoplemanager.domain.UserId
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Transactional
class JpaKudosRepositoryAdapter(
    private val springDataRepository: SpringDataKudosRepository
) : KudosRepository {

    override fun save(kudos: Kudos): Kudos {
        val entity = kudos.toEntity()
        return springDataRepository.save(entity).toDomain()
    }

    override fun findByIdAndUserIdAndPersonId(
        kudosId: KudosId,
        userId: UserId,
        personId: PersonId
    ): Kudos? {
        return springDataRepository.findByIdAndUserIdAndPersonId(
            kudosId.value, userId.value, personId.value
        )?.toDomain()
    }

    override fun findAllByUserIdAndPersonId(
        userId: UserId,
        personId: PersonId,
        pageable: Pageable
    ): Page<Kudos> {
        return springDataRepository.findAllByUserIdAndPersonId(
            userId.value, personId.value, pageable
        ).map { it.toDomain() }
    }

    override fun findAllByUserId(userId: UserId, pageable: Pageable): Page<Kudos> {
        return springDataRepository.findAllByUserId(userId.value, pageable).map { it.toDomain() }
    }

    override fun deleteByIdAndUserIdAndPersonId(
        kudosId: KudosId,
        userId: UserId,
        personId: PersonId
    ): Boolean {
        val deleted = springDataRepository.deleteByIdAndUserIdAndPersonId(
            kudosId.value, userId.value, personId.value
        )
        return deleted > 0
    }

    override fun countByUserId(userId: UserId): Long {
        return springDataRepository.countByUserId(userId.value)
    }

    private fun KudosEntity.toDomain(): Kudos = Kudos(
        id = KudosId(this.id),
        userId = UserId(this.userId),
        personId = PersonId(this.personId),
        date = this.date,
        text = this.text,
        tags = this.tags.toList(),
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )

    private fun Kudos.toEntity(): KudosEntity = KudosEntity(
        id = this.id.value,
        userId = this.userId.value,
        personId = this.personId.value,
        date = this.date,
        text = this.text,
        tags = this.tags.toTypedArray(),
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )
}
