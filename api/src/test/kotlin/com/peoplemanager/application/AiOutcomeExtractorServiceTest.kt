package com.peoplemanager.application

import com.peoplemanager.application.port.output.AiClientPort
import com.peoplemanager.application.port.output.AiCompletionResult
import com.peoplemanager.application.port.output.ActionItemRepository
import com.peoplemanager.application.port.output.OneOnOneEntryRepository
import com.peoplemanager.application.port.output.PersonRepository
import com.peoplemanager.application.port.output.UserSettingsRepository
import com.peoplemanager.domain.*
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.jacksonObjectMapper
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.slot
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.collections.shouldHaveSize
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class AiOutcomeExtractorServiceTest {

    private val userSettingsRepository: UserSettingsRepository = mockk()
    private val personRepository: PersonRepository = mockk()
    private val entryRepository: OneOnOneEntryRepository = mockk()
    private val actionItemRepository: ActionItemRepository = mockk()
    private val aiClientPort: AiClientPort = mockk()
    private val objectMapper: ObjectMapper = jacksonObjectMapper()
    private val aiConfigResolver = AiConfigResolver(defaultBaseUrl = "", defaultApiKey = "", defaultModel = "")

    private val service = AiOutcomeExtractorService(
        userSettingsRepository,
        personRepository,
        entryRepository,
        actionItemRepository,
        aiClientPort,
        objectMapper,
        aiConfigResolver
    )

    private val userId = UserId(UUID.randomUUID())
    private val personId = PersonId(UUID.randomUUID())
    private val entryId = OneOnOneEntryId(UUID.randomUUID())

    private val person = Person(
        id = personId,
        userId = userId,
        name = "Alice Smith",
        roleTitle = "Senior Engineer"
    )

    private val entry = OneOnOneEntry(
        id = entryId,
        userId = userId,
        personId = personId,
        meetingDate = Instant.now(),
        notesMarkdown = "- Alice will finish the API docs by next week\n- We agreed to move to biweekly 1:1s\n- I need to set up her access to the staging environment",
        sensitive = false
    )

    private val configuredSettings = UserSettings(
        userId = userId,
        aiEnabled = true,
        aiApiBaseUrl = "http://localhost:11434/v1",
        aiModelName = "llama3",
        aiApiKey = "test-key"
    )

    @BeforeEach
    fun setup() {
        every { personRepository.findByIdAndUserId(personId, userId) } returns person
        every { entryRepository.findByIdAndUserIdAndPersonId(entryId, userId, personId) } returns entry
        every { userSettingsRepository.findByUserId(userId) } returns configuredSettings
    }

    // ===== extractOutcomes =====

    @Test
    fun `extractOutcomes should return extracted action items and decisions on success`() {
        val aiResponse = """
            {
                "action_items": [
                    {"title": "Finish API docs", "owner_type": "PERSON", "suggested_days_to_due": 7},
                    {"title": "Set up staging access", "owner_type": "MANAGER", "suggested_days_to_due": 3}
                ],
                "decisions": ["Move to biweekly 1:1 cadence"]
            }
        """.trimIndent()

        every { aiClientPort.chatCompletion(any(), any(), any(), any(), any()) } returns
            AiCompletionResult.Success(aiResponse)

        val result = service.extractOutcomes(userId, personId, entryId)

        result.shouldBeInstanceOf<AiExtractionResult.Success>()
        result.actionItems shouldHaveSize 2
        result.actionItems[0].title shouldBe "Finish API docs"
        result.actionItems[0].ownerType shouldBe "PERSON"
        result.actionItems[0].suggestedDaysToDue shouldBe 7
        result.actionItems[1].title shouldBe "Set up staging access"
        result.actionItems[1].ownerType shouldBe "MANAGER"
        result.actionItems[1].suggestedDaysToDue shouldBe 3
        result.decisions shouldHaveSize 1
        result.decisions[0] shouldBe "Move to biweekly 1:1 cadence"
    }

    @Test
    fun `extractOutcomes should handle JSON wrapped in markdown code fences`() {
        val aiResponse = """
            ```json
            {
                "action_items": [
                    {"title": "Review PR", "owner_type": "MANAGER", "suggested_days_to_due": 2}
                ],
                "decisions": ["Approved new architecture"]
            }
            ```
        """.trimIndent()

        every { aiClientPort.chatCompletion(any(), any(), any(), any(), any()) } returns
            AiCompletionResult.Success(aiResponse)

        val result = service.extractOutcomes(userId, personId, entryId)

        result.shouldBeInstanceOf<AiExtractionResult.Success>()
        result.actionItems shouldHaveSize 1
        result.actionItems[0].title shouldBe "Review PR"
        result.decisions shouldHaveSize 1
    }

    @Test
    fun `extractOutcomes should return error when person not found`() {
        every { personRepository.findByIdAndUserId(personId, userId) } returns null

        org.junit.jupiter.api.assertThrows<PersonNotFoundException> {
            service.extractOutcomes(userId, personId, entryId)
        }
    }

    @Test
    fun `extractOutcomes should return error when entry not found`() {
        every { entryRepository.findByIdAndUserIdAndPersonId(entryId, userId, personId) } returns null

        org.junit.jupiter.api.assertThrows<OneOnOneEntryNotFoundException> {
            service.extractOutcomes(userId, personId, entryId)
        }
    }

    @Test
    fun `extractOutcomes should return error when AI is not configured`() {
        every { userSettingsRepository.findByUserId(userId) } returns null

        val result = service.extractOutcomes(userId, personId, entryId)

        result.shouldBeInstanceOf<AiExtractionResult.Error>()
        result.message shouldBe "AI Assistant is not configured. Please configure it in Settings or ask your admin to set team defaults."
    }

    @Test
    fun `extractOutcomes should return error when AI is disabled`() {
        every { userSettingsRepository.findByUserId(userId) } returns configuredSettings.copy(aiEnabled = false)

        val result = service.extractOutcomes(userId, personId, entryId)

        result.shouldBeInstanceOf<AiExtractionResult.Error>()
        result.message shouldBe "AI Assistant is not configured. Please configure it in Settings or ask your admin to set team defaults."
    }

    @Test
    fun `extractOutcomes should refuse sensitive entries when privacy mode is ON`() {
        val sensitiveEntry = entry.copy(sensitive = true)
        every { entryRepository.findByIdAndUserIdAndPersonId(entryId, userId, personId) } returns sensitiveEntry
        every { userSettingsRepository.findByUserId(userId) } returns configuredSettings.copy(aiPrivacyMode = true)

        val result = service.extractOutcomes(userId, personId, entryId)

        result.shouldBeInstanceOf<AiExtractionResult.Error>()
        result.message shouldBe "Cannot extract outcomes from a sensitive entry while AI Privacy Mode is enabled. Disable AI Privacy Mode in Settings to use this feature on sensitive entries."
    }

    @Test
    fun `extractOutcomes should allow sensitive entries when privacy mode is OFF`() {
        val sensitiveEntry = entry.copy(sensitive = true)
        every { entryRepository.findByIdAndUserIdAndPersonId(entryId, userId, personId) } returns sensitiveEntry
        every { userSettingsRepository.findByUserId(userId) } returns configuredSettings.copy(aiPrivacyMode = false)

        val aiResponse = """{"action_items": [], "decisions": ["Some decision"]}"""
        every { aiClientPort.chatCompletion(any(), any(), any(), any(), any()) } returns
            AiCompletionResult.Success(aiResponse)

        val result = service.extractOutcomes(userId, personId, entryId)

        result.shouldBeInstanceOf<AiExtractionResult.Success>()
    }

    @Test
    fun `extractOutcomes should return error when notes are empty`() {
        val emptyNotesEntry = entry.copy(notesMarkdown = "")
        every { entryRepository.findByIdAndUserIdAndPersonId(entryId, userId, personId) } returns emptyNotesEntry

        val result = service.extractOutcomes(userId, personId, entryId)

        result.shouldBeInstanceOf<AiExtractionResult.Error>()
        result.message shouldBe "Cannot extract outcomes: the notes field is empty."
    }

    @Test
    fun `extractOutcomes should return error when notes are null`() {
        val nullNotesEntry = entry.copy(notesMarkdown = null)
        every { entryRepository.findByIdAndUserIdAndPersonId(entryId, userId, personId) } returns nullNotesEntry

        val result = service.extractOutcomes(userId, personId, entryId)

        result.shouldBeInstanceOf<AiExtractionResult.Error>()
        result.message shouldBe "Cannot extract outcomes: the notes field is empty."
    }

    @Test
    fun `extractOutcomes should return error when AI API fails`() {
        every { aiClientPort.chatCompletion(any(), any(), any(), any(), any()) } returns
            AiCompletionResult.Error("Connection refused")

        val result = service.extractOutcomes(userId, personId, entryId)

        result.shouldBeInstanceOf<AiExtractionResult.Error>()
        result.message shouldBe "Connection refused"
    }

    @Test
    fun `extractOutcomes should return error when AI returns invalid JSON`() {
        every { aiClientPort.chatCompletion(any(), any(), any(), any(), any()) } returns
            AiCompletionResult.Success("This is not JSON at all")

        val result = service.extractOutcomes(userId, personId, entryId)

        result.shouldBeInstanceOf<AiExtractionResult.Error>()
        result.message shouldBe "Failed to parse AI response. The model may not have returned valid JSON."
    }

    @Test
    fun `extractOutcomes should use custom prompt when set`() {
        val customPrompt = "Custom extraction prompt"
        every { userSettingsRepository.findByUserId(userId) } returns configuredSettings.copy(outcomeExtractorPrompt = customPrompt)
        every { aiClientPort.chatCompletion(any(), any(), any(), any(), any()) } returns
            AiCompletionResult.Success("""{"action_items": [], "decisions": []}""")

        service.extractOutcomes(userId, personId, entryId)

        verify {
            aiClientPort.chatCompletion(
                baseUrl = "http://localhost:11434/v1",
                apiKey = "test-key",
                model = "llama3",
                systemPrompt = customPrompt,
                userMessage = any()
            )
        }
    }

    @Test
    fun `extractOutcomes should include person name and role in user message`() {
        val messageSlot = slot<String>()
        every { aiClientPort.chatCompletion(any(), any(), any(), any(), capture(messageSlot)) } returns
            AiCompletionResult.Success("""{"action_items": [], "decisions": []}""")

        service.extractOutcomes(userId, personId, entryId)

        messageSlot.captured shouldBe "Person: Alice Smith (Senior Engineer)\n\nMeeting Notes:\n${entry.notesMarkdown}"
    }

    @Test
    fun `extractOutcomes should filter out blank action item titles`() {
        val aiResponse = """
            {
                "action_items": [
                    {"title": "Valid item", "owner_type": "MANAGER", "suggested_days_to_due": 7},
                    {"title": "", "owner_type": "PERSON", "suggested_days_to_due": 3},
                    {"title": "  ", "owner_type": "PERSON", "suggested_days_to_due": 5}
                ],
                "decisions": []
            }
        """.trimIndent()

        every { aiClientPort.chatCompletion(any(), any(), any(), any(), any()) } returns
            AiCompletionResult.Success(aiResponse)

        val result = service.extractOutcomes(userId, personId, entryId)

        result.shouldBeInstanceOf<AiExtractionResult.Success>()
        result.actionItems shouldHaveSize 1
        result.actionItems[0].title shouldBe "Valid item"
    }

    // ===== applyOutcomes =====

    @Test
    fun `applyOutcomes should create action items and append decisions`() {
        val savedItemSlot = mutableListOf<ActionItem>()
        every { actionItemRepository.save(capture(savedItemSlot)) } answers { firstArg() }
        every { entryRepository.save(any()) } answers { firstArg() }

        val command = ApplyOutcomesCommand(
            actionItems = listOf(
                ApplyActionItem("Finish docs", "PERSON", 7),
                ApplyActionItem("Set up access", "MANAGER", 3)
            ),
            decisions = listOf("Move to biweekly cadence")
        )

        val result = service.applyOutcomes(userId, personId, entryId, command)

        result.actionItemsCreated shouldBe 2
        result.decisionsAppended shouldBe 1

        savedItemSlot shouldHaveSize 2
        savedItemSlot[0].title shouldBe "Finish docs"
        savedItemSlot[0].ownerType shouldBe ActionItemOwnerType.PERSON
        savedItemSlot[0].dueDate shouldBe LocalDate.now().plusDays(7)
        savedItemSlot[0].originatingEntryId shouldBe entryId
        savedItemSlot[0].userId shouldBe userId
        savedItemSlot[0].personId shouldBe personId

        savedItemSlot[1].title shouldBe "Set up access"
        savedItemSlot[1].ownerType shouldBe ActionItemOwnerType.MANAGER
        savedItemSlot[1].dueDate shouldBe LocalDate.now().plusDays(3)
    }

    @Test
    fun `applyOutcomes should append decisions to existing outcomes`() {
        val entryWithOutcomes = entry.copy(outcomesMarkdown = "## Previous outcomes\n- Something")
        every { entryRepository.findByIdAndUserIdAndPersonId(entryId, userId, personId) } returns entryWithOutcomes
        every { actionItemRepository.save(any()) } answers { firstArg() }

        val entrySlot = slot<OneOnOneEntry>()
        every { entryRepository.save(capture(entrySlot)) } answers { firstArg() }

        val command = ApplyOutcomesCommand(
            actionItems = emptyList(),
            decisions = listOf("Agreed on new timeline", "Will revisit in 2 weeks")
        )

        service.applyOutcomes(userId, personId, entryId, command)

        entrySlot.captured.outcomesMarkdown shouldBe "## Previous outcomes\n- Something\n\n### Extracted Decisions\n- Agreed on new timeline\n- Will revisit in 2 weeks"
    }

    @Test
    fun `applyOutcomes should set outcomes when field is empty`() {
        val entryNoOutcomes = entry.copy(outcomesMarkdown = null)
        every { entryRepository.findByIdAndUserIdAndPersonId(entryId, userId, personId) } returns entryNoOutcomes
        every { actionItemRepository.save(any()) } answers { firstArg() }

        val entrySlot = slot<OneOnOneEntry>()
        every { entryRepository.save(capture(entrySlot)) } answers { firstArg() }

        val command = ApplyOutcomesCommand(
            actionItems = emptyList(),
            decisions = listOf("Key decision made")
        )

        service.applyOutcomes(userId, personId, entryId, command)

        entrySlot.captured.outcomesMarkdown shouldBe "### Extracted Decisions\n- Key decision made"
    }

    @Test
    fun `applyOutcomes should not update entry when no decisions`() {
        every { actionItemRepository.save(any()) } answers { firstArg() }

        val command = ApplyOutcomesCommand(
            actionItems = listOf(ApplyActionItem("Do something", "MANAGER", 5)),
            decisions = emptyList()
        )

        service.applyOutcomes(userId, personId, entryId, command)

        verify(exactly = 0) { entryRepository.save(any()) }
    }

    @Test
    fun `applyOutcomes should handle null suggestedDaysToDue`() {
        every { actionItemRepository.save(any()) } answers { firstArg() }

        val command = ApplyOutcomesCommand(
            actionItems = listOf(ApplyActionItem("No due date task", "MANAGER", null)),
            decisions = emptyList()
        )

        val result = service.applyOutcomes(userId, personId, entryId, command)

        result.actionItemsCreated shouldBe 1
        verify {
            actionItemRepository.save(match { it.dueDate == null })
        }
    }

    @Test
    fun `applyOutcomes should default to MANAGER when invalid ownerType`() {
        every { actionItemRepository.save(any()) } answers { firstArg() }

        val command = ApplyOutcomesCommand(
            actionItems = listOf(ApplyActionItem("Task", "INVALID", 5)),
            decisions = emptyList()
        )

        service.applyOutcomes(userId, personId, entryId, command)

        verify {
            actionItemRepository.save(match { it.ownerType == ActionItemOwnerType.MANAGER })
        }
    }

    @Test
    fun `applyOutcomes should throw when person not found`() {
        every { personRepository.findByIdAndUserId(personId, userId) } returns null

        val command = ApplyOutcomesCommand(actionItems = emptyList(), decisions = emptyList())

        org.junit.jupiter.api.assertThrows<PersonNotFoundException> {
            service.applyOutcomes(userId, personId, entryId, command)
        }
    }

    @Test
    fun `applyOutcomes should throw when entry not found`() {
        every { entryRepository.findByIdAndUserIdAndPersonId(entryId, userId, personId) } returns null

        val command = ApplyOutcomesCommand(actionItems = emptyList(), decisions = emptyList())

        org.junit.jupiter.api.assertThrows<OneOnOneEntryNotFoundException> {
            service.applyOutcomes(userId, personId, entryId, command)
        }
    }
}
