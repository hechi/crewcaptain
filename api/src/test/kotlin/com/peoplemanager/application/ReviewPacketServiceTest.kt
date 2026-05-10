package com.peoplemanager.application

import com.peoplemanager.application.ports.ActionItemRepository
import com.peoplemanager.application.ports.KudosRepository
import com.peoplemanager.application.ports.OneOnOneEntryRepository
import com.peoplemanager.application.ports.PdpGoalRepository
import com.peoplemanager.application.ports.PdpUpdateRepository
import com.peoplemanager.application.ports.PersonRepository
import com.peoplemanager.application.queries.GenerateReviewPacketQuery
import com.peoplemanager.domain.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class ReviewPacketServiceTest {

    private lateinit var personRepository: PersonRepository
    private lateinit var oneOnOneEntryRepository: OneOnOneEntryRepository
    private lateinit var actionItemRepository: ActionItemRepository
    private lateinit var pdpGoalRepository: PdpGoalRepository
    private lateinit var pdpUpdateRepository: PdpUpdateRepository
    private lateinit var kudosRepository: KudosRepository
    private lateinit var service: ReviewPacketService

    private val userId = UserId.generate()
    private val personId = PersonId.generate()
    private val dateFrom = LocalDate.of(2024, 1, 1)
    private val dateTo = LocalDate.of(2024, 6, 30)

    @BeforeEach
    fun setUp() {
        personRepository = mockk()
        oneOnOneEntryRepository = mockk()
        actionItemRepository = mockk()
        pdpGoalRepository = mockk()
        pdpUpdateRepository = mockk()
        kudosRepository = mockk()
        service = ReviewPacketService(
            personRepository, oneOnOneEntryRepository, actionItemRepository,
            pdpGoalRepository, pdpUpdateRepository, kudosRepository
        )
    }

    private fun createTestPerson() = Person(
        id = personId,
        userId = userId,
        name = "Jane Smith",
        preferredName = "Jane",
        roleTitle = "Senior Engineer",
        moraleStatus = MoraleStatus.GREEN,
        moraleNote = "Doing great"
    )

    private fun setupEmptyRepositories() {
        every { oneOnOneEntryRepository.findAllByUserIdAndPersonId(userId, personId, any()) } returns
            PageImpl(emptyList())
        every { actionItemRepository.findAllByUserIdAndPersonId(userId, personId, null, any()) } returns
            PageImpl(emptyList())
        every { pdpGoalRepository.findAllByUserIdAndPersonId(userId, personId, null, any()) } returns
            PageImpl(emptyList())
        every { kudosRepository.findAllByUserIdAndPersonId(userId, personId, any()) } returns
            PageImpl(emptyList())
    }

    private fun createQuery() = GenerateReviewPacketQuery(
        userId = userId,
        personId = personId,
        dateFrom = dateFrom,
        dateTo = dateTo
    )

    @Nested
    inner class GenerateReviewPacketTests {

        @Test
        fun `should throw PersonNotFoundException when person does not exist`() {
            every { personRepository.findByIdAndUserId(personId, userId) } returns null

            shouldThrow<PersonNotFoundException> {
                service.generateReviewPacket(createQuery())
            }
        }

        @Test
        fun `should return markdown with review packet title`() {
            every { personRepository.findByIdAndUserId(personId, userId) } returns createTestPerson()
            setupEmptyRepositories()

            val result = service.generateReviewPacket(createQuery())

            result shouldContain "# Review Packet: Jane Smith"
        }

        @Test
        fun `should include date range in output`() {
            every { personRepository.findByIdAndUserId(personId, userId) } returns createTestPerson()
            setupEmptyRepositories()

            val result = service.generateReviewPacket(createQuery())

            result shouldContain "2024-01-01 to 2024-06-30"
        }

        @Test
        fun `should include executive summary section`() {
            every { personRepository.findByIdAndUserId(personId, userId) } returns createTestPerson()
            setupEmptyRepositories()

            val result = service.generateReviewPacket(createQuery())

            result shouldContain "## Executive Summary"
        }

        @Test
        fun `should filter one-on-one entries by date range`() {
            every { personRepository.findByIdAndUserId(personId, userId) } returns createTestPerson()

            val inRangeEntry = OneOnOneEntry(
                id = OneOnOneEntryId.generate(), userId = userId, personId = personId,
                meetingDate = LocalDate.of(2024, 3, 15).atStartOfDay().toInstant(ZoneOffset.UTC),
                agendaItems = emptyList(), sensitive = false,
                notesMarkdown = "In range meeting"
            )
            val outOfRangeEntry = OneOnOneEntry(
                id = OneOnOneEntryId.generate(), userId = userId, personId = personId,
                meetingDate = LocalDate.of(2023, 12, 1).atStartOfDay().toInstant(ZoneOffset.UTC),
                agendaItems = emptyList(), sensitive = false,
                notesMarkdown = "Out of range meeting"
            )

            every { oneOnOneEntryRepository.findAllByUserIdAndPersonId(userId, personId, any()) } returns
                PageImpl(listOf(inRangeEntry, outOfRangeEntry))
            every { actionItemRepository.findAllByUserIdAndPersonId(userId, personId, null, any()) } returns
                PageImpl(emptyList())
            every { pdpGoalRepository.findAllByUserIdAndPersonId(userId, personId, null, any()) } returns
                PageImpl(emptyList())
            every { kudosRepository.findAllByUserIdAndPersonId(userId, personId, any()) } returns
                PageImpl(emptyList())

            val result = service.generateReviewPacket(createQuery())

            result shouldContain "In range meeting"
            result shouldNotContain "Out of range meeting"
        }

        @Test
        fun `should filter action items by date range`() {
            every { personRepository.findByIdAndUserId(personId, userId) } returns createTestPerson()

            val inRangeItem = ActionItem(
                id = ActionItemId.generate(), userId = userId, personId = personId,
                title = "In range task", status = ActionItemStatus.DONE,
                createdAt = LocalDate.of(2024, 2, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
            )
            val outOfRangeItem = ActionItem(
                id = ActionItemId.generate(), userId = userId, personId = personId,
                title = "Out of range task", status = ActionItemStatus.OPEN,
                createdAt = LocalDate.of(2023, 11, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
            )

            every { oneOnOneEntryRepository.findAllByUserIdAndPersonId(userId, personId, any()) } returns
                PageImpl(emptyList())
            every { actionItemRepository.findAllByUserIdAndPersonId(userId, personId, null, any()) } returns
                PageImpl(listOf(inRangeItem, outOfRangeItem))
            every { pdpGoalRepository.findAllByUserIdAndPersonId(userId, personId, null, any()) } returns
                PageImpl(emptyList())
            every { kudosRepository.findAllByUserIdAndPersonId(userId, personId, any()) } returns
                PageImpl(emptyList())

            val result = service.generateReviewPacket(createQuery())

            result shouldContain "In range task"
            result shouldNotContain "Out of range task"
        }

        @Test
        fun `should filter PDP goals by date range`() {
            every { personRepository.findByIdAndUserId(personId, userId) } returns createTestPerson()

            val inRangeGoal = PdpGoal(
                id = PdpGoalId.generate(), userId = userId, personId = personId,
                title = "In range goal", status = PdpGoalStatus.ACTIVE,
                createdAt = LocalDate.of(2024, 4, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
            )
            val outOfRangeGoal = PdpGoal(
                id = PdpGoalId.generate(), userId = userId, personId = personId,
                title = "Out of range goal", status = PdpGoalStatus.ACTIVE,
                createdAt = LocalDate.of(2023, 6, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
            )

            every { oneOnOneEntryRepository.findAllByUserIdAndPersonId(userId, personId, any()) } returns
                PageImpl(emptyList())
            every { actionItemRepository.findAllByUserIdAndPersonId(userId, personId, null, any()) } returns
                PageImpl(emptyList())
            every { pdpGoalRepository.findAllByUserIdAndPersonId(userId, personId, null, any()) } returns
                PageImpl(listOf(inRangeGoal, outOfRangeGoal))
            every { pdpUpdateRepository.findAllByGoalIdAndUserId(inRangeGoal.id, userId, any()) } returns
                PageImpl(emptyList())
            every { kudosRepository.findAllByUserIdAndPersonId(userId, personId, any()) } returns
                PageImpl(emptyList())

            val result = service.generateReviewPacket(createQuery())

            result shouldContain "In range goal"
            result shouldNotContain "Out of range goal"
        }

        @Test
        fun `should filter kudos by date range`() {
            every { personRepository.findByIdAndUserId(personId, userId) } returns createTestPerson()

            val inRangeKudos = Kudos(
                id = KudosId.generate(), userId = userId, personId = personId,
                date = LocalDate.of(2024, 5, 1), text = "In range kudos", tags = emptyList()
            )
            val outOfRangeKudos = Kudos(
                id = KudosId.generate(), userId = userId, personId = personId,
                date = LocalDate.of(2023, 10, 1), text = "Out of range kudos", tags = emptyList()
            )

            every { oneOnOneEntryRepository.findAllByUserIdAndPersonId(userId, personId, any()) } returns
                PageImpl(emptyList())
            every { actionItemRepository.findAllByUserIdAndPersonId(userId, personId, null, any()) } returns
                PageImpl(emptyList())
            every { pdpGoalRepository.findAllByUserIdAndPersonId(userId, personId, null, any()) } returns
                PageImpl(emptyList())
            every { kudosRepository.findAllByUserIdAndPersonId(userId, personId, any()) } returns
                PageImpl(listOf(inRangeKudos, outOfRangeKudos))

            val result = service.generateReviewPacket(createQuery())

            result shouldContain "In range kudos"
            result shouldNotContain "Out of range kudos"
        }

        @Test
        fun `should filter PDP updates by date range`() {
            every { personRepository.findByIdAndUserId(personId, userId) } returns createTestPerson()

            val goalId = PdpGoalId.generate()
            val goal = PdpGoal(
                id = goalId, userId = userId, personId = personId,
                title = "Test goal", status = PdpGoalStatus.ACTIVE,
                createdAt = LocalDate.of(2024, 2, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
            )

            val inRangeUpdate = PdpUpdate(
                id = PdpUpdateId.generate(), goalId = goalId, userId = userId,
                textMarkdown = "In range update", sensitive = false,
                createdAt = LocalDate.of(2024, 3, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
            )
            val outOfRangeUpdate = PdpUpdate(
                id = PdpUpdateId.generate(), goalId = goalId, userId = userId,
                textMarkdown = "Out of range update", sensitive = false,
                createdAt = LocalDate.of(2023, 11, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
            )

            every { oneOnOneEntryRepository.findAllByUserIdAndPersonId(userId, personId, any()) } returns
                PageImpl(emptyList())
            every { actionItemRepository.findAllByUserIdAndPersonId(userId, personId, null, any()) } returns
                PageImpl(emptyList())
            every { pdpGoalRepository.findAllByUserIdAndPersonId(userId, personId, null, any()) } returns
                PageImpl(listOf(goal))
            every { pdpUpdateRepository.findAllByGoalIdAndUserId(goalId, userId, any()) } returns
                PageImpl(listOf(inRangeUpdate, outOfRangeUpdate))
            every { kudosRepository.findAllByUserIdAndPersonId(userId, personId, any()) } returns
                PageImpl(emptyList())

            val result = service.generateReviewPacket(createQuery())

            result shouldContain "In range update"
            result shouldNotContain "Out of range update"
        }

        @Test
        fun `should scope all queries by userId`() {
            every { personRepository.findByIdAndUserId(personId, userId) } returns createTestPerson()
            setupEmptyRepositories()

            service.generateReviewPacket(createQuery())

            verify { personRepository.findByIdAndUserId(personId, userId) }
            verify { oneOnOneEntryRepository.findAllByUserIdAndPersonId(userId, personId, any()) }
            verify { actionItemRepository.findAllByUserIdAndPersonId(userId, personId, null, any()) }
            verify { pdpGoalRepository.findAllByUserIdAndPersonId(userId, personId, null, any()) }
            verify { kudosRepository.findAllByUserIdAndPersonId(userId, personId, any()) }
        }

        @Test
        fun `should include items on boundary dates`() {
            every { personRepository.findByIdAndUserId(personId, userId) } returns createTestPerson()

            val startBoundaryItem = ActionItem(
                id = ActionItemId.generate(), userId = userId, personId = personId,
                title = "Start boundary", status = ActionItemStatus.DONE,
                createdAt = dateFrom.atStartOfDay().toInstant(ZoneOffset.UTC)
            )
            val endBoundaryItem = ActionItem(
                id = ActionItemId.generate(), userId = userId, personId = personId,
                title = "End boundary", status = ActionItemStatus.DONE,
                createdAt = dateTo.atStartOfDay().toInstant(ZoneOffset.UTC)
            )

            every { oneOnOneEntryRepository.findAllByUserIdAndPersonId(userId, personId, any()) } returns
                PageImpl(emptyList())
            every { actionItemRepository.findAllByUserIdAndPersonId(userId, personId, null, any()) } returns
                PageImpl(listOf(startBoundaryItem, endBoundaryItem))
            every { pdpGoalRepository.findAllByUserIdAndPersonId(userId, personId, null, any()) } returns
                PageImpl(emptyList())
            every { kudosRepository.findAllByUserIdAndPersonId(userId, personId, any()) } returns
                PageImpl(emptyList())

            val result = service.generateReviewPacket(createQuery())

            result shouldContain "Start boundary"
            result shouldContain "End boundary"
        }

        @Test
        fun `should compute correct summary statistics`() {
            every { personRepository.findByIdAndUserId(personId, userId) } returns createTestPerson()

            val entries = listOf(
                OneOnOneEntry(
                    id = OneOnOneEntryId.generate(), userId = userId, personId = personId,
                    meetingDate = LocalDate.of(2024, 2, 1).atStartOfDay().toInstant(ZoneOffset.UTC),
                    agendaItems = emptyList(), sensitive = false
                ),
                OneOnOneEntry(
                    id = OneOnOneEntryId.generate(), userId = userId, personId = personId,
                    meetingDate = LocalDate.of(2024, 3, 1).atStartOfDay().toInstant(ZoneOffset.UTC),
                    agendaItems = emptyList(), sensitive = false
                )
            )
            val items = listOf(
                ActionItem(
                    id = ActionItemId.generate(), userId = userId, personId = personId,
                    title = "Done 1", status = ActionItemStatus.DONE,
                    createdAt = LocalDate.of(2024, 2, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
                ),
                ActionItem(
                    id = ActionItemId.generate(), userId = userId, personId = personId,
                    title = "Done 2", status = ActionItemStatus.DONE,
                    createdAt = LocalDate.of(2024, 3, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
                ),
                ActionItem(
                    id = ActionItemId.generate(), userId = userId, personId = personId,
                    title = "Open 1", status = ActionItemStatus.OPEN,
                    createdAt = LocalDate.of(2024, 4, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
                )
            )

            every { oneOnOneEntryRepository.findAllByUserIdAndPersonId(userId, personId, any()) } returns
                PageImpl(entries)
            every { actionItemRepository.findAllByUserIdAndPersonId(userId, personId, null, any()) } returns
                PageImpl(items)
            every { pdpGoalRepository.findAllByUserIdAndPersonId(userId, personId, null, any()) } returns
                PageImpl(emptyList())
            every { kudosRepository.findAllByUserIdAndPersonId(userId, personId, any()) } returns
                PageImpl(emptyList())

            val result = service.generateReviewPacket(createQuery())

            result shouldContain "| 1:1 Meetings | 2 |"
            result shouldContain "| Action Items Created | 3 |"
            result shouldContain "| Action Items Completed | 2 |"
            result shouldContain "| Completion Rate | 66% |"
        }
    }
}
