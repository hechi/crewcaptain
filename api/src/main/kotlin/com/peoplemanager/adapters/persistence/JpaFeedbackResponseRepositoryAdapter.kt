package com.peoplemanager.adapters.persistence

import com.peoplemanager.application.port.output.EncryptionPort
import com.peoplemanager.application.port.output.FeedbackResponseRepository
import com.peoplemanager.domain.FeedbackConversionType
import com.peoplemanager.domain.FeedbackLinkId
import com.peoplemanager.domain.FeedbackResponse
import com.peoplemanager.domain.FeedbackResponseId
import com.peoplemanager.domain.FeedbackResponseStatus
import com.peoplemanager.domain.PersonId
import com.peoplemanager.domain.UserId
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * Persistence adapter for feedback responses. Free-text content (the answers JSON blob and
 * the additional comments) is always encrypted at rest — third-party opinion about an
 * employee is inherently sensitive. Decryption failures degrade gracefully.
 */
@Repository
@Transactional
class JpaFeedbackResponseRepositoryAdapter(
    private val springDataRepository: SpringDataFeedbackResponseRepository,
    private val answersCodec: FeedbackAnswersCodec,
    private val encryptionPort: EncryptionPort
) : FeedbackResponseRepository {

    private val logger = LoggerFactory.getLogger(JpaFeedbackResponseRepositoryAdapter::class.java)

    override fun save(response: FeedbackResponse): FeedbackResponse =
        springDataRepository.save(response.toEntity()).toDomain()

    override fun findByIdAndUserId(id: FeedbackResponseId, userId: UserId): FeedbackResponse? =
        springDataRepository.findByIdAndUserId(id.value, userId.value)?.toDomain()

    override fun findAllByUserIdAndPersonId(userId: UserId, personId: PersonId): List<FeedbackResponse> =
        springDataRepository.findAllByUserIdAndPersonIdOrderByCreatedAtDesc(userId.value, personId.value).map { it.toDomain() }

    override fun findAllByUserIdAndPersonIdAndStatus(userId: UserId, personId: PersonId, status: FeedbackResponseStatus): List<FeedbackResponse> =
        springDataRepository.findAllByUserIdAndPersonIdAndStatusOrderByCreatedAtDesc(userId.value, personId.value, status.name).map { it.toDomain() }

    override fun deleteByIdAndUserId(id: FeedbackResponseId, userId: UserId): Boolean =
        springDataRepository.deleteByIdAndUserId(id.value, userId.value) > 0

    override fun countByLinkId(linkId: FeedbackLinkId): Long =
        springDataRepository.countByLinkId(linkId.value)

    override fun findLatestCreatedAtByLinkId(linkId: FeedbackLinkId): Instant? =
        springDataRepository.findLatestCreatedAtByLinkId(linkId.value)

    private fun FeedbackResponseEntity.toDomain(): FeedbackResponse {
        val answersJson = decryptSafely(this.answers, this.id.toString()) ?: "[]"
        val comments = this.additionalComments?.let { decryptSafely(it, this.id.toString()) }
        return FeedbackResponse(
            id = FeedbackResponseId(this.id),
            userId = UserId(this.userId),
            personId = PersonId(this.personId),
            linkId = FeedbackLinkId(this.linkId),
            submitterName = this.submitterName,
            submitterEmail = this.submitterEmail,
            anonymous = this.anonymous,
            answers = answersCodec.decode(answersJson),
            additionalComments = comments,
            status = FeedbackResponseStatus.valueOf(this.status),
            flagged = this.flagged,
            pinned = this.pinned,
            convertedToType = this.convertedToType?.let { FeedbackConversionType.valueOf(it) },
            convertedToId = this.convertedToId,
            createdAt = this.createdAt,
            updatedAt = this.updatedAt
        )
    }

    private fun FeedbackResponse.toEntity(): FeedbackResponseEntity {
        val answersJson = answersCodec.encode(this.answers)
        return FeedbackResponseEntity(
            id = this.id.value,
            userId = this.userId.value,
            personId = this.personId.value,
            linkId = this.linkId.value,
            submitterName = this.submitterName,
            submitterEmail = this.submitterEmail,
            anonymous = this.anonymous,
            answers = encryptionPort.encrypt(answersJson) ?: answersJson,
            additionalComments = this.additionalComments?.let { encryptionPort.encrypt(it) ?: it },
            status = this.status.name,
            flagged = this.flagged,
            pinned = this.pinned,
            convertedToType = this.convertedToType?.name,
            convertedToId = this.convertedToId,
            createdAt = this.createdAt,
            updatedAt = this.updatedAt
        )
    }

    private fun decryptSafely(value: String, id: String): String? = try {
        encryptionPort.decrypt(value) ?: value
    } catch (e: Exception) {
        logger.error("Failed to decrypt feedback response $id: ${e.javaClass.simpleName}: ${e.message}")
        "[encrypted content - unable to decrypt]"
    }
}
