package com.peoplemanager.adapters.persistence

import com.peoplemanager.application.port.output.FeedbackLinkRepository
import com.peoplemanager.domain.FeedbackLink
import com.peoplemanager.domain.FeedbackLinkId
import com.peoplemanager.domain.FeedbackTemplateId
import com.peoplemanager.domain.PersonId
import com.peoplemanager.domain.UserId
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Transactional
class JpaFeedbackLinkRepositoryAdapter(
    private val springDataRepository: SpringDataFeedbackLinkRepository,
    private val questionsCodec: FeedbackQuestionsCodec
) : FeedbackLinkRepository {

    override fun save(link: FeedbackLink): FeedbackLink =
        springDataRepository.save(link.toEntity()).toDomain()

    override fun findByIdAndUserId(id: FeedbackLinkId, userId: UserId): FeedbackLink? =
        springDataRepository.findByIdAndUserId(id.value, userId.value)?.toDomain()

    override fun findAllByUserIdAndPersonId(userId: UserId, personId: PersonId): List<FeedbackLink> =
        springDataRepository.findAllByUserIdAndPersonIdOrderByCreatedAtDesc(userId.value, personId.value).map { it.toDomain() }

    override fun findByToken(token: String): FeedbackLink? =
        springDataRepository.findByToken(token)?.toDomain()

    override fun existsByToken(token: String): Boolean =
        springDataRepository.existsByToken(token)

    private fun FeedbackLinkEntity.toDomain(): FeedbackLink = FeedbackLink(
        id = FeedbackLinkId(this.id),
        userId = UserId(this.userId),
        personId = PersonId(this.personId),
        token = this.token,
        title = this.title,
        description = this.description,
        questions = questionsCodec.decode(this.questions),
        label = this.label,
        requestSubmitterInfo = this.requestSubmitterInfo,
        expiresAt = this.expiresAt,
        revokedAt = this.revokedAt,
        sourceTemplateId = this.sourceTemplateId?.let { FeedbackTemplateId(it) },
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )

    private fun FeedbackLink.toEntity(): FeedbackLinkEntity = FeedbackLinkEntity(
        id = this.id.value,
        userId = this.userId.value,
        personId = this.personId.value,
        token = this.token,
        title = this.title,
        description = this.description,
        questions = questionsCodec.encode(this.questions),
        label = this.label,
        requestSubmitterInfo = this.requestSubmitterInfo,
        expiresAt = this.expiresAt,
        revokedAt = this.revokedAt,
        sourceTemplateId = this.sourceTemplateId?.value,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )
}
