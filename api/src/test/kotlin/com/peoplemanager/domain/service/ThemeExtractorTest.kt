package com.peoplemanager.domain.service

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class ThemeExtractorTest {

    @Test
    fun `returns empty for no texts`() {
        ThemeExtractor.extract(emptyList()).shouldBeEmpty()
    }

    @Test
    fun `counts document frequency, not raw frequency`() {
        val texts = listOf(
            "communication communication communication",
            "communication was excellent",
            "delivery was slow"
        )
        val themes = ThemeExtractor.extract(texts)
        val communication = themes.first { it.theme == "communication" }
        // appears in 2 of 3 texts -> count 2, not 4
        communication.count shouldBe 2
    }

    @Test
    fun `drops stopwords and short tokens`() {
        val themes = ThemeExtractor.extract(listOf("the and for you are ok"))
        themes.map { it.theme }.contains("the") shouldBe false
        themes.map { it.theme }.contains("and") shouldBe false
    }

    @Test
    fun `ranks by count then alphabetically`() {
        val texts = listOf(
            "leadership mentoring",
            "leadership delivery",
            "leadership ownership",
            "mentoring ownership"
        )
        val themes = ThemeExtractor.extract(texts, maxThemes = 2)
        themes[0].theme shouldBe "leadership" // count 3
        themes.size shouldBe 2
    }

    @Test
    fun `respects maxThemes`() {
        val texts = listOf("alpha bravo charlie delta echo foxtrot")
        ThemeExtractor.extract(texts, maxThemes = 3).size shouldBe 3
    }
}
