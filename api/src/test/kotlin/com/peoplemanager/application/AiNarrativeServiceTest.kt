package com.peoplemanager.application

import com.peoplemanager.application.port.input.*
import com.peoplemanager.application.port.output.*
import com.peoplemanager.domain.*
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.springframework.data.domain.PageImpl
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class AiNarrativeServiceTest {

    private val userSettingsRepository = mockk<UserSettingsRepository>()
    private val personRepository = mockk<PersonRepository>()
    private val entryRepository = mockk<OneOnOneEntryRepository>()
    private val actionItemRepository = mockk<ActionItemRepository>()
    private val pdpGoalRepository = mockk<PdpGoalRepository>()
    private val pdpUpdateRepository = mockk<PdpUpdateRepository>()
    private val kudosRepository = mockk<KudosRepository>()
    private val aiClientPort = mockk<AiClientPort>()
    private val aiConfigResolver = AiConfigResolver(defaultBaseUrl = "", defaultApiKey = "", defaultModel = "")

    private lateinit var service: AiNarrativeService

    private val userId = UserId(UUID.randomUUID())
    private val personId = PersonId(UUID.randomUUID())
    private val dateFrom = LocalDate.of(2026, 1, 1)
    private val dateTo = LocalDate.of(2026, 6, 30)

    private val person = Person(
        id = personId,
        userId = userId,
        name = "Alice Smith",
        roleTitle = "Senior Engineer"
    )

    private val aiSettings = UserSettings(
        userId = userId,
        aiEnabled = true,
        aiApiBaseUrl = "http://ollama:11434/v1",
        aiApiKey = "test-key",
        aiModelName = "llama3",
        aiPrivacyMode = true,
        aiWritingStyle = AiWritingStyle.NARRATIVE
    )

    @BeforeEach
    fun setUp() {
        service = AiNarrativeService(
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
    fun `should throw PersonNotFoundException when person not found`() {
        every { personRepository.findByIdAndUserId(personId, userId) } returns null

        assertThrows(PersonNotFoundException::class.java) {
            service.generateNarrative(userId, personId, dateFrom, dateTo)
        }
    }

    @Test
    fun `should return error when AI is not configured`() {
        every { personRepository.findByIdAndUserId(personId, userId) } returns person
        every { userSettingsRepository.findByUserId(userId) } returns UserSettings.createDefault(userId)

        val result = service.generateNarrative(userId, personId, dateFrom, dateTo)

        assertTrue(result is AiNarrativeResult.Error)
        assertTrue((result as AiNarrativeResult.Error).message.contains("not configured"))
    }

    @Test
    fun `should return error when no settings exist`() {
        every { personRepository.findByIdAndUserId(personId, userId) } returns person
        every { userSettingsRepository.findByUserId(userId) } returns null

        val result = service.generateNarrative(userId, personId, dateFrom, dateTo)

        assertTrue(result is AiNarrativeResult.Error)
        assertTrue((result as AiNarrativeResult.Error).message.contains("not configured"))
    }

    @Test
    fun `should return error when no data available in date range`() {
        every { personRepository.findByIdAndUserId(personId, userId) } returns person
        every { userSettingsRepository.findByUserId(userId) } returns aiSettings
        every { kudosRepository.findAllByUserIdAndPersonId(userId, personId, any()) } returns PageImpl(emptyList())
        every { pdpGoalRepository.findAllByUserIdAndPersonId(userId, personId, null, any()) } returns PageImpl(emptyList())
        every { entryRepository.findAllByUserIdAndPersonId(userId, personId, any()) } returns PageImpl(emptyList())
        every { actionItemRepository.findAllByUserIdAndPersonId(userId, personId, null, any()) } returns PageImpl(emptyList())

        val result = service.generateNarrative(userId, personId, dateFrom, dateTo)

        assertTrue(result is AiNarrativeResult.Error)
        assertTrue((result as AiNarrativeResult.Error).message.contains("No data"))
    }

    @Test
    fun `should call AI client with context and return narrative on success`() {
        val kudos = Kudos(
            id = KudosId.generate(),
            userId = userId,
            personId = personId,
            date = LocalDate.of(2026, 3, 15),
            text = "Great presentation at the all-hands",
            tags = listOf("impact", "communication")
        )

        every { personRepository.findByIdAndUserId(personId, userId) } returns person
        every { userSettingsRepository.findByUserId(userId) } returns aiSettings
        every { kudosRepository.findAllByUserIdAndPersonId(userId, personId, any()) } returns PageImpl(listOf(kudos))
        every { pdpGoalRepository.findAllByUserIdAndPersonId(userId, personId, null, any()) } returns PageImpl(emptyList())
        every { entryRepository.findAllByUserIdAndPersonId(userId, personId, any()) } returns PageImpl(emptyList())
        every { actionItemRepository.findAllByUserIdAndPersonId(userId, personId, null, any()) } returns PageImpl(emptyList())
        every { aiClientPort.chatCompletion(any(), any(), any(), any(), any()) } returns AiCompletionResult.Success(
            "Alice demonstrated exceptional impact through her all-hands presentation..."
        )

        val result = service.generateNarrative(userId, personId, dateFrom, dateTo)

        assertTrue(result is AiNarrativeResult.Success)
        assertEquals(
            "Alice demonstrated exceptional impact through her all-hands presentation...",
            (result as AiNarrativeResult.Success).narrative
        )

        verify {
            aiClientPort.chatCompletion(
                "http://ollama:11434/v1",
                "test-key",
                "llama3",
                AiNarrativeService.SYSTEM_PROMPT,
                match { it.contains("Great presentation") && it.contains("Alice Smith") }
            )
        }
    }

    @Test
    fun `should include kudos tags in context`() {
        val kudos = Kudos(
            id = KudosId.generate(),
            userId = userId,
            personId = personId,
            date = LocalDate.of(2026, 2, 10),
            text = "Helped onboard new team member",
            tags = listOf("collaboration", "mentoring")
        )

        every { personRepository.findByIdAndUserId(personId, userId) } returns person
        every { userSettingsRepository.findByUserId(userId) } returns aiSettings
        every { kudosRepository.findAllByUserIdAndPersonId(userId, personId, any()) } returns PageImpl(listOf(kudos))
        every { pdpGoalRepository.findAllByUserIdAndPersonId(userId, personId, null, any()) } returns PageImpl(emptyList())
        every { entryRepository.findAllByUserIdAndPersonId(userId, personId, any()) } returns PageImpl(emptyList())
        every { actionItemRepository.findAllByUserIdAndPersonId(userId, personId, null, any()) } returns PageImpl(emptyList())
        every { aiClientPort.chatCompletion(any(), any(), any(), any(), any()) } returns AiCompletionResult.Success("Narrative text")

        service.generateNarrative(userId, personId, dateFrom, dateTo)

        verify {
            aiClientPort.chatCompletion(any(), any(), any(), any(), match {
                it.contains("collaboration") && it.contains("mentoring")
            })
        }
    }

    @Test
    fun `should include PDP goals with status in context`() {
        val goal = PdpGoal(
            id = PdpGoalId.generate(),
            userId = userId,
            personId = personId,
            title = "Learn Kubernetes",
            status = PdpGoalStatus.ACHIEVED,
            createdAt = Instant.parse("2026-02-01T10:00:00Z")
        )

        every { personRepository.findByIdAndUserId(personId, userId) } returns person
        every { userSettingsRepository.findByUserId(userId) } returns aiSettings
        every { kudosRepository.findAllByUserIdAndPersonId(userId, personId, any()) } returns PageImpl(emptyList())
        every { pdpGoalRepository.findAllByUserIdAndPersonId(userId, personId, null, any()) } returns PageImpl(listOf(goal))
        every { pdpUpdateRepository.findAllByGoalIdAndUserId(goal.id, userId, any()) } returns PageImpl(emptyList())
        every { entryRepository.findAllByUserIdAndPersonId(userId, personId, any()) } returns PageImpl(emptyList())
        every { actionItemRepository.findAllByUserIdAndPersonId(userId, personId, null, any()) } returns PageImpl(emptyList())
        every { aiClientPort.chatCompletion(any(), any(), any(), any(), any()) } returns AiCompletionResult.Success("Narrative text")

        service.generateNarrative(userId, personId, dateFrom, dateTo)

        verify {
            aiClientPort.chatCompletion(any(), any(), any(), any(), match {
                it.contains("Learn Kubernetes") && it.contains("ACHIEVED")
            })
        }
    }

    @Test
    fun `should include 1-1 outcomes in context`() {
        val entry = OneOnOneEntry(
            id = OneOnOneEntryId.generate(),
            userId = userId,
            personId = personId,
            meetingDate = Instant.parse("2026-03-10T14:00:00Z"),
            outcomesMarkdown = "Agreed to lead the migration project",
            sensitive = false
        )

        every { personRepository.findByIdAndUserId(personId, userId) } returns person
        every { userSettingsRepository.findByUserId(userId) } returns aiSettings
        every { kudosRepository.findAllByUserIdAndPersonId(userId, personId, any()) } returns PageImpl(emptyList())
        every { pdpGoalRepository.findAllByUserIdAndPersonId(userId, personId, null, any()) } returns PageImpl(emptyList())
        every { entryRepository.findAllByUserIdAndPersonId(userId, personId, any()) } returns PageImpl(listOf(entry))
        every { actionItemRepository.findAllByUserIdAndPersonId(userId, personId, null, any()) } returns PageImpl(emptyList())
        every { aiClientPort.chatCompletion(any(), any(), any(), any(), any()) } returns AiCompletionResult.Success("Narrative text")

        service.generateNarrative(userId, personId, dateFrom, dateTo)

        verify {
            aiClientPort.chatCompletion(any(), any(), any(), any(), match {
                it.contains("Agreed to lead the migration project")
            })
        }
    }

    @Test
    fun `should include action item completion stats in context`() {
        val completedItem = ActionItem(
            id = ActionItemId.generate(),
            userId = userId,
            personId = personId,
            title = "Deploy new service",
            status = ActionItemStatus.DONE,
            createdAt = Instant.parse("2026-02-15T10:00:00Z")
        )
        val openItem = ActionItem(
            id = ActionItemId.generate(),
            userId = userId,
            personId = personId,
            title = "Write documentation",
            status = ActionItemStatus.OPEN,
            createdAt = Instant.parse("2026-03-01T10:00:00Z")
        )

        every { personRepository.findByIdAndUserId(personId, userId) } returns person
        every { userSettingsRepository.findByUserId(userId) } returns aiSettings
        every { kudosRepository.findAllByUserIdAndPersonId(userId, personId, any()) } returns PageImpl(emptyList())
        every { pdpGoalRepository.findAllByUserIdAndPersonId(userId, personId, null, any()) } returns PageImpl(emptyList())
        every { entryRepository.findAllByUserIdAndPersonId(userId, personId, any()) } returns PageImpl(emptyList())
        every { actionItemRepository.findAllByUserIdAndPersonId(userId, personId, null, any()) } returns PageImpl(
            listOf(completedItem, openItem)
        )
        every { aiClientPort.chatCompletion(any(), any(), any(), any(), any()) } returns AiCompletionResult.Success("Narrative text")

        service.generateNarrative(userId, personId, dateFrom, dateTo)

        verify {
            aiClientPort.chatCompletion(any(), any(), any(), any(), match {
                it.contains("2 total action items") && it.contains("1 completed") && it.contains("Deploy new service")
            })
        }
    }

    @Test
    fun `should exclude sensitive 1-1 entries in privacy mode`() {
        val sensitiveEntry = OneOnOneEntry(
            id = OneOnOneEntryId.generate(),
            userId = userId,
            personId = personId,
            meetingDate = Instant.parse("2026-04-01T14:00:00Z"),
            outcomesMarkdown = "Discussed PIP details",
            sensitive = true
        )
        val normalEntry = OneOnOneEntry(
            id = OneOnOneEntryId.generate(),
            userId = userId,
            personId = personId,
            meetingDate = Instant.parse("2026-04-15T14:00:00Z"),
            outcomesMarkdown = "Sprint planning outcomes",
            sensitive = false
        )

        every { personRepository.findByIdAndUserId(personId, userId) } returns person
        every { userSettingsRepository.findByUserId(userId) } returns aiSettings
        every { kudosRepository.findAllByUserIdAndPersonId(userId, personId, any()) } returns PageImpl(emptyList())
        every { pdpGoalRepository.findAllByUserIdAndPersonId(userId, personId, null, any()) } returns PageImpl(emptyList())
        every { entryRepository.findAllByUserIdAndPersonId(userId, personId, any()) } returns PageImpl(
            listOf(sensitiveEntry, normalEntry)
        )
        every { actionItemRepository.findAllByUserIdAndPersonId(userId, personId, null, any()) } returns PageImpl(emptyList())
        every { aiClientPort.chatCompletion(any(), any(), any(), any(), any()) } returns AiCompletionResult.Success("Narrative text")

        service.generateNarrative(userId, personId, dateFrom, dateTo)

        verify {
            aiClientPort.chatCompletion(any(), any(), any(), any(), match {
                !it.contains("PIP details") && it.contains("Sprint planning outcomes")
            })
        }
    }

    @Test
    fun `should include sensitive entries when privacy mode is off`() {
        val settingsNoPrivacy = aiSettings.copy(aiPrivacyMode = false)
        val sensitiveEntry = OneOnOneEntry(
            id = OneOnOneEntryId.generate(),
            userId = userId,
            personId = personId,
            meetingDate = Instant.parse("2026-04-01T14:00:00Z"),
            outcomesMarkdown = "Discussed PIP details",
            sensitive = true
        )

        every { personRepository.findByIdAndUserId(personId, userId) } returns person
        every { userSettingsRepository.findByUserId(userId) } returns settingsNoPrivacy
        every { kudosRepository.findAllByUserIdAndPersonId(userId, personId, any()) } returns PageImpl(emptyList())
        every { pdpGoalRepository.findAllByUserIdAndPersonId(userId, personId, null, any()) } returns PageImpl(emptyList())
        every { entryRepository.findAllByUserIdAndPersonId(userId, personId, any()) } returns PageImpl(listOf(sensitiveEntry))
        every { actionItemRepository.findAllByUserIdAndPersonId(userId, personId, null, any()) } returns PageImpl(emptyList())
        every { aiClientPort.chatCompletion(any(), any(), any(), any(), any()) } returns AiCompletionResult.Success("Narrative text")

        service.generateNarrative(userId, personId, dateFrom, dateTo)

        verify {
            aiClientPort.chatCompletion(any(), any(), any(), any(), match { it.contains("PIP details") })
        }
    }

    @Test
    fun `should exclude sensitive PDP updates in privacy mode`() {
        val goal = PdpGoal(
            id = PdpGoalId.generate(),
            userId = userId,
            personId = personId,
            title = "Improve communication",
            status = PdpGoalStatus.ACTIVE,
            createdAt = Instant.parse("2026-02-01T10:00:00Z")
        )
        val sensitiveUpdate = PdpUpdate(
            id = PdpUpdateId.generate(),
            goalId = goal.id,
            userId = userId,
            textMarkdown = "Confidential feedback from skip-level",
            sensitive = true,
            createdAt = Instant.parse("2026-03-01T10:00:00Z")
        )
        val normalUpdate = PdpUpdate(
            id = PdpUpdateId.generate(),
            goalId = goal.id,
            userId = userId,
            textMarkdown = "Completed public speaking course",
            sensitive = false,
            createdAt = Instant.parse("2026-03-15T10:00:00Z")
        )

        every { personRepository.findByIdAndUserId(personId, userId) } returns person
        every { userSettingsRepository.findByUserId(userId) } returns aiSettings
        every { kudosRepository.findAllByUserIdAndPersonId(userId, personId, any()) } returns PageImpl(emptyList())
        every { pdpGoalRepository.findAllByUserIdAndPersonId(userId, personId, null, any()) } returns PageImpl(listOf(goal))
        every { pdpUpdateRepository.findAllByGoalIdAndUserId(goal.id, userId, any()) } returns PageImpl(
            listOf(sensitiveUpdate, normalUpdate)
        )
        every { entryRepository.findAllByUserIdAndPersonId(userId, personId, any()) } returns PageImpl(emptyList())
        every { actionItemRepository.findAllByUserIdAndPersonId(userId, personId, null, any()) } returns PageImpl(emptyList())
        every { aiClientPort.chatCompletion(any(), any(), any(), any(), any()) } returns AiCompletionResult.Success("Narrative text")

        service.generateNarrative(userId, personId, dateFrom, dateTo)

        verify {
            aiClientPort.chatCompletion(any(), any(), any(), any(), match {
                !it.contains("Confidential feedback") && it.contains("Completed public speaking course")
            })
        }
    }

    @Test
    fun `should return error when AI client fails`() {
        val kudos = Kudos(
            id = KudosId.generate(),
            userId = userId,
            personId = personId,
            date = LocalDate.of(2026, 3, 15),
            text = "Good work"
        )

        every { personRepository.findByIdAndUserId(personId, userId) } returns person
        every { userSettingsRepository.findByUserId(userId) } returns aiSettings
        every { kudosRepository.findAllByUserIdAndPersonId(userId, personId, any()) } returns PageImpl(listOf(kudos))
        every { pdpGoalRepository.findAllByUserIdAndPersonId(userId, personId, null, any()) } returns PageImpl(emptyList())
        every { entryRepository.findAllByUserIdAndPersonId(userId, personId, any()) } returns PageImpl(emptyList())
        every { actionItemRepository.findAllByUserIdAndPersonId(userId, personId, null, any()) } returns PageImpl(emptyList())
        every { aiClientPort.chatCompletion(any(), any(), any(), any(), any()) } returns AiCompletionResult.Error("Connection refused")

        val result = service.generateNarrative(userId, personId, dateFrom, dateTo)

        assertTrue(result is AiNarrativeResult.Error)
        assertEquals("Connection refused", (result as AiNarrativeResult.Error).message)
    }

    @Test
    fun `should enforce userId scoping - throws PersonNotFoundException for wrong user`() {
        val otherUserId = UserId(UUID.randomUUID())
        every { personRepository.findByIdAndUserId(personId, otherUserId) } returns null

        assertThrows(PersonNotFoundException::class.java) {
            service.generateNarrative(otherUserId, personId, dateFrom, dateTo)
        }
    }

    @Test
    fun `should filter data outside date range`() {
        val kudosOutOfRange = Kudos(
            id = KudosId.generate(),
            userId = userId,
            personId = personId,
            date = LocalDate.of(2025, 12, 15), // Before dateFrom
            text = "Old kudos"
        )
        val kudosInRange = Kudos(
            id = KudosId.generate(),
            userId = userId,
            personId = personId,
            date = LocalDate.of(2026, 3, 15),
            text = "Recent kudos"
        )

        every { personRepository.findByIdAndUserId(personId, userId) } returns person
        every { userSettingsRepository.findByUserId(userId) } returns aiSettings
        every { kudosRepository.findAllByUserIdAndPersonId(userId, personId, any()) } returns PageImpl(
            listOf(kudosOutOfRange, kudosInRange)
        )
        every { pdpGoalRepository.findAllByUserIdAndPersonId(userId, personId, null, any()) } returns PageImpl(emptyList())
        every { entryRepository.findAllByUserIdAndPersonId(userId, personId, any()) } returns PageImpl(emptyList())
        every { actionItemRepository.findAllByUserIdAndPersonId(userId, personId, null, any()) } returns PageImpl(emptyList())
        every { aiClientPort.chatCompletion(any(), any(), any(), any(), any()) } returns AiCompletionResult.Success("Narrative text")

        service.generateNarrative(userId, personId, dateFrom, dateTo)

        verify {
            aiClientPort.chatCompletion(any(), any(), any(), any(), match {
                !it.contains("Old kudos") && it.contains("Recent kudos")
            })
        }
    }

    @Test
    fun `should use BULLET_POINTS style when configured`() {
        val bulletSettings = aiSettings.copy(aiWritingStyle = AiWritingStyle.BULLET_POINTS)
        val kudos = Kudos(
            id = KudosId.generate(),
            userId = userId,
            personId = personId,
            date = LocalDate.of(2026, 3, 15),
            text = "Good work"
        )

        every { personRepository.findByIdAndUserId(personId, userId) } returns person
        every { userSettingsRepository.findByUserId(userId) } returns bulletSettings
        every { kudosRepository.findAllByUserIdAndPersonId(userId, personId, any()) } returns PageImpl(listOf(kudos))
        every { pdpGoalRepository.findAllByUserIdAndPersonId(userId, personId, null, any()) } returns PageImpl(emptyList())
        every { entryRepository.findAllByUserIdAndPersonId(userId, personId, any()) } returns PageImpl(emptyList())
        every { actionItemRepository.findAllByUserIdAndPersonId(userId, personId, null, any()) } returns PageImpl(emptyList())
        every { aiClientPort.chatCompletion(any(), any(), any(), any(), any()) } returns AiCompletionResult.Success("- Achievement 1\n- Growth 1")

        service.generateNarrative(userId, personId, dateFrom, dateTo)

        verify {
            aiClientPort.chatCompletion(any(), any(), any(), any(), match {
                it.contains("bullet points")
            })
        }
    }

    @Test
    fun `should use CONCISE style when configured`() {
        val conciseSettings = aiSettings.copy(aiWritingStyle = AiWritingStyle.CONCISE)
        val kudos = Kudos(
            id = KudosId.generate(),
            userId = userId,
            personId = personId,
            date = LocalDate.of(2026, 3, 15),
            text = "Good work"
        )

        every { personRepository.findByIdAndUserId(personId, userId) } returns person
        every { userSettingsRepository.findByUserId(userId) } returns conciseSettings
        every { kudosRepository.findAllByUserIdAndPersonId(userId, personId, any()) } returns PageImpl(listOf(kudos))
        every { pdpGoalRepository.findAllByUserIdAndPersonId(userId, personId, null, any()) } returns PageImpl(emptyList())
        every { entryRepository.findAllByUserIdAndPersonId(userId, personId, any()) } returns PageImpl(emptyList())
        every { actionItemRepository.findAllByUserIdAndPersonId(userId, personId, null, any()) } returns PageImpl(emptyList())
        every { aiClientPort.chatCompletion(any(), any(), any(), any(), any()) } returns AiCompletionResult.Success("Concise summary.")

        service.generateNarrative(userId, personId, dateFrom, dateTo)

        verify {
            aiClientPort.chatCompletion(any(), any(), any(), any(), match {
                it.contains("concise") && it.contains("1-paragraph")
            })
        }
    }
}
