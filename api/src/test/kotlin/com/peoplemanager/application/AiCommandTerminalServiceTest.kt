package com.peoplemanager.application

import com.peoplemanager.application.port.output.AiClientPort
import com.peoplemanager.application.port.output.AiCompletionResult
import com.peoplemanager.application.port.output.PersonRepository
import com.peoplemanager.application.port.output.UserSettingsRepository
import com.peoplemanager.domain.*
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.collections.shouldContainExactly
import java.util.UUID

class AiCommandTerminalServiceTest {

    private val aiClientPort: AiClientPort = mockk()
    private val userSettingsRepository: UserSettingsRepository = mockk()
    private val personRepository: PersonRepository = mockk()
    private val objectMapper: ObjectMapper = jacksonObjectMapper()

    private val service = AiCommandTerminalService(
        aiClientPort, userSettingsRepository, personRepository, objectMapper
    )

    private val userId = UserId(UUID.randomUUID())
    private val personId = PersonId(UUID.randomUUID())

    private val configuredSettings = UserSettings(
        userId = userId,
        aiEnabled = true,
        aiApiBaseUrl = "http://localhost:11434/v1",
        aiModelName = "llama3",
        aiApiKey = null
    )

    private val testPerson = Person(
        id = personId,
        userId = userId,
        name = "Alice Smith",
        preferredName = "Alice"
    )

    @BeforeEach
    fun setup() {
        every { userSettingsRepository.findByUserId(userId) } returns configuredSettings
        every { personRepository.findAllByUserIdUnpaged(userId) } returns listOf(testPerson)
    }

    // ===== parseCommand =====

    @Test
    fun `parseCommand should return error when input is blank`() {
        val result = service.parseCommand(userId, "  ")

        result.error shouldBe "Command input cannot be empty."
        result.intent shouldBe null
    }

    @Test
    fun `parseCommand should return error when AI is not configured`() {
        every { userSettingsRepository.findByUserId(userId) } returns null

        val result = service.parseCommand(userId, "Create a note about Alice")

        result.error shouldBe "AI Assistant is not configured. Please configure it in Settings."
    }

    @Test
    fun `parseCommand should return error when AI is disabled`() {
        every { userSettingsRepository.findByUserId(userId) } returns configuredSettings.copy(aiEnabled = false)

        val result = service.parseCommand(userId, "Create a note about Alice")

        result.error shouldBe "AI Assistant is not configured. Please configure it in Settings."
    }

    @Test
    fun `parseCommand should return parsed action item on success`() {
        val aiResponse = """
            {"intent": "create_action_item", "target_person_id": "${personId.value}", "content": "Follow up on project deadline", "due_date": "2026-06-13", "tags": ["project"], "sensitive": false}
        """.trimIndent()

        every { aiClientPort.chatCompletion(any(), any(), any(), any(), any()) } returns
            AiCompletionResult.Success(aiResponse)

        val result = service.parseCommand(userId, "Remind Alice to follow up on project deadline by next Friday")

        result.error shouldBe null
        result.intent shouldBe "create_action_item"
        result.targetPersonId shouldBe personId.value.toString()
        result.content shouldBe "Follow up on project deadline"
        result.dueDate shouldBe "2026-06-13"
        result.tags shouldContainExactly listOf("project")
        result.sensitive shouldBe false
    }

    @Test
    fun `parseCommand should return parsed kudo on success`() {
        val aiResponse = """
            {"intent": "create_kudo", "target_person_id": "${personId.value}", "content": "Great job on the presentation", "due_date": null, "tags": ["presentation", "leadership"], "sensitive": false}
        """.trimIndent()

        every { aiClientPort.chatCompletion(any(), any(), any(), any(), any()) } returns
            AiCompletionResult.Success(aiResponse)

        val result = service.parseCommand(userId, "Give kudos to Alice for the great presentation")

        result.error shouldBe null
        result.intent shouldBe "create_kudo"
        result.targetPersonId shouldBe personId.value.toString()
        result.content shouldBe "Great job on the presentation"
        result.tags shouldContainExactly listOf("presentation", "leadership")
    }

    @Test
    fun `parseCommand should return parsed quick note on success`() {
        val aiResponse = """
            {"intent": "create_quick_note", "target_person_id": null, "content": "Remember to schedule team offsite", "due_date": null, "meeting_date": null, "tags": [], "sensitive": false}
        """.trimIndent()

        every { aiClientPort.chatCompletion(any(), any(), any(), any(), any()) } returns
            AiCompletionResult.Success(aiResponse)

        val result = service.parseCommand(userId, "Note: Remember to schedule team offsite")

        result.error shouldBe null
        result.intent shouldBe "create_quick_note"
        result.targetPersonId shouldBe null
        result.content shouldBe "Remember to schedule team offsite"
    }

