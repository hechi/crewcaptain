package com.peoplemanager.application

import com.peoplemanager.application.ports.ActionItemRepository
import com.peoplemanager.application.ports.OneOnOneEntryRepository
import com.peoplemanager.application.ports.OneOnOneSeriesRepository
import com.peoplemanager.application.ports.PersonRepository
import com.peoplemanager.application.queries.GetDashboardQuery
import com.peoplemanager.domain.*
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

class DashboardServiceTest {

    private val personRepository = mockk<PersonRepository>()
    private val actionItemRepository = mockk<ActionItemRepository>()
    private val oneOnOneSeriesRepository = mockk<OneOnOneSeriesRepository>()
    private val oneOnOneEntryRepository = mockk<OneOnOneEntryRepository>()

    private val service = DashboardService(
        personRepository, actionItemRepository, oneOnOneSeriesRepository, oneOnOneEntryRepository
    )

    private val userId = UserId.generate()
    private val personId1 = PersonId.generate()
    private val personId2 = PersonId.generate()

    private val person1 = Person(
        id = personId1,
        userId = userId,
        name = "Alice Smith",
        startDate = LocalDate.now().minusYears(2).plusDays(5) // Anniversary in 5 days
    )

    private val person2 = Person(
        id = personId2,
        userId = userId,
        name = "Bob Jones",
        startDate = LocalDate.now().minusYears(1).plusDays(60) // Anniversary in 60 days (outside window)
    )

    @BeforeEach
    fun setup() {
        clearAllMocks()
    }

