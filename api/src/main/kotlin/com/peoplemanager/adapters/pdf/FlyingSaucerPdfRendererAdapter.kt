package com.peoplemanager.adapters.pdf

import com.peoplemanager.application.port.output.PdfRendererPort
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import org.springframework.stereotype.Component
import org.xhtmlrenderer.pdf.ITextRenderer
import java.io.ByteArrayOutputStream

/**
 * Renders Markdown to PDF using CommonMark (Markdown -> HTML) and Flying Saucer
 * (XHTML+CSS -> PDF via PDFBox). Lives in the adapters layer so the PDF/HTML libraries
 * never leak into the domain (enforced by ArchUnit).
 */
@Component
class FlyingSaucerPdfRendererAdapter : PdfRendererPort {

    private val parser: Parser = Parser.builder().build()
    private val htmlRenderer: HtmlRenderer = HtmlRenderer.builder().build()

    override fun renderMarkdownToPdf(markdown: String, title: String?): ByteArray {
        val bodyHtml = htmlRenderer.render(parser.parse(markdown))
        val xhtml = wrapXhtml(bodyHtml, title)

        val renderer = ITextRenderer()
        renderer.setDocumentFromString(xhtml)
        renderer.layout()
        return ByteArrayOutputStream().use { out ->
            renderer.createPDF(out)
            out.toByteArray()
        }
    }

    /**
     * Wraps rendered HTML in a minimal, well-formed XHTML document with print CSS.
     * Flying Saucer requires valid XHTML; CommonMark's HTML output is XHTML-compatible.
     */
    private fun wrapXhtml(bodyHtml: String, title: String?): String {
        val safeTitle = (title ?: "Document").escapeXml()
        val css = """
            @page { size: A4; margin: 2cm; }
            body { font-family: sans-serif; font-size: 11pt; line-height: 1.45; color: #1a1a1a; }
            h1 { font-size: 20pt; border-bottom: 2px solid #333; padding-bottom: 4px; }
            h2 { font-size: 15pt; margin-top: 18px; border-bottom: 1px solid #ccc; padding-bottom: 2px; }
            h3 { font-size: 12pt; margin-top: 14px; }
            table { border-collapse: collapse; width: 100%; margin: 8px 0; }
            th, td { border: 1px solid #bbb; padding: 4px 8px; text-align: left; font-size: 10pt; }
            th { background: #f0f0f0; }
            hr { border: none; border-top: 1px solid #ddd; margin: 12px 0; }
            blockquote { border-left: 3px solid #ccc; margin-left: 0; padding-left: 12px; color: #555; }
            code { font-family: monospace; background: #f5f5f5; padding: 1px 3px; }
        """.trimIndent()
        // The XML declaration MUST be the very first characters of the document — no
        // leading whitespace — or Flying Saucer's XML parser rejects it.
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<html xmlns=\"http://www.w3.org/1999/xhtml\">\n" +
            "<head><title>$safeTitle</title>\n" +
            "<style type=\"text/css\">\n$css\n</style>\n" +
            "</head>\n<body>\n$bodyHtml\n</body>\n</html>"
    }

    private fun String.escapeXml(): String = this
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
}
