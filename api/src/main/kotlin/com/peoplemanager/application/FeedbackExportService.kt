package com.peoplemanager.application

import com.peoplemanager.application.port.output.FeedbackResponseRepository
import com.peoplemanager.application.port.output.PdfRendererPort
import com.peoplemanager.application.port.output.PersonRepository
import com.peoplemanager.domain.FeedbackResponseId
import com.peoplemanager.domain.PersonId
import com.peoplemanager.domain.UserId
import com.peoplemanager.domain.service.FeedbackMarkdownFormatter
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Exports a person's feedback responses as Markdown or PDF.
 * By default exports approved, non-flagged responses; an explicit id list overrides that.
 * Always scoped to the owning manager.
 */
@Service
@Transactional(readOnly = true)
class FeedbackExportService(
    private val personRepository: PersonRepository,
    private val feedbackResponseRepository: FeedbackResponseRepository,
    private val pdfRendererPort: PdfRendererPort
) {

    fun exportMarkdown(userId: UserId, personId: PersonId, ids: List<FeedbackResponseId>?): String {
        val (person, responses) = load(userId, personId, ids)
        return FeedbackMarkdownFormatter.format(person, responses)
    }

    fun exportPdf(userId: UserId, personId: PersonId, ids: List<FeedbackResponseId>?): ByteArray {
        val (person, responses) = load(userId, personId, ids)
        val markdown = FeedbackMarkdownFormatter.format(person, responses)
        return pdfRendererPort.renderMarkdownToPdf(markdown, "Peer Feedback: ${person.name}")
    }

    private fun load(
        userId: UserId,
        personId: PersonId,
        ids: List<FeedbackResponseId>?
    ): Pair<com.peoplemanager.domain.Person, List<com.peoplemanager.domain.FeedbackResponse>> {
        val person = personRepository.findByIdAndUserId(personId, userId)
            ?: throw PersonNotFoundException(personId)

        val all = feedbackResponseRepository.findAllByUserIdAndPersonId(userId, personId)
        val selected = if (ids != null && ids.isNotEmpty()) {
            val idSet = ids.toSet()
            all.filter { it.id in idSet }
        } else {
            // Default: usable responses only (approved and not flagged).
            all.filter { it.isUsable }
        }
        return person to selected
    }
}