    @Nested
    inner class GetDashboardTests {

        @Test
        fun `should return empty dashboard when no data exists`() {
            every { personRepository.findAllByUserIdUnpaged(userId) } returns emptyList()
            every { actionItemRepository.findOverdueByUserId(userId, any(), any()) } returns PageImpl(emptyList())
            every { actionItemRepository.findDueSoonByUserId(userId, any(), any()) } returns emptyList()
            every { oneOnOneSeriesRepository.findAllByUserId(userId) } returns emptyList()

            val query = GetDashboardQuery(userId = userId)
            val result = service.getDashboard(query)

            result.overdueActionItems.shouldBeEmpty()
            result.dueSoonActionItems.shouldBeEmpty()
            result.staleOneOnOnes.shouldBeEmpty()
            result.upcomingAnniversaries.shouldBeEmpty()
        }

        @Test
        fun `should return overdue action items with person names`() {
            val actionItem = ActionItem(
                id = ActionItemId.generate(),
                userId = userId,
                personId = personId1,
                title = "Overdue task",
                dueDate = LocalDate.now().minusDays(3),
                ownerType = ActionItemOwnerType.PERSON
            )

            every { personRepository.findAllByUserIdUnpaged(userId) } returns listOf(person1, person2)
            every { actionItemRepository.findOverdueByUserId(userId, any(), any()) } returns PageImpl(listOf(actionItem))
            every { actionItemRepository.findDueSoonByUserId(userId, any(), any()) } returns emptyList()
            every { oneOnOneSeriesRepository.findAllByUserId(userId) } returns emptyList()

            val query = GetDashboardQuery(userId = userId)
            val result = service.getDashboard(query)

            result.overdueActionItems shouldHaveSize 1
            result.overdueActionItems[0].title shouldBe "Overdue task"
            result.overdueActionItems[0].personName shouldBe "Alice Smith"
            result.overdueActionItems[0].ownerType shouldBe ActionItemOwnerType.PERSON
        }

        @Test
        fun `should return due soon action items`() {
            val actionItem = ActionItem(
                id = ActionItemId.generate(),
                userId = userId,
                personId = personId2,
                title = "Due tomorrow",
                dueDate = LocalDate.now().plusDays(1),
                ownerType = ActionItemOwnerType.MANAGER
            )

            every { personRepository.findAllByUserIdUnpaged(userId) } returns listOf(person1, person2)
            every { actionItemRepository.findOverdueByUserId(userId, any(), any()) } returns PageImpl(emptyList())
            every { actionItemRepository.findDueSoonByUserId(userId, any(), any()) } returns listOf(actionItem)
            every { oneOnOneSeriesRepository.findAllByUserId(userId) } returns emptyList()

            val query = GetDashboardQuery(userId = userId, dueSoonDays = 3)
            val result = service.getDashboard(query)

            result.dueSoonActionItems shouldHaveSize 1
            result.dueSoonActionItems[0].title shouldBe "Due tomorrow"
            result.dueSoonActionItems[0].personName shouldBe "Bob Jones"
            result.dueSoonActionItems[0].ownerType shouldBe ActionItemOwnerType.MANAGER
        }

        @Test
        fun `should return stale 1-on-1 reminders when last meeting exceeds cadence`() {
            val series = OneOnOneSeries(
                id = OneOnOneSeriesId.generate(),
                userId = userId,
                personId = personId1,
                cadenceType = CadenceType.WEEKLY
            )
            val lastMeeting = Instant.now().minus(10, ChronoUnit.DAYS)

            every { personRepository.findAllByUserIdUnpaged(userId) } returns listOf(person1, person2)
            every { actionItemRepository.findOverdueByUserId(userId, any(), any()) } returns PageImpl(emptyList())
            every { actionItemRepository.findDueSoonByUserId(userId, any(), any()) } returns emptyList()
            every { oneOnOneSeriesRepository.findAllByUserId(userId) } returns listOf(series)
            every { oneOnOneEntryRepository.findLatestMeetingDate(userId, personId1) } returns lastMeeting

            val query = GetDashboardQuery(userId = userId)
            val result = service.getDashboard(query)

            result.staleOneOnOnes shouldHaveSize 1
            result.staleOneOnOnes[0].personName shouldBe "Alice Smith"
            result.staleOneOnOnes[0].cadenceType shouldBe CadenceType.WEEKLY
            result.staleOneOnOnes[0].expectedIntervalDays shouldBe 7
            result.staleOneOnOnes[0].daysSinceLastMeeting shouldBe 10
        }

        @Test
        fun `should not return stale reminder when meeting is within cadence`() {
            val series = OneOnOneSeries(
                id = OneOnOneSeriesId.generate(),
                userId = userId,
                personId = personId1,
                cadenceType = CadenceType.WEEKLY
            )
            val lastMeeting = Instant.now().minus(3, ChronoUnit.DAYS)

            every { personRepository.findAllByUserIdUnpaged(userId) } returns listOf(person1, person2)
            every { actionItemRepository.findOverdueByUserId(userId, any(), any()) } returns PageImpl(emptyList())
            every { actionItemRepository.findDueSoonByUserId(userId, any(), any()) } returns emptyList()
            every { oneOnOneSeriesRepository.findAllByUserId(userId) } returns listOf(series)
            every { oneOnOneEntryRepository.findLatestMeetingDate(userId, personId1) } returns lastMeeting

            val query = GetDashboardQuery(userId = userId)
            val result = service.getDashboard(query)

            result.staleOneOnOnes.shouldBeEmpty()
        }

        @Test
        fun `should handle custom cadence interval`() {
            val series = OneOnOneSeries(
                id = OneOnOneSeriesId.generate(),
                userId = userId,
                personId = personId1,
                cadenceType = CadenceType.CUSTOM,
                customIntervalDays = 5
            )
            val lastMeeting = Instant.now().minus(8, ChronoUnit.DAYS)

            every { personRepository.findAllByUserIdUnpaged(userId) } returns listOf(person1, person2)
            every { actionItemRepository.findOverdueByUserId(userId, any(), any()) } returns PageImpl(emptyList())
            every { actionItemRepository.findDueSoonByUserId(userId, any(), any()) } returns emptyList()
            every { oneOnOneSeriesRepository.findAllByUserId(userId) } returns listOf(series)
            every { oneOnOneEntryRepository.findLatestMeetingDate(userId, personId1) } returns lastMeeting

            val query = GetDashboardQuery(userId = userId)
            val result = service.getDashboard(query)

            result.staleOneOnOnes shouldHaveSize 1
            result.staleOneOnOnes[0].expectedIntervalDays shouldBe 5
            result.staleOneOnOnes[0].daysSinceLastMeeting shouldBe 8
        }

        @Test
        fun `should consider series stale when no meeting ever occurred`() {
            val createdAt = Instant.now().minus(15, ChronoUnit.DAYS)
            val series = OneOnOneSeries(
                id = OneOnOneSeriesId.generate(),
                userId = userId,
                personId = personId1,
                cadenceType = CadenceType.WEEKLY,
                createdAt = createdAt
            )

            every { personRepository.findAllByUserIdUnpaged(userId) } returns listOf(person1, person2)
            every { actionItemRepository.findOverdueByUserId(userId, any(), any()) } returns PageImpl(emptyList())
            every { actionItemRepository.findDueSoonByUserId(userId, any(), any()) } returns emptyList()
            every { oneOnOneSeriesRepository.findAllByUserId(userId) } returns listOf(series)
            every { oneOnOneEntryRepository.findLatestMeetingDate(userId, personId1) } returns null

            val query = GetDashboardQuery(userId = userId)
            val result = service.getDashboard(query)

            result.staleOneOnOnes shouldHaveSize 1
            result.staleOneOnOnes[0].lastMeetingDate shouldBe null
            result.staleOneOnOnes[0].daysSinceLastMeeting shouldBe 15
        }

        @Test
        fun `should return upcoming anniversaries within lookahead window`() {
            every { personRepository.findAllByUserIdUnpaged(userId) } returns listOf(person1, person2)
            every { actionItemRepository.findOverdueByUserId(userId, any(), any()) } returns PageImpl(emptyList())
            every { actionItemRepository.findDueSoonByUserId(userId, any(), any()) } returns emptyList()
            every { oneOnOneSeriesRepository.findAllByUserId(userId) } returns emptyList()

            val query = GetDashboardQuery(userId = userId, anniversaryLookaheadDays = 30)
            val result = service.getDashboard(query)

            // person1 has anniversary in 5 days (within 30-day window)
            // person2 has anniversary in 60 days (outside 30-day window)
            result.upcomingAnniversaries shouldHaveSize 1
            result.upcomingAnniversaries[0].personName shouldBe "Alice Smith"
            result.upcomingAnniversaries[0].daysUntil shouldBe 5
            result.upcomingAnniversaries[0].yearsCompleted shouldBe 2
        }

        @Test
        fun `should not return anniversaries for persons without start date`() {
            val personNoDate = Person(
                id = PersonId.generate(),
                userId = userId,
                name = "No Date Person",
                startDate = null
            )

            every { personRepository.findAllByUserIdUnpaged(userId) } returns listOf(personNoDate)
            every { actionItemRepository.findOverdueByUserId(userId, any(), any()) } returns PageImpl(emptyList())
            every { actionItemRepository.findDueSoonByUserId(userId, any(), any()) } returns emptyList()
            every { oneOnOneSeriesRepository.findAllByUserId(userId) } returns emptyList()

            val query = GetDashboardQuery(userId = userId)
            val result = service.getDashboard(query)

            result.upcomingAnniversaries.shouldBeEmpty()
        }

        @Test
        fun `should sort stale reminders by days since last meeting descending`() {
            val series1 = OneOnOneSeries(
                id = OneOnOneSeriesId.generate(),
                userId = userId,
                personId = personId1,
                cadenceType = CadenceType.WEEKLY
            )
            val series2 = OneOnOneSeries(
                id = OneOnOneSeriesId.generate(),
                userId = userId,
                personId = personId2,
                cadenceType = CadenceType.WEEKLY
            )

            every { personRepository.findAllByUserIdUnpaged(userId) } returns listOf(person1, person2)
            every { actionItemRepository.findOverdueByUserId(userId, any(), any()) } returns PageImpl(emptyList())
            every { actionItemRepository.findDueSoonByUserId(userId, any(), any()) } returns emptyList()
            every { oneOnOneSeriesRepository.findAllByUserId(userId) } returns listOf(series1, series2)
            every { oneOnOneEntryRepository.findLatestMeetingDate(userId, personId1) } returns Instant.now().minus(10, ChronoUnit.DAYS)
            every { oneOnOneEntryRepository.findLatestMeetingDate(userId, personId2) } returns Instant.now().minus(20, ChronoUnit.DAYS)

            val query = GetDashboardQuery(userId = userId)
            val result = service.getDashboard(query)

            result.staleOneOnOnes shouldHaveSize 2
            result.staleOneOnOnes[0].personName shouldBe "Bob Jones" // 20 days
            result.staleOneOnOnes[1].personName shouldBe "Alice Smith" // 10 days
        }

        @Test
        fun `should sort upcoming anniversaries by days until ascending`() {
            val person3 = Person(
                id = PersonId.generate(),
                userId = userId,
                name = "Charlie",
                startDate = LocalDate.now().minusYears(3).plusDays(15) // Anniversary in 15 days
            )

            every { personRepository.findAllByUserIdUnpaged(userId) } returns listOf(person1, person3)
            every { actionItemRepository.findOverdueByUserId(userId, any(), any()) } returns PageImpl(emptyList())
            every { actionItemRepository.findDueSoonByUserId(userId, any(), any()) } returns emptyList()
            every { oneOnOneSeriesRepository.findAllByUserId(userId) } returns emptyList()

            val query = GetDashboardQuery(userId = userId, anniversaryLookaheadDays = 30)
            val result = service.getDashboard(query)

            result.upcomingAnniversaries shouldHaveSize 2
            result.upcomingAnniversaries[0].personName shouldBe "Alice Smith" // 5 days
            result.upcomingAnniversaries[1].personName shouldBe "Charlie" // 15 days
        }

        @Test
        fun `should handle biweekly cadence correctly`() {
            val series = OneOnOneSeries(
                id = OneOnOneSeriesId.generate(),
                userId = userId,
                personId = personId1,
                cadenceType = CadenceType.BIWEEKLY
            )
            val lastMeeting = Instant.now().minus(16, ChronoUnit.DAYS)

            every { personRepository.findAllByUserIdUnpaged(userId) } returns listOf(person1)
            every { actionItemRepository.findOverdueByUserId(userId, any(), any()) } returns PageImpl(emptyList())
            every { actionItemRepository.findDueSoonByUserId(userId, any(), any()) } returns emptyList()
            every { oneOnOneSeriesRepository.findAllByUserId(userId) } returns listOf(series)
            every { oneOnOneEntryRepository.findLatestMeetingDate(userId, personId1) } returns lastMeeting

            val query = GetDashboardQuery(userId = userId)
            val result = service.getDashboard(query)

            result.staleOneOnOnes shouldHaveSize 1
            result.staleOneOnOnes[0].expectedIntervalDays shouldBe 14
        }

        @Test
        fun `should handle monthly cadence correctly`() {
            val series = OneOnOneSeries(
                id = OneOnOneSeriesId.generate(),
                userId = userId,
                personId = personId1,
                cadenceType = CadenceType.MONTHLY
            )
            val lastMeeting = Instant.now().minus(35, ChronoUnit.DAYS)

            every { personRepository.findAllByUserIdUnpaged(userId) } returns listOf(person1)
            every { actionItemRepository.findOverdueByUserId(userId, any(), any()) } returns PageImpl(emptyList())
            every { actionItemRepository.findDueSoonByUserId(userId, any(), any()) } returns emptyList()
            every { oneOnOneSeriesRepository.findAllByUserId(userId) } returns listOf(series)
            every { oneOnOneEntryRepository.findLatestMeetingDate(userId, personId1) } returns lastMeeting

            val query = GetDashboardQuery(userId = userId)
            val result = service.getDashboard(query)

            result.staleOneOnOnes shouldHaveSize 1
            result.staleOneOnOnes[0].expectedIntervalDays shouldBe 30
        }

        @Test
        fun `should scope all queries by userId`() {
            every { personRepository.findAllByUserIdUnpaged(userId) } returns emptyList()
            every { actionItemRepository.findOverdueByUserId(userId, any(), any()) } returns PageImpl(emptyList())
            every { actionItemRepository.findDueSoonByUserId(userId, any(), any()) } returns emptyList()
            every { oneOnOneSeriesRepository.findAllByUserId(userId) } returns emptyList()

            val query = GetDashboardQuery(userId = userId)
            service.getDashboard(query)

            verify { personRepository.findAllByUserIdUnpaged(userId) }
            verify { actionItemRepository.findOverdueByUserId(userId, any(), any()) }
            verify { actionItemRepository.findDueSoonByUserId(userId, any(), any()) }
            verify { oneOnOneSeriesRepository.findAllByUserId(userId) }
        }

        @Test
        fun `should handle anniversary that already passed this year`() {
            // Person whose anniversary already passed this year — next anniversary is next year
            val personPastAnniversary = Person(
                id = PersonId.generate(),
                userId = userId,
                name = "Past Anniversary",
                startDate = LocalDate.now().minusYears(2).minusDays(10) // Anniversary was 10 days ago
            )

            every { personRepository.findAllByUserIdUnpaged(userId) } returns listOf(personPastAnniversary)
            every { actionItemRepository.findOverdueByUserId(userId, any(), any()) } returns PageImpl(emptyList())
            every { actionItemRepository.findDueSoonByUserId(userId, any(), any()) } returns emptyList()
            every { oneOnOneSeriesRepository.findAllByUserId(userId) } returns emptyList()

            val query = GetDashboardQuery(userId = userId, anniversaryLookaheadDays = 30)
            val result = service.getDashboard(query)

            // Next anniversary is ~355 days away, outside 30-day window
            result.upcomingAnniversaries.shouldBeEmpty()
        }
    }
}