    @Test
    fun `parseCommand should return parsed 1-1 entry on success`() {
        val aiResponse = """
            {"intent": "create_one_on_one_entry", "target_person_id": "${personId.value}", "content": "Discussed project timeline and blockers", "due_date": null, "meeting_date": "2026-06-06", "tags": ["project"], "sensitive": false}
        """.trimIndent()

        every { aiClientPort.chatCompletion(any(), any(), any(), any(), any()) } returns
            AiCompletionResult.Success(aiResponse)

        val result = service.parseCommand(userId, "I just had a chat with Alice about the project timeline and blockers")

        result.error shouldBe null
        result.intent shouldBe "create_one_on_one_entry"
        result.targetPersonId shouldBe personId.value.toString()
        result.content shouldBe "Discussed project timeline and blockers"
        result.meetingDate shouldBe "2026-06-06"
        result.tags shouldContainExactly listOf("project")
    }

    @Test
    fun `parseCommand should handle 1-1 entry with today as default meeting date`() {
        val today = java.time.LocalDate.now().toString()
        val aiResponse = """
            {"intent": "create_one_on_one_entry", "target_person_id": "${personId.value}", "content": "Quick sync about sprint planning", "due_date": null, "meeting_date": "$today", "tags": [], "sensitive": false}
        """.trimIndent()

        every { aiClientPort.chatCompletion(any(), any(), any(), any(), any()) } returns
            AiCompletionResult.Success(aiResponse)

        val result = service.parseCommand(userId, "Had a quick sync with Alice about sprint planning")

        result.error shouldBe null
        result.intent shouldBe "create_one_on_one_entry"
        result.meetingDate shouldBe today
    }

    @Test
    fun `parseCommand should override hallucinated meeting date with today`() {
        val today = java.time.LocalDate.now().toString()
        // LLM returns 2024-02-20 which is >30 days from today (2026-06-07) — clearly hallucinated
        val aiResponse = """
            {"intent": "create_one_on_one_entry", "target_person_id": "${personId.value}", "content": "Discussed project", "due_date": null, "meeting_date": "2024-02-20", "tags": [], "sensitive": false}
        """.trimIndent()

        every { aiClientPort.chatCompletion(any(), any(), any(), any(), any()) } returns
            AiCompletionResult.Success(aiResponse)

        val result = service.parseCommand(userId, "I just talked to Alice about the project")

        result.error shouldBe null
        result.intent shouldBe "create_one_on_one_entry"
        result.meetingDate shouldBe today
    }

    @Test
    fun `parseCommand should use null meeting date as today for 1-1 entries`() {
        val today = java.time.LocalDate.now().toString()
        val aiResponse = """
            {"intent": "create_one_on_one_entry", "target_person_id": "${personId.value}", "content": "Sync notes", "due_date": null, "meeting_date": null, "tags": [], "sensitive": false}
        """.trimIndent()

        every { aiClientPort.chatCompletion(any(), any(), any(), any(), any()) } returns
            AiCompletionResult.Success(aiResponse)

        val result = service.parseCommand(userId, "Had a chat with Alice")

        result.error shouldBe null
        result.meetingDate shouldBe today
    }

    @Test
    fun `parseCommand should handle sensitive flag`() {
        val aiResponse = """
            {"intent": "create_quick_note", "target_person_id": "${personId.value}", "content": "PIP discussion notes", "due_date": null, "tags": [], "sensitive": true}
        """.trimIndent()

        every { aiClientPort.chatCompletion(any(), any(), any(), any(), any()) } returns
            AiCompletionResult.Success(aiResponse)

        val result = service.parseCommand(userId, "Private note about Alice: PIP discussion notes")

        result.error shouldBe null
        result.sensitive shouldBe true
    }

    @Test
    fun `parseCommand should handle markdown-wrapped JSON response`() {
        val aiResponse = """
            ```json
            {"intent": "create_quick_note", "target_person_id": null, "content": "Test note", "due_date": null, "tags": [], "sensitive": false}
            ```
        """.trimIndent()

        every { aiClientPort.chatCompletion(any(), any(), any(), any(), any()) } returns
            AiCompletionResult.Success(aiResponse)

        val result = service.parseCommand(userId, "Take a note: Test note")

        result.error shouldBe null
        result.intent shouldBe "create_quick_note"
        result.content shouldBe "Test note"
    }

    @Test
    fun `parseCommand should return error for invalid intent`() {
        val aiResponse = """
            {"intent": "delete_everything", "target_person_id": null, "content": "bad", "due_date": null, "tags": [], "sensitive": false}
        """.trimIndent()

        every { aiClientPort.chatCompletion(any(), any(), any(), any(), any()) } returns
            AiCompletionResult.Success(aiResponse)

        val result = service.parseCommand(userId, "Do something weird")

        result.error shouldBe "Could not determine the action. Please try rephrasing your command."
    }

