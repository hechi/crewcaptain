package com.peoplemanager.adapters.persistence

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.peoplemanager.domain.FeedbackAnswer
import tools.jackson.core.type.TypeReference
import tools.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component

/** Serializes the answer list to/from the JSON column (which is then encrypted at rest). */
@Component
class FeedbackAnswersCodec(
    private val objectMapper: ObjectMapper
) {

    fun encode(answers: List<FeedbackAnswer>): String {
        val dtos = answers.map { AnswerDto(it.questionId, it.ratingValue, it.textValue) }
        return objectMapper.writeValueAsString(dtos)
    }

    fun decode(json: String?): List<FeedbackAnswer> {
        if (json.isNullOrBlank() || json == "[]") return emptyList()
        val dtos: List<AnswerDto> = objectMapper.readValue(json, object : TypeReference<List<AnswerDto>>() {})
        return dtos.map { FeedbackAnswer(it.questionId, it.ratingValue, it.textValue) }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class AnswerDto(
        val questionId: String = "",
        val ratingValue: Int? = null,
        val textValue: String? = null
    )
}
