package com.peoplemanager.application.port.output

/**
 * Renders a Markdown document to PDF bytes. Implementations live in the adapters layer
 * (keeping PDF/HTML libraries out of the domain, per the hexagonal boundary).
 */
interface PdfRendererPort {
    /**
     * Renders the given Markdown into a PDF document.
     * @param markdown the source Markdown
     * @param title an optional document title (used for PDF metadata / page header)
     * @return the PDF file as a byte array
     */
    fun renderMarkdownToPdf(markdown: String, title: String? = null): ByteArray
}
