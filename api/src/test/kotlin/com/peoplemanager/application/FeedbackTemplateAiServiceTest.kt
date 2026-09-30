package com.peoplemanager.application

import com.peoplemanager.application.commands.GenerateFeedbackTemplateCommand
import com.peoplemanager.application.port.output.AiClientPort
import com.peoplemanager.application.port.output.AiCompletionResult
import com.peoplemanager.application.port.output.UserSettingsRepository
import com.peoplemanager.domain.FeedbackQuestionType
import com.peoplemanager.domain.UserId
import com.peoplemanager.domain.UserSettings
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import tools.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Test

class FeedbackTemplateAiServiceTest {

    private val userSettingsRepository = mockk<UserSettingsRepository>()
    private val aiClientPort = mockk<AiClientPort>()
    private val aiConfigResolver = mockk<AiConfigResolver>()
    private val objectMapper = jacksonObjectMapper()

    private val service = FeedbackTemplateAiService(
        userSettingsRepository, aiClientPort, aiConfigResolver, objectMapper
    )

    private val userId = UserId.generate()

    @Test
    fun `parses a valid AI template with a follow-up branch`() {
        val json = """
            {
              "title": "Mid-year coaching",
              "description": "Feedback for coaching",
              "questions": [
                {"id": "q1", "type": "RATING", "text": "Rate collaboration", "required": true, "lowLabel": "Poor", "highLabel": "Excellent"},
                {"id": "q1b", "type": "TEXT", "text": "Explain a low rating", "required": false, "showIf": {"questionId": "q1", "operator": "LTE", "value": 3}},
                {"id": "q2", "type": "LIKERT", "text": "Owns their work", "required": true}
              ]
            }
        """.trimIndent()

        val result = service.parseAndValidate(userId, json)

        result.shouldBeInstanceOf<FeedbackTemplateGenerationResult.Success>()
        val t = result.template
        t.title shouldBe "Mid-year coaching"
        t.questions.size shouldBe 3
        t.questions[0].type shouldBe FeedbackQuestionType.RATING
        t.questions[1].showIf?.questionId shouldBe "q1"
        t.userId shouldBe userId
    }

    @Test
    fun `strips markdown code fences`() {
        val json = "```json\n{\"title\":\"T\",\"questions\":[{\"id\":\"q1\",\"type\":\"TEXT\",\"text\":\"Q\"}]}\n```"
        val result = service.parseAndValidate(userId, json)
        result.shouldBeInstanceOf<FeedbackTemplateGenerationResult.Success>()
    }

    @Test
    fun `returns error when JSON is malformed`() {
        val result = service.parseAndValidate(userId, "not json at all")
        result.shouldBeInstanceOf<FeedbackTemplateGenerationResult.Error>()
    }

    @Test
    fun `returns error when no usable questions`() {
        val result = service.parseAndValidate(userId, """{"title":"T","questions":[]}""")
        result.shouldBeInstanceOf<FeedbackTemplateGenerationResult.Error>()
    }

    @Test
    fun `returns error when AI produces invalid branching (rejected by domain)`() {
        // showIf references a later question -> domain invariant violation -> error, not a crash
        val json = """
            {"title":"T","questions":[
              {"id":"q1","type":"TEXT","text":"first","showIf":{"questionId":"q2","operator":"LTE","value":3}},
              {"id":"q2","type":"RATING","text":"second"}
            ]}
        """.trimIndent()
        val result = service.parseAndValidate(userId, json)
        result.shouldBeInstanceOf<FeedbackTemplateGenerationResult.Error>()
    }

    @Test
    fun `generate returns error when AI not configured`() {
        every { userSettingsRepository.findByUserId(userId) } returns UserSettings.createDefault(userId)
        every { aiConfigResolver.resolve(any()) } returns null

        val result = service.generate(GenerateFeedbackTemplateCommand(userId, "Post-project retro"))
        result.shouldBeInstanceOf<FeedbackTemplateGenerationResult.Error>()
        (result as FeedbackTemplateGenerationResult.Error).message shouldContain "not configured"
    }

    @Test
    fun `generate returns error when brief is blank`() {
        val result = service.generate(GenerateFeedbackTemplateCommand(userId, "  "))
        result.shouldBeInstanceOf<FeedbackTemplateGenerationResult.Error>()
    }

    @Test
    fun `generate surfaces AI transport error`() {
        every { userSettingsRepository.findByUserId(userId) } returns UserSettings.createDefault(userId)
        every { aiConfigResolver.resolve(any()) } returns ResolvedAiConfig("http://x", null, "m", AiConfigSource.ADMIN_DEFAULTS)
        every { aiClientPort.chatCompletion(any(), any(), any(), any(), any()) } returns AiCompletionResult.Error("boom")

        val result = service.generate(GenerateFeedbackTemplateCommand(userId, "brief"))
        result.shouldBeInstanceOf<FeedbackTemplateGenerationResult.Error>()
        (result as FeedbackTemplateGenerationResult.Error).message shouldBe "boom"
    }
}
