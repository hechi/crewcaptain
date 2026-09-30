package com.peoplemanager.adapters.persistence

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.peoplemanager.domain.FeedbackQuestion
import com.peoplemanager.domain.FeedbackQuestionType
import com.peoplemanager.domain.ShowIfOperator
import com.peoplemanager.domain.ShowIfRule
import tools.jackson.core.type.TypeReference
import tools.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component

/**
 * Serializes/deserializes the [FeedbackQuestion] snapshot to/from the JSON TEXT column
 * used by both feedback templates and feedback links.
 *
 * Uses an explicit persistence DTO so the on-disk shape is stable and independent of the
 * domain class layout.
 */
@Component
class FeedbackQuestionsCodec(
    private val objectMapper: ObjectMapper
) {

    fun encode(questions: List<FeedbackQuestion>): String {
        val dtos = questions.map { it.toDto() }
        return objectMapper.writeValueAsString(dtos)
    }

    fun decode(json: String?): List<FeedbackQuestion> {
        if (json.isNullOrBlank() || json == "[]") return emptyList()
        val dtos: List<QuestionDto> = objectMapper.readValue(json, object : TypeReference<List<QuestionDto>>() {})
        return dtos.map { it.toDomain() }
    }

    private fun FeedbackQuestion.toDto() = QuestionDto(
        id = id,
        type = type.name,
        text = text,
        required = required,
        lowLabel = lowLabel,
        highLabel = highLabel,
        showIf = showIf?.let { ShowIfDto(it.questionId, it.operator.name, it.value) }
    )

    private fun QuestionDto.toDomain() = FeedbackQuestion(
        id = id,
        type = FeedbackQuestionType.valueOf(type),
        text = text,
        required = required,
        lowLabel = lowLabel,
        highLabel = highLabel,
        showIf = showIf?.let { ShowIfRule(it.questionId, ShowIfOperator.valueOf(it.operator), it.value) }
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class QuestionDto(
        val id: String = "",
        val type: String = "TEXT",
        val text: String = "",
        val required: Boolean = false,
        val lowLabel: String? = null,
        val highLabel: String? = null,
        val showIf: ShowIfDto? = null
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class ShowIfDto(
        val questionId: String = "",
        val operator: String = "LTE",
        val value: Int = 1
    )
}
