package com.peoplemanager.domain

/**
 * Built-in starter feedback templates offered to every manager as a one-click
 * starting point. These are not persisted; a manager copies one into their own
 * library (which assigns a fresh id and their userId).
 */
object FeedbackStarterTemplates {

    fun forUser(userId: UserId): List<FeedbackTemplate> = listOf(
        performanceReview(userId),
        retro(userId)
    )

    private fun performanceReview(userId: UserId) = FeedbackTemplate(
        id = FeedbackTemplateId.generate(),
        userId = userId,
        title = "Performance review",
        description = "Structured peer input for a performance review.",
        questions = listOf(
            FeedbackQuestion(
                id = "q1",
                type = FeedbackQuestionType.LIKERT,
                text = "This person demonstrates reliable ownership of their responsibilities.",
                required = true
            ),
            FeedbackQuestion(
                id = "q1b",
                type = FeedbackQuestionType.TEXT,
                text = "If you rated below 'Somewhat agree', please share specifics or examples.",
                required = false,
                showIf = ShowIfRule("q1", ShowIfOperator.LTE, 3)
            ),
            FeedbackQuestion(
                id = "q2",
                type = FeedbackQuestionType.RATING,
                text = "Rate the quality of communication and collaboration.",
                required = true,
                lowLabel = "Very poor",
                highLabel = "Excellent"
            ),
            FeedbackQuestion(
                id = "q3",
                type = FeedbackQuestionType.TEXT,
                text = "What should they keep doing? What should they change?",
                required = false
            )
        )
    )

    private fun retro(userId: UserId) = FeedbackTemplate(
        id = FeedbackTemplateId.generate(),
        userId = userId,
        title = "Project retro",
        description = "Quick post-project feedback on teamwork and delivery.",
        questions = listOf(
            FeedbackQuestion(
                id = "q1",
                type = FeedbackQuestionType.RATING,
                text = "How would you rate the team's overall delivery on this project?",
                required = true,
                lowLabel = "Poor",
                highLabel = "Excellent"
            ),
            FeedbackQuestion(
                id = "q2",
                type = FeedbackQuestionType.LIKERT,
                text = "This person contributed effectively to problem-solving during the project.",
                required = true
            ),
            FeedbackQuestion(
                id = "q3",
                type = FeedbackQuestionType.TEXT,
                text = "Actionable suggestions for next time (optional).",
                required = false
            )
        )
    )
}
