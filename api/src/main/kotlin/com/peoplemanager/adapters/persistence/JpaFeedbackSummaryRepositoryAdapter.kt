package com.peoplemanager.adapters.persistence

import com.peoplemanager.application.port.output.EncryptionPort
import com.peoplemanager.application.port.output.FeedbackSummaryRepository
import com.peoplemanager.domain.FeedbackSummary
import com.peoplemanager.domain.FeedbackSummaryId
import com.peoplemanager.domain.PersonId
import com.peoplemanager.domain.UserId
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Transactional
class JpaFeedbackSummaryRepositoryAdapter(
    private val springDataRepository: SpringDataFeedbackSummaryRepository,
    private val encryptionPort: EncryptionPort
) : FeedbackSummaryRepository {

    private val logger = LoggerFactory.getLogger(JpaFeedbackSummaryRepositoryAdapter::class.java)

    override fun save(summary: FeedbackSummary): FeedbackSummary =
        springDataRepository.save(summary.toEntity()).toDomain()

    override fun findAllByUserIdAndPersonId(userId: UserId, personId: PersonId): List<FeedbackSummary> =
        springDataRepository.findAllByUserIdAndPersonIdOrderByCreatedAtDesc(userId.value, personId.value).map { it.toDomain() }

    override fun findLatestByUserIdAndPersonId(userId: UserId, personId: PersonId): FeedbackSummary? =
        springDataRepository.findFirstByUserIdAndPersonIdOrderByCreatedAtDesc(userId.value, personId.value)?.toDomain()

    private fun FeedbackSummaryEntity.toDomain(): FeedbackSummary {
        val content = try {
            encryptionPort.decrypt(this.content) ?: this.content
        } catch (e: Exception) {
            logger.error("Failed to decrypt feedback summary ${this.id}: ${e.javaClass.simpleName}: ${e.message}")
            "[encrypted content - unable to decrypt]"
        }
        return FeedbackSummary(
            id = FeedbackSummaryId(this.id),
            userId = UserId(this.userId),
            personId = PersonId(this.personId),
            periodFrom = this.periodFrom,
            periodTo = this.periodTo,
            content = content,
            responseCount = this.responseCount,
            createdAt = this.createdAt,
            updatedAt = this.updatedAt
        )
    }

    private fun FeedbackSummary.toEntity(): FeedbackSummaryEntity = FeedbackSummaryEntity(
        id = this.id.value,
        userId = this.userId.value,
        personId = this.personId.value,
        periodFrom = this.periodFrom,
        periodTo = this.periodTo,
        content = encryptionPort.encrypt(this.content) ?: this.content,
        responseCount = this.responseCount,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )
}
