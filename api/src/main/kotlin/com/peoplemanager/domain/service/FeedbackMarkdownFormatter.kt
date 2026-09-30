package com.peoplemanager.domain.service

import com.peoplemanager.domain.FeedbackAnswer
import com.peoplemanager.domain.FeedbackQuestionType
import com.peoplemanager.domain.FeedbackResponse
import com.peoplemanager.domain.Person
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Pure domain formatter that renders a set of feedback responses (plus optional analytics
 * and a saved AI summary) into a structured Markdown document for export.
 *
 * Framework-free: the PDF adapter renders this Markdown to PDF; the export controller
 * also serves it directly as Markdown.
 */
object FeedbackMarkdownFormatter {

    private val DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC)

    fun format(
        person: Person,
        responses: List<FeedbackResponse>,
        analytics: FeedbackAnalytics? = null,
        aiSummary: String? = null
    ): String {
        val sb = StringBuilder()
        sb.appendLine("# Peer Feedback: ${person.name}")
        person.roleTitle?.let { sb.appendLine("**Role:** $it") }
        sb.appendLine("**Generated:** ${DATE.format(java.time.Instant.now())}")
        sb.appendLine("**Responses included:** ${responses.size}")
        sb.appendLine()

        if (aiSummary != null && aiSummary.isNotBlank()) {
            sb.appendLine("## Summary")
            sb.appendLine()
            sb.appendLine(aiSummary.trim())
            sb.appendLine()
        }

        if (analytics != null) {
            appendAnalytics(sb, analytics)
        }

        sb.appendLine("## Responses")
        sb.appendLine()
        if (responses.isEmpty()) {
            sb.appendLine("*No feedback responses to display.*")
            sb.appendLine()
            return sb.toString()
        }

        responses.forEachIndexed { index, r -> appendResponse(sb, index + 1, r) }
        return sb.toString()
    }

    private fun appendAnalytics(sb: StringBuilder, analytics: FeedbackAnalytics) {
        sb.appendLine("## Analytics")
        sb.appendLine()
        val avg = analytics.overallAverageRating
        sb.appendLine("- **Overall average rating:** ${avg?.let { "%.2f".format(it) } ?: "n/a"}")
        sb.appendLine("- **Total responses:** ${analytics.totalResponses}")
        sb.appendLine()

        if (analytics.periods.isNotEmpty()) {
            sb.appendLine("**Average rating over time:**")
            sb.appendLine()
            sb.appendLine("| Period | Avg rating | Responses |")
            sb.appendLine("|--------|-----------|-----------|")
            analytics.periods.forEach { p ->
                sb.appendLine("| ${p.label} | ${"%.2f".format(p.averageRating)} | ${p.responseCount} |")
            }
            sb.appendLine()
        }

        if (analytics.topThemes.isNotEmpty()) {
            sb.appendLine("**Top themes:** " + analytics.topThemes.joinToString(", ") { "${it.theme} (${it.count})" })
            sb.appendLine()
        }
    }

    private fun appendResponse(sb: StringBuilder, index: Int, r: FeedbackResponse) {
        val who = when {
            r.anonymous -> "Anonymous"
            !r.submitterName.isNullOrBlank() -> r.submitterName
            else -> "Unnamed"
        }
        val date = DATE.format(r.createdAt)
        val flags = buildList {
            if (r.pinned) add("pinned")
            if (r.flagged) add("flagged")
        }
        val flagStr = if (flags.isEmpty()) "" else " _(${flags.joinToString(", ")})_"
        sb.appendLine("### Response $index — $who ($date)$flagStr")
        sb.appendLine()

        r.answers.forEach { a -> appendAnswer(sb, a) }

        r.additionalComments?.takeIf { it.isNotBlank() }?.let {
            sb.appendLine("**Additional comments:**")
            sb.appendLine()
            sb.appendLine(it.trim())
            sb.appendLine()
        }
        sb.appendLine("---")
        sb.appendLine()
    }

    private fun appendAnswer(sb: StringBuilder, a: FeedbackAnswer) {
        when {
            a.ratingValue != null -> sb.appendLine("- **${a.questionId}:** ${a.ratingValue}/5")
            !a.textValue.isNullOrBlank() -> {
                sb.appendLine("- **${a.questionId}:**")
                sb.appendLine("  ${a.textValue.trim().replace("\n", "\n  ")}")
            }
        }
    }

    /** Type hint helper for callers that need to know a question's type when labeling. */
    fun answerLabel(type: FeedbackQuestionType): String = when (type) {
        FeedbackQuestionType.RATING -> "Rating"
        FeedbackQuestionType.LIKERT -> "Likert"
        FeedbackQuestionType.TEXT -> "Text"
    }
}
