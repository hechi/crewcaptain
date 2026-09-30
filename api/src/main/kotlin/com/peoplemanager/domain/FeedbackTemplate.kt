package com.peoplemanager.domain

import java.time.Instant

/**
 * A single-level conditional rule. A question with a [ShowIfRule] is only shown
 * on the public form when the answer to [questionId] satisfies [operator] against [value].
 *
 * Only meaningful when the referenced question is RATING or LIKERT (numeric answers).
 */
data class ShowIfRule(
    val questionId: String,
    val operator: ShowIfOperator,
    val value: Int
) {
    init {
        require(questionId.isNotBlank()) { "showIf questionId must not be blank" }
        require(value in 1..5) { "showIf value must be between 1 and 5" }
    }
}

/**
 * A question within a feedback template.
 *
 * [id] is a stable string key (unique within the template) used both to store answers
 * and to reference the question from another question's [showIf] rule.
 */
data class FeedbackQuestion(
    val id: String,
    val type: FeedbackQuestionType,
    val text: String,
    val required: Boolean = false,
    /** For RATING: optional endpoint labels, e.g. ["Very poor", "Excellent"]. */
    val lowLabel: String? = null,
    val highLabel: String? = null,
    val showIf: ShowIfRule? = null
) {
    init {
        require(id.isNotBlank()) { "Question id must not be blank" }
        require(text.isNotBlank()) { "Question text must not be blank" }
        require(text.length <= 500) { "Question text must not exceed 500 characters" }
        // A question cannot reference itself.
        require(showIf == null || showIf.questionId != id) {
            "A question's showIf rule cannot reference itself"
        }
    }
}

/**
 * A manager-owned, reusable feedback survey definition. Always scoped to a single [userId].
 *
 * Templates are edited freely by the owning manager. When a feedback link is created,
 * the questions are snapshotted onto the link so that later template edits or deletion
 * never change already-collected responses.
 */
data class FeedbackTemplate(
    val id: FeedbackTemplateId,
    val userId: UserId,
    val title: String,
    val description: String? = null,
    val questions: List<FeedbackQuestion> = emptyList(),
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
) {
    init {
        require(title.isNotBlank()) { "Template title must not be blank" }
        require(title.length <= 200) { "Template title must not exceed 200 characters" }
        require(questions.size <= MAX_QUESTIONS) { "A template must not exceed $MAX_QUESTIONS questions" }

        val ids = questions.map { it.id }
        require(ids.size == ids.toSet().size) { "Question ids must be unique within a template" }

        // Every showIf must reference an earlier question of a numeric type.
        questions.forEachIndexed { index, q ->
            val rule = q.showIf ?: return@forEachIndexed
            val referencedIndex = questions.indexOfFirst { it.id == rule.questionId }
            require(referencedIndex >= 0) {
                "showIf references unknown question '${rule.questionId}'"
            }
            require(referencedIndex < index) {
                "showIf must reference a question that appears earlier (single-level branching)"
            }
            val referenced = questions[referencedIndex]
            require(referenced.type == FeedbackQuestionType.RATING || referenced.type == FeedbackQuestionType.LIKERT) {
                "showIf can only reference a RATING or LIKERT question"
            }
            // No chained branching: the referenced question must itself be unconditional.
            require(referenced.showIf == null) {
                "showIf may not reference a conditional question (single-level only)"
            }
        }
    }

    fun update(
        title: String,
        description: String?,
        questions: List<FeedbackQuestion>
    ): FeedbackTemplate = copy(
        title = title,
        description = description,
        questions = questions,
        updatedAt = Instant.now()
    )

    companion object {
        const val MAX_QUESTIONS = 20
    }
}
