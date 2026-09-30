package com.peoplemanager.adapters.pdf

import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class FlyingSaucerPdfRendererAdapterTest {

    private val adapter = FlyingSaucerPdfRendererAdapter()

    @Test
    fun `renders markdown to a non-empty PDF starting with the PDF magic bytes`() {
        val md = """
            # Peer Feedback: Alex

            ## Analytics

            | Period | Avg |
            |--------|-----|
            | Q1     | 4.0 |

            ## Responses

            ### Anonymous
            - **q1:** 5/5

            Great collaborator.
        """.trimIndent()

        val pdf = adapter.renderMarkdownToPdf(md, "Peer Feedback: Alex")

        pdf.size shouldBeGreaterThan 100
        // PDF files start with "%PDF"
        String(pdf.copyOfRange(0, 4), Charsets.US_ASCII) shouldBe "%PDF"
    }

    @Test
    fun `escapes special characters in the title without failing`() {
        val pdf = adapter.renderMarkdownToPdf("# Hi", "A & B <weird> \"title\"")
        String(pdf.copyOfRange(0, 4), Charsets.US_ASCII) shouldBe "%PDF"
    }

    @Test
    fun `handles empty markdown`() {
        val pdf = adapter.renderMarkdownToPdf("", null)
        String(pdf.copyOfRange(0, 4), Charsets.US_ASCII) shouldBe "%PDF"
    }
}
