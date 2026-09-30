package com.peoplemanager.adapters.persistence

import com.peoplemanager.application.port.output.FeedbackTemplateRepository
import com.peoplemanager.domain.FeedbackTemplate
import com.peoplemanager.domain.FeedbackTemplateId
import com.peoplemanager.domain.UserId
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Transactional
class JpaFeedbackTemplateRepositoryAdapter(
    private val springDataRepository: SpringDataFeedbackTemplateRepository,
    private val questionsCodec: FeedbackQuestionsCodec
) : FeedbackTemplateRepository {

    override fun save(template: FeedbackTemplate): FeedbackTemplate {
        return springDataRepository.save(template.toEntity()).toDomain()
    }

    override fun findByIdAndUserId(id: FeedbackTemplateId, userId: UserId): FeedbackTemplate? {
        return springDataRepository.findByIdAndUserId(id.value, userId.value)?.toDomain()
    }

    override fun findAllByUserId(userId: UserId): List<FeedbackTemplate> {
        return springDataRepository.findAllByUserIdOrderByUpdatedAtDesc(userId.value).map { it.toDomain() }
    }

    override fun deleteByIdAndUserId(id: FeedbackTemplateId, userId: UserId): Boolean {
        return springDataRepository.deleteByIdAndUserId(id.value, userId.value) > 0
    }

    private fun FeedbackTemplateEntity.toDomain(): FeedbackTemplate = FeedbackTemplate(
        id = FeedbackTemplateId(this.id),
        userId = UserId(this.userId),
        title = this.title,
        description = this.description,
        questions = questionsCodec.decode(this.questions),
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )

    private fun FeedbackTemplate.toEntity(): FeedbackTemplateEntity = FeedbackTemplateEntity(
        id = this.id.value,
        userId = this.userId.value,
        title = this.title,
        description = this.description,
        questions = questionsCodec.encode(this.questions),
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )
}
