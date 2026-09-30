package com.peoplemanager.application

import com.peoplemanager.application.commands.GenerateFeedbackSummaryCommand
import com.peoplemanager.application.port.output.AiClientPort
import com.peoplemanager.application.port.output.AiCompletionResult
import com.peoplemanager.application.port.output.FeedbackResponseRepository
import com.peoplemanager.application.port.output.PersonRepository
import com.peoplemanager.application.port.output.UserSettingsRepository
import com.peoplemanager.domain.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class FeedbackSummaryAiServiceTest {

    private val userSettingsRepository = mockk<UserSettingsRepository>()
    private val personRepository = mockk<PersonRepository>()
    private val responseRepository = mockk<FeedbackResponseRepository>()
    private val aiClientPort = mockk<AiClientPort>()
    private val aiConfigResolver = mockk<AiConfigResolver>()
    private val service = FeedbackSummaryAiService(userSettingsRepository, personRepository, responseRepository, aiClientPort, aiConfigResolver)

    private val userId = UserId.generate()
    private val personId = PersonId.generate()
    private val person = Person(id = personId, userId = userId, name = "Alex")

    private fun resp(text: String) = FeedbackResponse(
        id = FeedbackResponseId.generate(), userId = userId, personId = personId, linkId = FeedbackLinkId.generate(),
        answers = listOf(FeedbackAnswer("q1", ratingValue = 4), FeedbackAnswer("q2", textValue = text)),
        status = FeedbackResponseStatus.APPROVED
    )

    @BeforeEach
    fun setup() {
        clearAllMocks()
        every { personRepository.findByIdAndUserId(personId, userId) } returns person
        every { userSettingsRepository.findByUserId(userId) } returns UserSettings.createDefault(userId)
    }

    @Test
    fun `returns error when AI not configured`() {
        every { aiConfigResolver.resolve(any()) } returns null
        every { responseRepository.findAllByUserIdAndPersonId(userId, personId) } returns listOf(resp("great"))
        val result = service.generate(GenerateFeedbackSummaryCommand(userId, personId))
        result.shouldBeInstanceOf<FeedbackSummaryResult.Error>()
        (result as FeedbackSummaryResult.Error).message shouldContain "not configured"
    }

    @Test
    fun `returns error when there is no usable feedback`() {
        every { aiConfigResolver.resolve(any()) } returns ResolvedAiConfig("http://x", null, "m", AiConfigSource.ADMIN_DEFAULTS)
        every { responseRepository.findAllByUserIdAndPersonId(userId, personId) } returns emptyList()
        val result = service.generate(GenerateFeedbackSummaryCommand(userId, personId))
        result.shouldBeInstanceOf<FeedbackSummaryResult.Error>()
    }

    @Test
    fun `generates a summary from usable feedback and includes context`() {
        every { aiConfigResolver.resolve(any()) } returns ResolvedAiConfig("http://x", null, "m", AiConfigSource.ADMIN_DEFAULTS)
        every { responseRepository.findAllByUserIdAndPersonId(userId, personId) } returns listOf(resp("excellent communication"))
        val msgSlot = slot<String>()
        every { aiClientPort.chatCompletion(any(), any(), any(), any(), capture(msgSlot)) } returns
            AiCompletionResult.Success("Alex communicates clearly and is reliable.")

        val result = service.generate(GenerateFeedbackSummaryCommand(userId, personId))
        result.shouldBeInstanceOf<FeedbackSummaryResult.Success>()
        (result as FeedbackSummaryResult.Success).responseCount shouldContainInt 1
        msgSlot.captured shouldContain "excellent communication"
        msgSlot.captured shouldContain "Average rating"
    }

    @Test
    fun `surfaces AI transport error`() {
        every { aiConfigResolver.resolve(any()) } returns ResolvedAiConfig("http://x", null, "m", AiConfigSource.ADMIN_DEFAULTS)
        every { responseRepository.findAllByUserIdAndPersonId(userId, personId) } returns listOf(resp("great"))
        every { aiClientPort.chatCompletion(any(), any(), any(), any(), any()) } returns AiCompletionResult.Error("boom")
        val result = service.generate(GenerateFeedbackSummaryCommand(userId, personId))
        result.shouldBeInstanceOf<FeedbackSummaryResult.Error>()
    }

    @Test
    fun `throws when person not owned`() {
        every { personRepository.findByIdAndUserId(personId, userId) } returns null
        shouldThrow<PersonNotFoundException> {
            service.generate(GenerateFeedbackSummaryCommand(userId, personId))
        }
    }

    private infix fun Int.shouldContainInt(expected: Int) { assert(this == expected) { "expected $expected but was $this" } }
}
