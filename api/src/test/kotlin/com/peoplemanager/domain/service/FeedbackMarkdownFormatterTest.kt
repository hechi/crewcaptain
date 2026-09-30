package com.peoplemanager.domain.service

import com.peoplemanager.domain.*
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.junit.jupiter.api.Test

class FeedbackMarkdownFormatterTest {

    private val userId = UserId.generate()
    private val personId = PersonId.generate()
    private val person = Person(id = personId, userId = userId, name = "Alex Doe", roleTitle = "Engineer")

    private fun response(
        anonymous: Boolean = true,
        submitterName: String? = null,
        answers: List<FeedbackAnswer> = listOf(FeedbackAnswer("q1", ratingValue = 4)),
        comments: String? = null,
        flagged: Boolean = false
    ) = FeedbackResponse(
        id = FeedbackResponseId.generate(),
        userId = userId, personId = personId, linkId = FeedbackLinkId.generate(),
        anonymous = anonymous, submitterName = submitterName,
        answers = answers, additionalComments = comments, flagged = flagged
    )

    @Test
    fun `renders header and response count`() {
        val md = FeedbackMarkdownFormatter.format(person, listOf(response(), response()))
        md shouldContain "# Peer Feedback: Alex Doe"
        md shouldContain "**Role:** Engineer"
        md shouldContain "**Responses included:** 2"
    }

    @Test
    fun `anonymous responses are labeled Anonymous`() {
        val md = FeedbackMarkdownFormatter.format(person, listOf(response(anonymous = true)))
        md shouldContain "Anonymous"
    }

    @Test
    fun `named responses show the submitter name`() {
        val md = FeedbackMarkdownFormatter.format(person, listOf(response(anonymous = false, submitterName = "Sam")))
        md shouldContain "Sam"
    }

    @Test
    fun `renders rating and text answers and comments`() {
        val md = FeedbackMarkdownFormatter.format(person, listOf(
            response(
                answers = listOf(FeedbackAnswer("q1", ratingValue = 5), FeedbackAnswer("q2", textValue = "Clear communicator")),
                comments = "Keep growing"
            )
        ))
        md shouldContain "5/5"
        md shouldContain "Clear communicator"
        md shouldContain "Keep growing"
    }

    @Test
    fun `includes analytics when provided`() {
        val analytics = FeedbackAnalytics(
            totalResponses = 3,
            overallAverageRating = 4.33,
            periods = listOf(RatingPeriod("2026-Q1", 4.0, 2)),
            topThemes = listOf(ThemeCount("communication", 2))
        )
        val md = FeedbackMarkdownFormatter.format(person, listOf(response()), analytics = analytics)
        md shouldContain "## Analytics"
        md shouldContain "4.33"
        md shouldContain "communication (2)"
    }

    @Test
    fun `includes AI summary when provided`() {
        val md = FeedbackMarkdownFormatter.format(person, listOf(response()), aiSummary = "Alex is a strong collaborator.")
        md shouldContain "## Summary"
        md shouldContain "strong collaborator"
    }

    @Test
    fun `handles empty response list gracefully`() {
        val md = FeedbackMarkdownFormatter.format(person, emptyList())
        md shouldContain "No feedback responses to display"
    }
}
