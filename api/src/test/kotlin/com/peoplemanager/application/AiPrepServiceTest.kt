package com.peoplemanager.application

import com.peoplemanager.application.port.input.*
import com.peoplemanager.application.port.output.*
import com.peoplemanager.domain.*
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class AiPrepServiceTest {

    private val userSettingsRepository = mockk<UserSettingsRepository>()
    private val personRepository = mockk<PersonRepository>()
    private val entryRepository = mockk<OneOnOneEntryRepository>()
    private val actionItemRepository = mockk<ActionItemRepository>()
    private val pdpGoalRepository = mockk<PdpGoalRepository>()
    private val pdpUpdateRepository = mockk<PdpUpdateRepository>()
    private val kudosRepository = mockk<KudosRepository>()
    private val aiClientPort = mockk<AiClientPort>()
    private val aiConfigResolver = AiConfigResolver(defaultBaseUrl = "", defaultApiKey = "", defaultModel = "")

    private lateinit var service: AiPrepService

    private val userId = UserId(UUID.randomUUID())
    private val personId = PersonId(UUID.randomUUID())

    private val person = Person(
        id = personId,
        userId = userId,
        name = "Alice Smith",
        roleTitle = "Engineer"
    )

    private val aiSettings = UserSettings(
        userId = userId,
        aiEnabled = true,
        aiApiBaseUrl = "http://ollama:11434/v1",
        aiApiKey = "test-key",
        aiModelName = "llama3",
        aiPrivacyMode = true
    )

    @BeforeEach
    fun setUp() {
        service = AiPrepService(
            userSettingsRepository,
            personRepository,
            entryRepository,
            actionItemRepository,
            pdpGoalRepository,
            pdpUpdateRepository,
            kudosRepository,
            aiClientPort,
            aiConfigResolver
        )
    }

    @Test
    fun `should return error when person not found`() {
        every { personRepository.findByIdAndUserId(personId, userId) } returns null

        assertThrows(PersonNotFoundException::class.java) {
            service.generateAgendaSuggestions(userId, personId)
        }
    }

    @Test
    fun `should return error when AI is not configured`() {
        every { personRepository.findByIdAndUserId(personId, userId) } returns person
        every { userSettingsRepository.findByUserId(userId) } returns UserSettings.createDefault(userId)

        val result = service.generateAgendaSuggestions(userId, personId)

        assertTrue(result is AiPrepResult.Error)
        assertTrue((result as AiPrepResult.Error).message.contains("not configured"))
    }

    @Test
    fun `should return error when no settings exist`() {
        every { personRepository.findByIdAndUserId(personId, userId) } returns person
        every { userSettingsRepository.findByUserId(userId) } returns null

        val result = service.generateAgendaSuggestions(userId, personId)

        assertTrue(result is AiPrepResult.Error)
        assertTrue((result as AiPrepResult.Error).message.contains("not configured"))
    }

    @Test
    fun `should return error when no context available`() {
        every { personRepository.findByIdAndUserId(personId, userId) } returns person
        every { userSettingsRepository.findByUserId(userId) } returns aiSettings
        every { entryRepository.findAllByUserIdAndPersonId(userId, personId, any()) } returns PageImpl(emptyList())
        every { actionItemRepository.findAllByUserIdAndPersonId(userId, personId, ActionItemStatus.OPEN, any()) } returns PageImpl(emptyList())
        every { pdpGoalRepository.findAllByUserIdAndPersonId(userId, personId, PdpGoalStatus.ACTIVE, any()) } returns PageImpl(emptyList())
        every { kudosRepository.findAllByUserIdAndPersonId(userId, personId, any()) } returns PageImpl(emptyList())

        val result = service.generateAgendaSuggestions(userId, personId)

        assertTrue(result is AiPrepResult.Error)
        assertTrue((result as AiPrepResult.Error).message.contains("No context"))
    }

    @Test
    fun `should call AI client with synthesized context and return suggestions`() {
        val entry = OneOnOneEntry(
            id = OneOnOneEntryId.generate(),
            userId = userId,
            personId = personId,
            meetingDate = Instant.now().minusSeconds(86400),
            notesMarkdown = "Discussed project progress",
            outcomesMarkdown = "Agreed on next steps",
            sensitive = false
        )

        every { personRepository.findByIdAndUserId(personId, userId) } returns person
        every { userSettingsRepository.findByUserId(userId) } returns aiSettings
        every { entryRepository.findAllByUserIdAndPersonId(userId, personId, any()) } returns PageImpl(listOf(entry))
        every { actionItemRepository.findAllByUserIdAndPersonId(userId, personId, ActionItemStatus.OPEN, any()) } returns PageImpl(emptyList())
        every { pdpGoalRepository.findAllByUserIdAndPersonId(userId, personId, PdpGoalStatus.ACTIVE, any()) } returns PageImpl(emptyList())
        every { kudosRepository.findAllByUserIdAndPersonId(userId, personId, any()) } returns PageImpl(emptyList())
        every { aiClientPort.chatCompletion(any(), any(), any(), any(), any()) } returns AiCompletionResult.Success(
            "- Follow up on project progress\n- Discuss blockers\n- Review career goals"
        )

        val result = service.generateAgendaSuggestions(userId, personId)

        assertTrue(result is AiPrepResult.Success)
        val suggestions = (result as AiPrepResult.Success).suggestions
        assertEquals(3, suggestions.size)
        assertEquals("Follow up on project progress", suggestions[0])
        assertEquals("Discuss blockers", suggestions[1])
        assertEquals("Review career goals", suggestions[2])

        verify {
            aiClientPort.chatCompletion(
                "http://ollama:11434/v1",
                "test-key",
                "llama3",
                AiPrepService.SYSTEM_PROMPT,
                any()
            )
        }
    }

    @Test
    fun `should exclude sensitive entries in privacy mode`() {
        val sensitiveEntry = OneOnOneEntry(
            id = OneOnOneEntryId.generate(),
            userId = userId,
            personId = personId,
            meetingDate = Instant.now().minusSeconds(86400),
            notesMarkdown = "Sensitive discussion about performance",
            outcomesMarkdown = "Private outcomes",
            sensitive = true
        )

        every { personRepository.findByIdAndUserId(personId, userId) } returns person
        every { userSettingsRepository.findByUserId(userId) } returns aiSettings
        every { entryRepository.findAllByUserIdAndPersonId(userId, personId, any()) } returns PageImpl(listOf(sensitiveEntry))
        every { actionItemRepository.findAllByUserIdAndPersonId(userId, personId, ActionItemStatus.OPEN, any()) } returns PageImpl(emptyList())
        every { pdpGoalRepository.findAllByUserIdAndPersonId(userId, personId, PdpGoalStatus.ACTIVE, any()) } returns PageImpl(emptyList())
        every { kudosRepository.findAllByUserIdAndPersonId(userId, personId, any()) } returns PageImpl(emptyList())

        val result = service.generateAgendaSuggestions(userId, personId)

        // With only a sensitive entry and privacy mode on, there's no context
        assertTrue(result is AiPrepResult.Error)
        assertTrue((result as AiPrepResult.Error).message.contains("No context"))
    }

    @Test
    fun `should include sensitive entries when privacy mode is off`() {
        val settingsNoPrivacy = aiSettings.copy(aiPrivacyMode = false)
        val sensitiveEntry = OneOnOneEntry(
            id = OneOnOneEntryId.generate(),
            userId = userId,
            personId = personId,
            meetingDate = Instant.now().minusSeconds(86400),
            notesMarkdown = "Sensitive discussion about performance",
            outcomesMarkdown = null,
            sensitive = true
        )

        every { personRepository.findByIdAndUserId(personId, userId) } returns person
        every { userSettingsRepository.findByUserId(userId) } returns settingsNoPrivacy
        every { entryRepository.findAllByUserIdAndPersonId(userId, personId, any()) } returns PageImpl(listOf(sensitiveEntry))
        every { actionItemRepository.findAllByUserIdAndPersonId(userId, personId, ActionItemStatus.OPEN, any()) } returns PageImpl(emptyList())
        every { pdpGoalRepository.findAllByUserIdAndPersonId(userId, personId, PdpGoalStatus.ACTIVE, any()) } returns PageImpl(emptyList())
        every { kudosRepository.findAllByUserIdAndPersonId(userId, personId, any()) } returns PageImpl(emptyList())
        every { aiClientPort.chatCompletion(any(), any(), any(), any(), any()) } returns AiCompletionResult.Success(
            "- Discuss performance improvement plan"
        )

        val result = service.generateAgendaSuggestions(userId, personId)

        assertTrue(result is AiPrepResult.Success)
        verify { aiClientPort.chatCompletion(any(), any(), any(), any(), match { it.contains("Sensitive discussion") }) }
    }

    @Test
    fun `should return error when AI client fails`() {
        val entry = OneOnOneEntry(
            id = OneOnOneEntryId.generate(),
            userId = userId,
            personId = personId,
            meetingDate = Instant.now().minusSeconds(86400),
            notesMarkdown = "Some notes",
            sensitive = false
        )

        every { personRepository.findByIdAndUserId(personId, userId) } returns person
        every { userSettingsRepository.findByUserId(userId) } returns aiSettings
        every { entryRepository.findAllByUserIdAndPersonId(userId, personId, any()) } returns PageImpl(listOf(entry))
        every { actionItemRepository.findAllByUserIdAndPersonId(userId, personId, ActionItemStatus.OPEN, any()) } returns PageImpl(emptyList())
        every { pdpGoalRepository.findAllByUserIdAndPersonId(userId, personId, PdpGoalStatus.ACTIVE, any()) } returns PageImpl(emptyList())
        every { kudosRepository.findAllByUserIdAndPersonId(userId, personId, any()) } returns PageImpl(emptyList())
        every { aiClientPort.chatCompletion(any(), any(), any(), any(), any()) } returns AiCompletionResult.Error("Connection refused")

        val result = service.generateAgendaSuggestions(userId, personId)

        assertTrue(result is AiPrepResult.Error)
        assertEquals("Connection refused", (result as AiPrepResult.Error).message)
    }

    @Test
    fun `should include open action items in context`() {
        val actionItem = ActionItem(
            id = ActionItemId.generate(),
            userId = userId,
            personId = personId,
            title = "Complete code review",
            status = ActionItemStatus.OPEN
        )

        every { personRepository.findByIdAndUserId(personId, userId) } returns person
        every { userSettingsRepository.findByUserId(userId) } returns aiSettings
        every { entryRepository.findAllByUserIdAndPersonId(userId, personId, any()) } returns PageImpl(emptyList())
        every { actionItemRepository.findAllByUserIdAndPersonId(userId, personId, ActionItemStatus.OPEN, any()) } returns PageImpl(listOf(actionItem))
        every { pdpGoalRepository.findAllByUserIdAndPersonId(userId, personId, PdpGoalStatus.ACTIVE, any()) } returns PageImpl(emptyList())
        every { kudosRepository.findAllByUserIdAndPersonId(userId, personId, any()) } returns PageImpl(emptyList())
        every { aiClientPort.chatCompletion(any(), any(), any(), any(), any()) } returns AiCompletionResult.Success(
            "- Follow up on code review"
        )

        val result = service.generateAgendaSuggestions(userId, personId)

        assertTrue(result is AiPrepResult.Success)
        verify { aiClientPort.chatCompletion(any(), any(), any(), any(), match { it.contains("Complete code review") }) }
    }

    @Test
    fun `should include active PDP goals in context`() {
        val goal = PdpGoal(
            id = PdpGoalId.generate(),
            userId = userId,
            personId = personId,
            title = "Learn Kubernetes",
            status = PdpGoalStatus.ACTIVE
        )

        every { personRepository.findByIdAndUserId(personId, userId) } returns person
        every { userSettingsRepository.findByUserId(userId) } returns aiSettings
        every { entryRepository.findAllByUserIdAndPersonId(userId, personId, any()) } returns PageImpl(emptyList())
        every { actionItemRepository.findAllByUserIdAndPersonId(userId, personId, ActionItemStatus.OPEN, any()) } returns PageImpl(emptyList())
        every { pdpGoalRepository.findAllByUserIdAndPersonId(userId, personId, PdpGoalStatus.ACTIVE, any()) } returns PageImpl(listOf(goal))
        every { pdpUpdateRepository.findAllByGoalIdAndUserId(goal.id, userId, any()) } returns PageImpl(emptyList())
        every { kudosRepository.findAllByUserIdAndPersonId(userId, personId, any()) } returns PageImpl(emptyList())
        every { aiClientPort.chatCompletion(any(), any(), any(), any(), any()) } returns AiCompletionResult.Success(
            "- Check progress on Kubernetes learning"
        )

        val result = service.generateAgendaSuggestions(userId, personId)

        assertTrue(result is AiPrepResult.Success)
        verify { aiClientPort.chatCompletion(any(), any(), any(), any(), match { it.contains("Learn Kubernetes") }) }
    }

    @Test
    fun `should exclude sensitive PDP updates in privacy mode`() {
        val goal = PdpGoal(
            id = PdpGoalId.generate(),
            userId = userId,
            personId = personId,
            title = "Improve communication",
            status = PdpGoalStatus.ACTIVE
        )
        val sensitiveUpdate = PdpUpdate(
            id = PdpUpdateId.generate(),
            goalId = goal.id,
            userId = userId,
            textMarkdown = "Confidential feedback from skip-level",
            sensitive = true
        )

        every { personRepository.findByIdAndUserId(personId, userId) } returns person
        every { userSettingsRepository.findByUserId(userId) } returns aiSettings
        every { entryRepository.findAllByUserIdAndPersonId(userId, personId, any()) } returns PageImpl(emptyList())
        every { actionItemRepository.findAllByUserIdAndPersonId(userId, personId, ActionItemStatus.OPEN, any()) } returns PageImpl(emptyList())
        every { pdpGoalRepository.findAllByUserIdAndPersonId(userId, personId, PdpGoalStatus.ACTIVE, any()) } returns PageImpl(listOf(goal))
        every { pdpUpdateRepository.findAllByGoalIdAndUserId(goal.id, userId, any()) } returns PageImpl(listOf(sensitiveUpdate))
        every { kudosRepository.findAllByUserIdAndPersonId(userId, personId, any()) } returns PageImpl(emptyList())
        every { aiClientPort.chatCompletion(any(), any(), any(), any(), any()) } returns AiCompletionResult.Success(
            "- Discuss communication progress"
        )

        val result = service.generateAgendaSuggestions(userId, personId)

        assertTrue(result is AiPrepResult.Success)
        verify { aiClientPort.chatCompletion(any(), any(), any(), any(), match { !it.contains("Confidential feedback") }) }
    }

    @Test
    fun `should enforce userId scoping - throws PersonNotFoundException for wrong user`() {
        val otherUserId = UserId(UUID.randomUUID())
        every { personRepository.findByIdAndUserId(personId, otherUserId) } returns null

        assertThrows(PersonNotFoundException::class.java) {
            service.generateAgendaSuggestions(otherUserId, personId)
        }
    }

    @Test
    fun `should limit suggestions to 5`() {
        val entry = OneOnOneEntry(
            id = OneOnOneEntryId.generate(),
            userId = userId,
            personId = personId,
            meetingDate = Instant.now().minusSeconds(86400),
            notesMarkdown = "Lots of topics discussed",
            sensitive = false
        )

        every { personRepository.findByIdAndUserId(personId, userId) } returns person
        every { userSettingsRepository.findByUserId(userId) } returns aiSettings
        every { entryRepository.findAllByUserIdAndPersonId(userId, personId, any()) } returns PageImpl(listOf(entry))
        every { actionItemRepository.findAllByUserIdAndPersonId(userId, personId, ActionItemStatus.OPEN, any()) } returns PageImpl(emptyList())
        every { pdpGoalRepository.findAllByUserIdAndPersonId(userId, personId, PdpGoalStatus.ACTIVE, any()) } returns PageImpl(emptyList())
        every { kudosRepository.findAllByUserIdAndPersonId(userId, personId, any()) } returns PageImpl(emptyList())
        every { aiClientPort.chatCompletion(any(), any(), any(), any(), any()) } returns AiCompletionResult.Success(
            "- Item 1\n- Item 2\n- Item 3\n- Item 4\n- Item 5\n- Item 6\n- Item 7"
        )

        val result = service.generateAgendaSuggestions(userId, personId)

        assertTrue(result is AiPrepResult.Success)
        assertEquals(5, (result as AiPrepResult.Success).suggestions.size)
    }

    @Test
    fun `should filter out preamble lines from LLM response`() {
        val entry = OneOnOneEntry(
            id = OneOnOneEntryId.generate(),
            userId = userId,
            personId = personId,
            meetingDate = Instant.now().minusSeconds(86400),
            notesMarkdown = "Some notes",
            sensitive = false
        )

        every { personRepository.findByIdAndUserId(personId, userId) } returns person
        every { userSettingsRepository.findByUserId(userId) } returns aiSettings
        every { entryRepository.findAllByUserIdAndPersonId(userId, personId, any()) } returns PageImpl(listOf(entry))
        every { actionItemRepository.findAllByUserIdAndPersonId(userId, personId, ActionItemStatus.OPEN, any()) } returns PageImpl(emptyList())
        every { pdpGoalRepository.findAllByUserIdAndPersonId(userId, personId, PdpGoalStatus.ACTIVE, any()) } returns PageImpl(emptyList())
        every { kudosRepository.findAllByUserIdAndPersonId(userId, personId, any()) } returns PageImpl(emptyList())
        every { aiClientPort.chatCompletion(any(), any(), any(), any(), any()) } returns AiCompletionResult.Success(
            "Here are 3-5 high-impact agenda items:\n\n- Follow up on project timeline\n- Discuss career growth\n- Review blockers\n\nOptional additional suggestion:\n- Celebrate recent kudos"
        )

        val result = service.generateAgendaSuggestions(userId, personId)

        assertTrue(result is AiPrepResult.Success)
        val suggestions = (result as AiPrepResult.Success).suggestions
        assertEquals(4, suggestions.size)
        assertEquals("Follow up on project timeline", suggestions[0])
        assertEquals("Discuss career growth", suggestions[1])
        assertEquals("Review blockers", suggestions[2])
        assertEquals("Celebrate recent kudos", suggestions[3])
    }

    @Test
    fun `should strip markdown formatting from suggestions`() {
        val entry = OneOnOneEntry(
            id = OneOnOneEntryId.generate(),
            userId = userId,
            personId = personId,
            meetingDate = Instant.now().minusSeconds(86400),
            notesMarkdown = "Some notes",
            sensitive = false
        )

        every { personRepository.findByIdAndUserId(personId, userId) } returns person
        every { userSettingsRepository.findByUserId(userId) } returns aiSettings
        every { entryRepository.findAllByUserIdAndPersonId(userId, personId, any()) } returns PageImpl(listOf(entry))
        every { actionItemRepository.findAllByUserIdAndPersonId(userId, personId, ActionItemStatus.OPEN, any()) } returns PageImpl(emptyList())
        every { pdpGoalRepository.findAllByUserIdAndPersonId(userId, personId, PdpGoalStatus.ACTIVE, any()) } returns PageImpl(emptyList())
        every { kudosRepository.findAllByUserIdAndPersonId(userId, personId, any()) } returns PageImpl(emptyList())
        every { aiClientPort.chatCompletion(any(), any(), any(), any(), any()) } returns AiCompletionResult.Success(
            "- **Follow up** on the _project timeline_\n- Check progress on `kubernetes` learning\n- Review [action items](http://example.com)"
        )

        val result = service.generateAgendaSuggestions(userId, personId)

        assertTrue(result is AiPrepResult.Success)
        val suggestions = (result as AiPrepResult.Success).suggestions
        assertEquals(3, suggestions.size)
        assertEquals("Follow up on the project timeline", suggestions[0])
        assertEquals("Check progress on kubernetes learning", suggestions[1])
        assertEquals("Review action items", suggestions[2])
    }

    @Test
    fun `should handle numbered list format from LLM`() {
        val entry = OneOnOneEntry(
            id = OneOnOneEntryId.generate(),
            userId = userId,
            personId = personId,
            meetingDate = Instant.now().minusSeconds(86400),
            notesMarkdown = "Some notes",
            sensitive = false
        )

        every { personRepository.findByIdAndUserId(personId, userId) } returns person
        every { userSettingsRepository.findByUserId(userId) } returns aiSettings
        every { entryRepository.findAllByUserIdAndPersonId(userId, personId, any()) } returns PageImpl(listOf(entry))
        every { actionItemRepository.findAllByUserIdAndPersonId(userId, personId, ActionItemStatus.OPEN, any()) } returns PageImpl(emptyList())
        every { pdpGoalRepository.findAllByUserIdAndPersonId(userId, personId, PdpGoalStatus.ACTIVE, any()) } returns PageImpl(emptyList())
        every { kudosRepository.findAllByUserIdAndPersonId(userId, personId, any()) } returns PageImpl(emptyList())
        every { aiClientPort.chatCompletion(any(), any(), any(), any(), any()) } returns AiCompletionResult.Success(
            "1. Follow up on project\n2) Discuss blockers\n3: Review goals"
        )

        val result = service.generateAgendaSuggestions(userId, personId)

        assertTrue(result is AiPrepResult.Success)
        val suggestions = (result as AiPrepResult.Success).suggestions
        assertEquals(3, suggestions.size)
        assertEquals("Follow up on project", suggestions[0])
        assertEquals("Discuss blockers", suggestions[1])
        assertEquals("Review goals", suggestions[2])
    }
}
