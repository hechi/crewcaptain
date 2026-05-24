package com.peoplemanager.adapters.persistence

import com.peoplemanager.application.ports.OneOnOneSeriesRepository
import com.peoplemanager.domain.CadenceType
import com.peoplemanager.domain.OneOnOneSeries
import com.peoplemanager.domain.OneOnOneSeriesId
import com.peoplemanager.domain.PersonId
import com.peoplemanager.domain.UserId
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Transactional
class JpaOneOnOneSeriesRepositoryAdapter(
    private val springDataRepository: SpringDataOneOnOneSeriesRepository
) : OneOnOneSeriesRepository {

    override fun findByUserIdAndPersonId(userId: UserId, personId: PersonId): OneOnOneSeries? {
        return springDataRepository.findByUserIdAndPersonId(userId.value, personId.value)?.toDomain()
    }

    override fun findAllByUserId(userId: UserId): List<OneOnOneSeries> {
        return springDataRepository.findAllByUserId(userId.value).map { it.toDomain() }
    }

    override fun save(series: OneOnOneSeries): OneOnOneSeries {
        val entity = series.toEntity()
        return springDataRepository.save(entity).toDomain()
    }

    private fun OneOnOneSeriesEntity.toDomain(): OneOnOneSeries = OneOnOneSeries(
        id = OneOnOneSeriesId(this.id),
        userId = UserId(this.userId),
        personId = PersonId(this.personId),
        cadenceType = CadenceType.valueOf(this.cadenceType),
        customIntervalDays = this.customIntervalDays,
        templateMarkdown = this.templateMarkdown,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )

    private fun OneOnOneSeries.toEntity(): OneOnOneSeriesEntity = OneOnOneSeriesEntity(
        id = this.id.value,
        userId = this.userId.value,
        personId = this.personId.value,
        cadenceType = this.cadenceType.name,
        customIntervalDays = this.customIntervalDays,
        templateMarkdown = this.templateMarkdown,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )
}