    @Test
    fun `parseCommand should return error when content is empty`() {
        val aiResponse = """
            {"intent": "create_action_item", "target_person_id": null, "content": "", "due_date": null, "tags": [], "sensitive": false}
        """.trimIndent()

        every { aiClientPort.chatCompletion(any(), any(), any(), any(), any()) } returns
            AiCompletionResult.Success(aiResponse)

        val result = service.parseCommand(userId, "...")

        result.error shouldBe "Could not extract content from your command. Please try rephrasing."
    }

    @Test
    fun `parseCommand should return error when AI returns invalid JSON`() {
        every { aiClientPort.chatCompletion(any(), any(), any(), any(), any()) } returns
            AiCompletionResult.Success("Sorry, I don't understand that command.")

        val result = service.parseCommand(userId, "foo bar baz")

        result.error shouldBe "Failed to parse AI response. Please try rephrasing your command."
    }

    @Test
    fun `parseCommand should return error when AI API fails`() {
        every { aiClientPort.chatCompletion(any(), any(), any(), any(), any()) } returns
            AiCompletionResult.Error("Connection refused")

        val result = service.parseCommand(userId, "Create a note")

        result.error shouldBe "Connection refused"
    }

    @Test
    fun `parseCommand should pass person directory in user message`() {
        val aiResponse = """
            {"intent": "create_quick_note", "target_person_id": null, "content": "Test", "due_date": null, "tags": [], "sensitive": false}
        """.trimIndent()

        every { aiClientPort.chatCompletion(any(), any(), any(), any(), any()) } returns
            AiCompletionResult.Success(aiResponse)

        service.parseCommand(userId, "Test")

        verify {
            aiClientPort.chatCompletion(
                baseUrl = "http://localhost:11434/v1",
                apiKey = null,
                model = "llama3",
                systemPrompt = UserSettings.DEFAULT_COMMAND_TERMINAL_PROMPT,
                userMessage = match { it.contains("Alice") && it.contains(personId.value.toString()) && it.contains("Current Date:") }
            )
        }
    }

    @Test
    fun `parseCommand should inject current date and time into user message`() {
        val aiResponse = """
            {"intent": "create_quick_note", "target_person_id": null, "content": "Test", "due_date": null, "tags": [], "sensitive": false}
        """.trimIndent()

        every { aiClientPort.chatCompletion(any(), any(), any(), any(), any()) } returns
            AiCompletionResult.Success(aiResponse)

        service.parseCommand(userId, "Test")

        val today = java.time.LocalDate.now().toString()
        val tomorrow = java.time.LocalDate.now().plusDays(1).toString()

        verify {
            aiClientPort.chatCompletion(
                baseUrl = any(),
                apiKey = any(),
                model = any(),
                systemPrompt = any(),
                userMessage = match {
                    it.contains("Current Date: $today") &&
                    it.contains("Current Time:") &&
                    it.contains("This week's dates:") &&
                    it.contains(tomorrow)
                }
            )
        }
    }

    @Test
    fun `parseCommand should use custom prompt when set`() {
        val customPrompt = "Custom parser instruction"
        every { userSettingsRepository.findByUserId(userId) } returns configuredSettings.copy(commandTerminalPrompt = customPrompt)

        val aiResponse = """
            {"intent": "create_quick_note", "target_person_id": null, "content": "Test", "due_date": null, "tags": [], "sensitive": false}
        """.trimIndent()

        every { aiClientPort.chatCompletion(any(), any(), any(), any(), any()) } returns
            AiCompletionResult.Success(aiResponse)

        service.parseCommand(userId, "Test")

        verify {
            aiClientPort.chatCompletion(
                baseUrl = any(),
                apiKey = any(),
                model = any(),
                systemPrompt = customPrompt,
                userMessage = any()
            )
        }
    }

    // ===== getPersonDirectory =====

    @Test
    fun `getPersonDirectory should return person id and preferred name`() {
        val result = service.getPersonDirectory(userId)

        result.size shouldBe 1
        result[0].id shouldBe personId.value.toString()
        result[0].preferredName shouldBe "Alice"
    }

    @Test
    fun `getPersonDirectory should use name when preferredName is null`() {
        val personWithoutPreferred = testPerson.copy(preferredName = null)
        every { personRepository.findAllByUserIdUnpaged(userId) } returns listOf(personWithoutPreferred)

        val result = service.getPersonDirectory(userId)

        result[0].preferredName shouldBe "Alice Smith"
    }

    @Test
    fun `getPersonDirectory should return empty list when no persons`() {
        every { personRepository.findAllByUserIdUnpaged(userId) } returns emptyList()

        val result = service.getPersonDirectory(userId)

        result.size shouldBe 0
    }
}
