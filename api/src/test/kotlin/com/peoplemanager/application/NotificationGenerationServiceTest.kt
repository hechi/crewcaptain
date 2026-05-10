package com.peoplemanager.application

import com.peoplemanager.application.ports.*
import com.peoplemanager.domain.*
import io.kotest.matchers.shouldBe
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class NotificationGenerationServiceTest {

    private val userRepository = mockk<UserRepository>()
    private val personRepository = mockk<PersonRepository>()
    private val actionItemRepository = mockk<ActionItemRepository>()
    private val oneOnOneSeriesRepository = mockk<OneOnOneSeriesRepository>()
    private val oneOnOneEntryRepository = mockk<OneOnOneEntryRepository>()
    private val notificationRepository = mockk<NotificationRepository>()
    private val userSettingsRepository = mockk<UserSettingsRepository>()

    private val service = NotificationGenerationService(
        userRepository, personRepository, actionItemRepository,
        oneOnOneSeriesRepository, oneOnOneEntryRepository, notificationRepository,
        userSettingsRepository
    )

    private val userId = UserId.generate()
    private val personId = PersonId.generate()
    private val person = Person(
        id = personId,
        userId = userId,
        name = "Alice Smith",
        startDate = null
    )
    private val personWithAnniversary = Person(
        id = personId,
        userId = userId,
        name = "Alice Smith",
        startDate = LocalDate.now().minusYears(2).plusDays(5)
    )

    @BeforeEach
    fun setup() {
        clearAllMocks()
        every { notificationRepository.saveAll(any()) } answers { firstArg() }
        every { notificationRepository.existsByUserIdAndTypeAndReferenceIdAndCreatedAfter(any(), any(), any(), any()) } returns false
        every { userSettingsRepository.findByUserId(any()) } returns null
    }

    @Nested
    inner class OverdueActionItemNotifications {

        @Test
        fun `should generate notification for overdue action item`() {
            val actionItem = ActionItem(
                id = ActionItemId.generate(),
                userId = userId,
                personId = personId,
                title = "Review PR",
                dueDate = LocalDate.now().minusDays(2),
                ownerType = ActionItemOwnerType.PERSON
            )

            every { personRepository.findAllByUserIdUnpaged(userId) } returns listOf(person)
            every { actionItemRepository.findOverdueByUserId(userId, any(), any()) } returns PageImpl(listOf(actionItem))
            every { actionItemRepository.findDueSoonByUserId(userId, any(), any()) } returns emptyList()
            every { oneOnOneSeriesRepository.findAllByUserId(userId) } returns emptyList()

            val count = service.generateForUser(userId)

            count shouldBe 1
            verify {
                notificationRepository.saveAll(match { notifications ->
                    notifications.size == 1 &&
                    notifications[0].type == NotificationType.ACTION_ITEM_OVERDUE &&
                    notifications[0].title == "Action item overdue" &&
                    notifications[0].message.contains("Review PR") &&
                    notifications[0].message.contains("Alice Smith")
                })
            }
        }

        @Test
        fun `should not generate duplicate notification within deduplication window`() {
            val actionItem = ActionItem(
                id = ActionItemId.generate(),
                userId = userId,
                personId = personId,
                title = "Review PR",
                dueDate = LocalDate.now().minusDays(2),
                ownerType = ActionItemOwnerType.PERSON
            )

            every { personRepository.findAllByUserIdUnpaged(userId) } returns listOf(person)
            every { actionItemRepository.findOverdueByUserId(userId, any(), any()) } returns PageImpl(listOf(actionItem))
            every { actionItemRepository.findDueSoonByUserId(userId, any(), any()) } returns emptyList()
            every { oneOnOneSeriesRepository.findAllByUserId(userId) } returns emptyList()
            every {
                notificationRepository.existsByUserIdAndTypeAndReferenceIdAndCreatedAfter(
                    userId, NotificationType.ACTION_ITEM_OVERDUE, actionItem.id.value.toString(), any()
                )
            } returns true

            val count = service.generateForUser(userId)

            count shouldBe 0
        }
    }

    @Nested
    inner class DueSoonActionItemNotifications {

        @Test
        fun `should generate notification for action item due soon`() {
            val actionItem = ActionItem(
                id = ActionItemId.generate(),
                userId = userId,
                personId = personId,
                title = "Submit report",
                dueDate = LocalDate.now().plusDays(2),
                ownerType = ActionItemOwnerType.MANAGER
            )

            every { personRepository.findAllByUserIdUnpaged(userId) } returns listOf(person)
            every { actionItemRepository.findOverdueByUserId(userId, any(), any()) } returns PageImpl(emptyList())
            every { actionItemRepository.findDueSoonByUserId(userId, any(), any()) } returns listOf(actionItem)
            every { oneOnOneSeriesRepository.findAllByUserId(userId) } returns emptyList()

            val count = service.generateForUser(userId)

            count shouldBe 1
            verify {
                notificationRepository.saveAll(match { notifications ->
                    notifications.size == 1 &&
                    notifications[0].type == NotificationType.ACTION_ITEM_DUE_SOON &&
                    notifications[0].message.contains("Submit report") &&
                    notifications[0].message.contains("Alice Smith")
                })
            }
        }

        @Test
        fun `should not generate duplicate due-soon notification`() {
            val actionItem = ActionItem(
                id = ActionItemId.generate(),
                userId = userId,
                personId = personId,
                title = "Submit report",
                dueDate = LocalDate.now().plusDays(2),
                ownerType = ActionItemOwnerType.MANAGER
            )

            every { personRepository.findAllByUserIdUnpaged(userId) } returns listOf(person)
            every { actionItemRepository.findOverdueByUserId(userId, any(), any()) } returns PageImpl(emptyList())
            every { actionItemRepository.findDueSoonByUserId(userId, any(), any()) } returns listOf(actionItem)
            every { oneOnOneSeriesRepository.findAllByUserId(userId) } returns emptyList()
            every {
                notificationRepository.existsByUserIdAndTypeAndReferenceIdAndCreatedAfter(
                    userId, NotificationType.ACTION_ITEM_DUE_SOON, actionItem.id.value.toString(), any()
                )
            } returns true

            val count = service.generateForUser(userId)

            count shouldBe 0
        }
    }

    @Nested
    inner class StaleOneOnOneNotifications {

        @Test
        fun `should generate notification for stale 1-on-1`() {
            val series = OneOnOneSeries(
                id = OneOnOneSeriesId.generate(),
                userId = userId,
                personId = personId,
                cadenceType = CadenceType.WEEKLY
            )
            val lastMeeting = Instant.now().minus(10, ChronoUnit.DAYS)

            every { personRepository.findAllByUserIdUnpaged(userId) } returns listOf(person)
            every { actionItemRepository.findOverdueByUserId(userId, any(), any()) } returns PageImpl(emptyList())
            every { actionItemRepository.findDueSoonByUserId(userId, any(), any()) } returns emptyList()
            every { oneOnOneSeriesRepository.findAllByUserId(userId) } returns listOf(series)
            every { oneOnOneEntryRepository.findLatestMeetingDate(userId, personId) } returns lastMeeting

            val count = service.generateForUser(userId)

            count shouldBe 1
            verify {
                notificationRepository.saveAll(match { notifications ->
                    notifications.size == 1 &&
                    notifications[0].type == NotificationType.STALE_ONE_ON_ONE &&
                    notifications[0].message.contains("Alice Smith") &&
                    notifications[0].message.contains("10 days")
                })
            }
        }

        @Test
        fun `should not generate notification when 1-on-1 is within cadence`() {
            val series = OneOnOneSeries(
                id = OneOnOneSeriesId.generate(),
                userId = userId,
                personId = personId,
                cadenceType = CadenceType.WEEKLY
            )
            val lastMeeting = Instant.now().minus(3, ChronoUnit.DAYS)

            every { personRepository.findAllByUserIdUnpaged(userId) } returns listOf(person)
            every { actionItemRepository.findOverdueByUserId(userId, any(), any()) } returns PageImpl(emptyList())
            every { actionItemRepository.findDueSoonByUserId(userId, any(), any()) } returns emptyList()
            every { oneOnOneSeriesRepository.findAllByUserId(userId) } returns listOf(series)
            every { oneOnOneEntryRepository.findLatestMeetingDate(userId, personId) } returns lastMeeting

            val count = service.generateForUser(userId)

            count shouldBe 0
        }

        @Test
        fun `should generate notification when no meeting ever occurred and cadence exceeded`() {
            val createdAt = Instant.now().minus(15, ChronoUnit.DAYS)
            val series = OneOnOneSeries(
                id = OneOnOneSeriesId.generate(),
                userId = userId,
                personId = personId,
                cadenceType = CadenceType.WEEKLY,
                createdAt = createdAt
            )

            every { personRepository.findAllByUserIdUnpaged(userId) } returns listOf(person)
            every { actionItemRepository.findOverdueByUserId(userId, any(), any()) } returns PageImpl(emptyList())
            every { actionItemRepository.findDueSoonByUserId(userId, any(), any()) } returns emptyList()
            every { oneOnOneSeriesRepository.findAllByUserId(userId) } returns listOf(series)
            every { oneOnOneEntryRepository.findLatestMeetingDate(userId, personId) } returns null

            val count = service.generateForUser(userId)

            count shouldBe 1
            verify {
                notificationRepository.saveAll(match { notifications ->
                    notifications[0].type == NotificationType.STALE_ONE_ON_ONE &&
                    notifications[0].message.contains("15 days")
                })
            }
        }

        @Test
        fun `should not generate duplicate stale 1-on-1 notification`() {
            val series = OneOnOneSeries(
                id = OneOnOneSeriesId.generate(),
                userId = userId,
                personId = personId,
                cadenceType = CadenceType.WEEKLY
            )
            val lastMeeting = Instant.now().minus(10, ChronoUnit.DAYS)

            every { personRepository.findAllByUserIdUnpaged(userId) } returns listOf(person)
            every { actionItemRepository.findOverdueByUserId(userId, any(), any()) } returns PageImpl(emptyList())
            every { actionItemRepository.findDueSoonByUserId(userId, any(), any()) } returns emptyList()
            every { oneOnOneSeriesRepository.findAllByUserId(userId) } returns listOf(series)
            every { oneOnOneEntryRepository.findLatestMeetingDate(userId, personId) } returns lastMeeting
            every {
                notificationRepository.existsByUserIdAndTypeAndReferenceIdAndCreatedAfter(
                    userId, NotificationType.STALE_ONE_ON_ONE, personId.value.toString(), any()
                )
            } returns true

            val count = service.generateForUser(userId)

            count shouldBe 0
        }
    }

    @Nested
    inner class AnniversaryNotifications {

        @Test
        fun `should generate notification for upcoming anniversary`() {
            every { personRepository.findAllByUserIdUnpaged(userId) } returns listOf(personWithAnniversary)
            every { actionItemRepository.findOverdueByUserId(userId, any(), any()) } returns PageImpl(emptyList())
            every { actionItemRepository.findDueSoonByUserId(userId, any(), any()) } returns emptyList()
            every { oneOnOneSeriesRepository.findAllByUserId(userId) } returns emptyList()

            val count = service.generateForUser(userId, anniversaryLookaheadDays = 7)

            count shouldBe 1
            verify {
                notificationRepository.saveAll(match { notifications ->
                    notifications.size == 1 &&
                    notifications[0].type == NotificationType.UPCOMING_ANNIVERSARY &&
                    notifications[0].message.contains("Alice Smith") &&
                    notifications[0].message.contains("2-year")
                })
            }
        }

        @Test
        fun `should not generate anniversary notification outside lookahead window`() {
            val personFarAnniversary = Person(
                id = PersonId.generate(),
                userId = userId,
                name = "Bob Jones",
                startDate = LocalDate.now().minusYears(1).plusDays(60)
            )

            every { personRepository.findAllByUserIdUnpaged(userId) } returns listOf(personFarAnniversary)
            every { actionItemRepository.findOverdueByUserId(userId, any(), any()) } returns PageImpl(emptyList())
            every { actionItemRepository.findDueSoonByUserId(userId, any(), any()) } returns emptyList()
            every { oneOnOneSeriesRepository.findAllByUserId(userId) } returns emptyList()

            val count = service.generateForUser(userId, anniversaryLookaheadDays = 7)

            count shouldBe 0
        }

        @Test
        fun `should not generate anniversary notification for person without start date`() {
            val personNoDate = Person(
                id = PersonId.generate(),
                userId = userId,
                name = "No Date",
                startDate = null
            )

            every { personRepository.findAllByUserIdUnpaged(userId) } returns listOf(personNoDate)
            every { actionItemRepository.findOverdueByUserId(userId, any(), any()) } returns PageImpl(emptyList())
            every { actionItemRepository.findDueSoonByUserId(userId, any(), any()) } returns emptyList()
            every { oneOnOneSeriesRepository.findAllByUserId(userId) } returns emptyList()

            val count = service.generateForUser(userId)

            count shouldBe 0
        }

        @Test
        fun `should not generate duplicate anniversary notification`() {
            every { personRepository.findAllByUserIdUnpaged(userId) } returns listOf(personWithAnniversary)
            every { actionItemRepository.findOverdueByUserId(userId, any(), any()) } returns PageImpl(emptyList())
            every { actionItemRepository.findDueSoonByUserId(userId, any(), any()) } returns emptyList()
            every { oneOnOneSeriesRepository.findAllByUserId(userId) } returns emptyList()
            every {
                notificationRepository.existsByUserIdAndTypeAndReferenceIdAndCreatedAfter(
                    userId, NotificationType.UPCOMING_ANNIVERSARY, personId.value.toString(), any()
                )
            } returns true

            val count = service.generateForUser(userId, anniversaryLookaheadDays = 7)

            count shouldBe 0
        }
    }

    @Nested
    inner class GeneralBehavior {

        @Test
        fun `should return zero when no data exists`() {
            every { personRepository.findAllByUserIdUnpaged(userId) } returns emptyList()
            every { actionItemRepository.findOverdueByUserId(userId, any(), any()) } returns PageImpl(emptyList())
            every { actionItemRepository.findDueSoonByUserId(userId, any(), any()) } returns emptyList()
            every { oneOnOneSeriesRepository.findAllByUserId(userId) } returns emptyList()

            val count = service.generateForUser(userId)

            count shouldBe 0
            verify(exactly = 0) { notificationRepository.saveAll(any()) }
        }

        @Test
        fun `should generate multiple notification types in single run`() {
            val overdueItem = ActionItem(
                id = ActionItemId.generate(),
                userId = userId,
                personId = personId,
                title = "Overdue task",
                dueDate = LocalDate.now().minusDays(1),
                ownerType = ActionItemOwnerType.PERSON
            )
            val dueSoonItem = ActionItem(
                id = ActionItemId.generate(),
                userId = userId,
                personId = personId,
                title = "Due soon task",
                dueDate = LocalDate.now().plusDays(1),
                ownerType = ActionItemOwnerType.MANAGER
            )
            val series = OneOnOneSeries(
                id = OneOnOneSeriesId.generate(),
                userId = userId,
                personId = personId,
                cadenceType = CadenceType.WEEKLY
            )

            every { personRepository.findAllByUserIdUnpaged(userId) } returns listOf(personWithAnniversary)
            every { actionItemRepository.findOverdueByUserId(userId, any(), any()) } returns PageImpl(listOf(overdueItem))
            every { actionItemRepository.findDueSoonByUserId(userId, any(), any()) } returns listOf(dueSoonItem)
            every { oneOnOneSeriesRepository.findAllByUserId(userId) } returns listOf(series)
            every { oneOnOneEntryRepository.findLatestMeetingDate(userId, personId) } returns Instant.now().minus(10, ChronoUnit.DAYS)

            val count = service.generateForUser(userId, anniversaryLookaheadDays = 7)

            // overdue + due soon + stale 1:1 + anniversary = 4
            count shouldBe 4
            verify {
                notificationRepository.saveAll(match { it.size == 4 })
            }
        }

        @Test
        fun `should scope all queries by userId`() {
            every { personRepository.findAllByUserIdUnpaged(userId) } returns emptyList()
            every { actionItemRepository.findOverdueByUserId(userId, any(), any()) } returns PageImpl(emptyList())
            every { actionItemRepository.findDueSoonByUserId(userId, any(), any()) } returns emptyList()
            every { oneOnOneSeriesRepository.findAllByUserId(userId) } returns emptyList()

            service.generateForUser(userId)

            verify { personRepository.findAllByUserIdUnpaged(userId) }
            verify { actionItemRepository.findOverdueByUserId(userId, any(), any()) }
            verify { actionItemRepository.findDueSoonByUserId(userId, any(), any()) }
            verify { oneOnOneSeriesRepository.findAllByUserId(userId) }
        }
    }
}
