package com.peoplemanager.adapters.web.dto

import com.peoplemanager.domain.FeedbackQuestion
import com.peoplemanager.domain.FeedbackQuestionType
import com.peoplemanager.domain.FeedbackTemplate
import com.peoplemanager.domain.ShowIfOperator
import com.peoplemanager.domain.ShowIfRule
import jakarta.validation.constraints.NotBlank
import java.time.Instant
import java.util.UUID

// ===== Shared question DTOs =====

data class ShowIfDto(
    val questionId: String,
    val operator: String,
    val value: Int
) {
    fun toDomain(): ShowIfRule =
        ShowIfRule(questionId, ShowIfOperator.valueOf(operator.uppercase()), value)

    companion object {
        fun from(rule: ShowIfRule): ShowIfDto =
            ShowIfDto(rule.questionId, rule.operator.name, rule.value)
    }
}

data class FeedbackQuestionDto(
    val id: String,
    val type: String,
    val text: String,
    val required: Boolean = false,
    val lowLabel: String? = null,
    val highLabel: String? = null,
    val showIf: ShowIfDto? = null
) {
    fun toDomain(): FeedbackQuestion = FeedbackQuestion(
        id = id,
        type = FeedbackQuestionType.valueOf(type.uppercase()),
        text = text,
        required = required,
        lowLabel = lowLabel,
        highLabel = highLabel,
        showIf = showIf?.toDomain()
    )

    companion object {
        fun from(q: FeedbackQuestion): FeedbackQuestionDto = FeedbackQuestionDto(
            id = q.id,
            type = q.type.name,
            text = q.text,
            required = q.required,
            lowLabel = q.lowLabel,
            highLabel = q.highLabel,
            showIf = q.showIf?.let { ShowIfDto.from(it) }
        )
    }
}

// ===== Requests =====

data class SaveFeedbackTemplateRequest(
    @field:NotBlank(message = "Title must not be blank")
    val title: String?,
    val description: String? = null,
    val questions: List<FeedbackQuestionDto> = emptyList()
)

data class GenerateFeedbackTemplateRequest(
    @field:NotBlank(message = "Brief must not be blank")
    val brief: String?
)

// ===== Responses =====

data class FeedbackTemplateResponse(
    val id: UUID,
    val title: String,
    val description: String?,
    val questions: List<FeedbackQuestionDto>,
    val createdAt: Instant?,
    val updatedAt: Instant?
) {
    companion object {
        fun from(t: FeedbackTemplate): FeedbackTemplateResponse = FeedbackTemplateResponse(
            id = t.id.value,
            title = t.title,
            description = t.description,
            questions = t.questions.map { FeedbackQuestionDto.from(it) },
            createdAt = t.createdAt,
            updatedAt = t.updatedAt
        )
    }
}

/** A generated draft (or starter) template that is not persisted yet. */
data class FeedbackTemplateDraftResponse(
    val title: String,
    val description: String?,
    val questions: List<FeedbackQuestionDto>
) {
    companion object {
        fun from(t: FeedbackTemplate): FeedbackTemplateDraftResponse = FeedbackTemplateDraftResponse(
            title = t.title,
            description = t.description,
            questions = t.questions.map { FeedbackQuestionDto.from(it) }
        )
    }
}
