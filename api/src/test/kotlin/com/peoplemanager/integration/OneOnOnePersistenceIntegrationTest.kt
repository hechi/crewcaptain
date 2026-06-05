package com.peoplemanager.integration

import com.peoplemanager.adapters.persistence.*
import com.peoplemanager.application.port.output.OneOnOneEntryRepository
import com.peoplemanager.application.port.output.OneOnOneSeriesRepository
import com.peoplemanager.domain.*
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.sql.Timestamp
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

@SpringBootTest
@Testcontainers
class OneOnOnePersistenceIntegrationTest {

    companion object {
        @Container
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:16")

        @DynamicPropertySource
        @JvmStatic
        fun configureProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { postgres.jdbcUrl }
            registry.add("spring.datasource.username") { postgres.username }
            registry.add("spring.datasource.password") { postgres.password }
            registry.add("spring.flyway.enabled") { "true" }
            registry.add("spring.jpa.hibernate.ddl-auto") { "validate" }
            registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri") { "http://localhost:9000" }
            registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri") { "http://localhost:9000/jwks" }
        }
    }

    @Autowired
    lateinit var seriesRepository: OneOnOneSeriesRepository

    @Autowired
    lateinit var entryRepository: OneOnOneEntryRepository

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    private lateinit var userAId: UUID
    private lateinit var userBId: UUID
    private lateinit var personAId: UUID
    private lateinit var personBId: UUID

    @BeforeEach
    fun setUp() {
        // Clean up tables in correct order (respecting FK constraints)
        jdbcTemplate.execute("DELETE FROM agenda_items")
        jdbcTemplate.execute("DELETE FROM one_on_one_entries")
        jdbcTemplate.execute("DELETE FROM one_on_one_series")
        jdbcTemplate.execute("DELETE FROM pinned_remember_items")
        jdbcTemplate.execute("DELETE FROM persons")
        jdbcTemplate.execute("DELETE FROM users")

        // Create test users
        userAId = UUID.randomUUID()
        userBId = UUID.randomUUID()
        val now = Timestamp.from(Instant.now())

        jdbcTemplate.update(
            "INSERT INTO users (id, oidc_subject, oidc_issuer, display_name, email, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
            userAId, "subject-a", "http://issuer", "User A", "usera@test.com", now, now
        )
        jdbcTemplate.update(
            "INSERT INTO users (id, oidc_subject, oidc_issuer, display_name, email, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
            userBId, "subject-b", "http://issuer", "User B", "userb@test.com", now, now
        )

        // Create test persons (one for each user)
        personAId = UUID.randomUUID()
        personBId = UUID.randomUUID()

        jdbcTemplate.update(
            "INSERT INTO persons (id, user_id, name, morale_status, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?)",
            personAId, userAId, "Person A", "UNKNOWN", now, now
        )
        jdbcTemplate.update(
            "INSERT INTO persons (id, user_id, name, morale_status, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?)",
            personBId, userBId, "Person B", "UNKNOWN", now, now
        )
    }

    @Nested
    inner class OneOnOneSeriesRepositoryTests {

        @Test
        fun `save should persist a new series`() {
            val series = OneOnOneSeries(
                id = OneOnOneSeriesId.generate(),
                userId = UserId(userAId),
                personId = PersonId(personAId),
                cadenceType = CadenceType.BIWEEKLY,
                customIntervalDays = null,
                templateMarkdown = "## Template\n- [ ] Item 1",
                createdAt = Instant.now().truncatedTo(ChronoUnit.MILLIS),
                updatedAt = Instant.now().truncatedTo(ChronoUnit.MILLIS)
            )

            val saved = seriesRepository.save(series)

            saved.id shouldBe series.id
            saved.userId shouldBe series.userId
            saved.personId shouldBe series.personId
            saved.cadenceType shouldBe CadenceType.BIWEEKLY
            saved.customIntervalDays.shouldBeNull()
            saved.templateMarkdown shouldBe "## Template\n- [ ] Item 1"
        }

        @Test
        fun `findByUserIdAndPersonId should return series when it exists`() {
            val series = OneOnOneSeries(
                id = OneOnOneSeriesId.generate(),
                userId = UserId(userAId),
                personId = PersonId(personAId),
                cadenceType = CadenceType.WEEKLY,
                customIntervalDays = null,
                templateMarkdown = null,
                createdAt = Instant.now().truncatedTo(ChronoUnit.MILLIS),
                updatedAt = Instant.now().truncatedTo(ChronoUnit.MILLIS)
            )
            seriesRepository.save(series)

            val found = seriesRepository.findByUserIdAndPersonId(UserId(userAId), PersonId(personAId))

            found.shouldNotBeNull()
            found.id shouldBe series.id
            found.cadenceType shouldBe CadenceType.WEEKLY
        }

        @Test
        fun `findByUserIdAndPersonId should return null when no series exists`() {
            val found = seriesRepository.findByUserIdAndPersonId(UserId(userAId), PersonId(personAId))
            found.shouldBeNull()
        }

        @Test
        fun `unique constraint should prevent duplicate series for same user and person`() {
            val series1 = OneOnOneSeries(
                id = OneOnOneSeriesId.generate(),
                userId = UserId(userAId),
                personId = PersonId(personAId),
                cadenceType = CadenceType.WEEKLY,
                customIntervalDays = null,
                templateMarkdown = null,
                createdAt = Instant.now().truncatedTo(ChronoUnit.MILLIS),
                updatedAt = Instant.now().truncatedTo(ChronoUnit.MILLIS)
            )
            seriesRepository.save(series1)

            val series2 = OneOnOneSeries(
                id = OneOnOneSeriesId.generate(),
                userId = UserId(userAId),
                personId = PersonId(personAId),
                cadenceType = CadenceType.MONTHLY,
                customIntervalDays = null,
                templateMarkdown = null,
                createdAt = Instant.now().truncatedTo(ChronoUnit.MILLIS),
                updatedAt = Instant.now().truncatedTo(ChronoUnit.MILLIS)
            )

            val exception = org.junit.jupiter.api.assertThrows<Exception> {
                seriesRepository.save(series2)
                // Force flush by querying
                seriesRepository.findByUserIdAndPersonId(UserId(userAId), PersonId(personAId))
            }
            // The exception should be related to unique constraint violation
            val rootCause = generateSequence(exception) { it.cause as? Exception }.last()
            rootCause.message.shouldNotBeNull()
        }

        @Test
        fun `save should allow series for different user-person combinations`() {
            val seriesA = OneOnOneSeries(
                id = OneOnOneSeriesId.generate(),
                userId = UserId(userAId),
                personId = PersonId(personAId),
                cadenceType = CadenceType.WEEKLY,
                customIntervalDays = null,
                templateMarkdown = null,
                createdAt = Instant.now().truncatedTo(ChronoUnit.MILLIS),
                updatedAt = Instant.now().truncatedTo(ChronoUnit.MILLIS)
            )
            val seriesB = OneOnOneSeries(
                id = OneOnOneSeriesId.generate(),
                userId = UserId(userBId),
                personId = PersonId(personBId),
                cadenceType = CadenceType.MONTHLY,
                customIntervalDays = null,
                templateMarkdown = null,
                createdAt = Instant.now().truncatedTo(ChronoUnit.MILLIS),
                updatedAt = Instant.now().truncatedTo(ChronoUnit.MILLIS)
            )

            seriesRepository.save(seriesA)
            seriesRepository.save(seriesB)

            seriesRepository.findByUserIdAndPersonId(UserId(userAId), PersonId(personAId)).shouldNotBeNull()
            seriesRepository.findByUserIdAndPersonId(UserId(userBId), PersonId(personBId)).shouldNotBeNull()
        }
    }

    @Nested
    inner class OneOnOneEntryRepositoryTests {

        @Test
        fun `save should persist entry with agenda items`() {
            val now = Instant.now().truncatedTo(ChronoUnit.MILLIS)
            val entry = OneOnOneEntry(
                id = OneOnOneEntryId.generate(),
                userId = UserId(userAId),
                personId = PersonId(personAId),
                meetingDate = now,
                agendaItems = listOf(
                    AgendaItem(
                        id = AgendaItemId.generate(),
                        text = "Discuss project timeline",
                        checked = false,
                        displayOrder = 0,
                        createdAt = now
                    ),
                    AgendaItem(
                        id = AgendaItemId.generate(),
                        text = "Review Q2 goals",
                        checked = true,
                        displayOrder = 1,
                        createdAt = now
                    )
                ),
                notesMarkdown = "## Notes\nSome discussion points",
                outcomesMarkdown = "## Outcomes\nAgreed on timeline",
                sensitive = false,
                createdAt = now,
                updatedAt = now
            )

            val saved = entryRepository.save(entry)

            saved.id shouldBe entry.id
            saved.userId shouldBe entry.userId
            saved.personId shouldBe entry.personId
            saved.notesMarkdown shouldBe "## Notes\nSome discussion points"
            saved.outcomesMarkdown shouldBe "## Outcomes\nAgreed on timeline"
            saved.sensitive shouldBe false
            saved.agendaItems shouldHaveSize 2
            saved.agendaItems[0].text shouldBe "Discuss project timeline"
            saved.agendaItems[0].checked shouldBe false
            saved.agendaItems[1].text shouldBe "Review Q2 goals"
            saved.agendaItems[1].checked shouldBe true
        }

        @Test
        fun `findByIdAndUserIdAndPersonId should return entry when it exists`() {
            val now = Instant.now().truncatedTo(ChronoUnit.MILLIS)
            val entry = createAndSaveEntry(userAId, personAId, now)

            val found = entryRepository.findByIdAndUserIdAndPersonId(
                entry.id, UserId(userAId), PersonId(personAId)
            )

            found.shouldNotBeNull()
            found.id shouldBe entry.id
        }

        @Test
        fun `findByIdAndUserIdAndPersonId should return null for wrong userId`() {
            val now = Instant.now().truncatedTo(ChronoUnit.MILLIS)
            val entry = createAndSaveEntry(userAId, personAId, now)

            val found = entryRepository.findByIdAndUserIdAndPersonId(
                entry.id, UserId(userBId), PersonId(personAId)
            )

            found.shouldBeNull()
        }

        @Test
        fun `findAllByUserIdAndPersonId should return paginated results ordered by meetingDate DESC`() {
            val baseDate = Instant.parse("2025-01-01T10:00:00Z")

            // Create entries with different meeting dates
            createAndSaveEntry(userAId, personAId, baseDate)
            createAndSaveEntry(userAId, personAId, baseDate.plus(1, ChronoUnit.DAYS))
            createAndSaveEntry(userAId, personAId, baseDate.plus(2, ChronoUnit.DAYS))
            val entry4 = createAndSaveEntry(userAId, personAId, baseDate.plus(3, ChronoUnit.DAYS))
            val entry5 = createAndSaveEntry(userAId, personAId, baseDate.plus(4, ChronoUnit.DAYS))

            // Request first page of size 2, sorted by meetingDate DESC
            val pageable = PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "meetingDate"))
            val page = entryRepository.findAllByUserIdAndPersonId(
                UserId(userAId), PersonId(personAId), pageable
            )

            page.totalElements shouldBe 5
            page.totalPages shouldBe 3
            page.content shouldHaveSize 2
            // Most recent first
            page.content[0].id shouldBe entry5.id
            page.content[1].id shouldBe entry4.id
        }

        @Test
        fun `findAllByUserIdAndPersonId should return second page correctly`() {
            val baseDate = Instant.parse("2025-01-01T10:00:00Z")

            createAndSaveEntry(userAId, personAId, baseDate)
            createAndSaveEntry(userAId, personAId, baseDate.plus(1, ChronoUnit.DAYS))
            createAndSaveEntry(userAId, personAId, baseDate.plus(2, ChronoUnit.DAYS))

            val pageable = PageRequest.of(1, 2, Sort.by(Sort.Direction.DESC, "meetingDate"))
            val page = entryRepository.findAllByUserIdAndPersonId(
                UserId(userAId), PersonId(personAId), pageable
            )

            page.totalElements shouldBe 3
            page.content shouldHaveSize 1
            // The oldest entry should be on the second page
            page.content[0].meetingDate shouldBe baseDate
        }
    }

    @Nested
    inner class DataIsolationTests {

        @Test
        fun `User B cannot access User A entries via findByIdAndUserIdAndPersonId`() {
            val now = Instant.now().truncatedTo(ChronoUnit.MILLIS)
            val entryA = createAndSaveEntry(userAId, personAId, now)

            // User B tries to access User A's entry
            val found = entryRepository.findByIdAndUserIdAndPersonId(
                entryA.id, UserId(userBId), PersonId(personAId)
            )

            found.shouldBeNull()
        }

        @Test
        fun `User B cannot see User A entries in list`() {
            val now = Instant.now().truncatedTo(ChronoUnit.MILLIS)
            createAndSaveEntry(userAId, personAId, now)
            createAndSaveEntry(userAId, personAId, now.plus(1, ChronoUnit.DAYS))

            // User B queries for entries on User A's person
            val pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "meetingDate"))
            val page = entryRepository.findAllByUserIdAndPersonId(
                UserId(userBId), PersonId(personAId), pageable
            )

            page.totalElements shouldBe 0
            page.content shouldHaveSize 0
        }

        @Test
        fun `User B cannot delete User A entries`() {
            val now = Instant.now().truncatedTo(ChronoUnit.MILLIS)
            val entryA = createAndSaveEntry(userAId, personAId, now)

            val deleted = entryRepository.deleteByIdAndUserIdAndPersonId(
                entryA.id, UserId(userBId), PersonId(personAId)
            )

            deleted shouldBe false

            // Entry should still exist for User A
            val stillExists = entryRepository.findByIdAndUserIdAndPersonId(
                entryA.id, UserId(userAId), PersonId(personAId)
            )
            stillExists.shouldNotBeNull()
        }

        @Test
        fun `User B series lookup does not return User A series`() {
            val series = OneOnOneSeries(
                id = OneOnOneSeriesId.generate(),
                userId = UserId(userAId),
                personId = PersonId(personAId),
                cadenceType = CadenceType.WEEKLY,
                customIntervalDays = null,
                templateMarkdown = "template",
                createdAt = Instant.now().truncatedTo(ChronoUnit.MILLIS),
                updatedAt = Instant.now().truncatedTo(ChronoUnit.MILLIS)
            )
            seriesRepository.save(series)

            // User B tries to find User A's series
            val found = seriesRepository.findByUserIdAndPersonId(UserId(userBId), PersonId(personAId))
            found.shouldBeNull()
        }
    }

    @Nested
    inner class CascadeDeleteTests {

        @Test
        fun `deleting an entry removes its agenda items`() {
            val now = Instant.now().truncatedTo(ChronoUnit.MILLIS)
            val entry = OneOnOneEntry(
                id = OneOnOneEntryId.generate(),
                userId = UserId(userAId),
                personId = PersonId(personAId),
                meetingDate = now,
                agendaItems = listOf(
                    AgendaItem(
                        id = AgendaItemId.generate(),
                        text = "Item 1",
                        checked = false,
                        displayOrder = 0,
                        createdAt = now
                    ),
                    AgendaItem(
                        id = AgendaItemId.generate(),
                        text = "Item 2",
                        checked = false,
                        displayOrder = 1,
                        createdAt = now
                    )
                ),
                notesMarkdown = null,
                outcomesMarkdown = null,
                sensitive = false,
                createdAt = now,
                updatedAt = now
            )
            entryRepository.save(entry)

            // Verify agenda items exist
            val agendaCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM agenda_items WHERE entry_id = ?",
                Int::class.java,
                entry.id.value
            )!!
            agendaCount shouldBe 2

            // Delete the entry
            val deleted = entryRepository.deleteByIdAndUserIdAndPersonId(
                entry.id, UserId(userAId), PersonId(personAId)
            )
            deleted shouldBe true

            // Verify agenda items are gone (cascade delete)
            val agendaCountAfter = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM agenda_items WHERE entry_id = ?",
                Int::class.java,
                entry.id.value
            )!!
            agendaCountAfter shouldBe 0
        }

        @Test
        fun `deleting a person removes their entries and series via FK cascade`() {
            val now = Instant.now().truncatedTo(ChronoUnit.MILLIS)

            // Create a series for User A's person
            val series = OneOnOneSeries(
                id = OneOnOneSeriesId.generate(),
                userId = UserId(userAId),
                personId = PersonId(personAId),
                cadenceType = CadenceType.WEEKLY,
                customIntervalDays = null,
                templateMarkdown = null,
                createdAt = now,
                updatedAt = now
            )
            seriesRepository.save(series)

            // Create an entry with agenda items
            val entry = OneOnOneEntry(
                id = OneOnOneEntryId.generate(),
                userId = UserId(userAId),
                personId = PersonId(personAId),
                meetingDate = now,
                agendaItems = listOf(
                    AgendaItem(
                        id = AgendaItemId.generate(),
                        text = "Agenda item",
                        checked = false,
                        displayOrder = 0,
                        createdAt = now
                    )
                ),
                notesMarkdown = "notes",
                outcomesMarkdown = null,
                sensitive = false,
                createdAt = now,
                updatedAt = now
            )
            entryRepository.save(entry)

            // Delete the person directly via SQL (simulating cascade from person deletion)
            jdbcTemplate.update("DELETE FROM persons WHERE id = ?", personAId)

            // Verify series is gone
            val seriesCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM one_on_one_series WHERE person_id = ?",
                Int::class.java,
                personAId
            )!!
            seriesCount shouldBe 0

            // Verify entries are gone
            val entryCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM one_on_one_entries WHERE person_id = ?",
                Int::class.java,
                personAId
            )!!
            entryCount shouldBe 0

            // Verify agenda items are gone
            val agendaCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM agenda_items WHERE entry_id = ?",
                Int::class.java,
                entry.id.value
            )!!
            agendaCount shouldBe 0
        }
    }

    @Nested
    inner class FindLatestMeetingDateTests {

        @Test
        fun `findLatestMeetingDate returns the most recent meeting date`() {
            val date1 = Instant.parse("2025-01-01T10:00:00Z")
            val date2 = Instant.parse("2025-02-15T14:00:00Z")
            val date3 = Instant.parse("2025-03-20T09:00:00Z")

            createAndSaveEntry(userAId, personAId, date1)
            createAndSaveEntry(userAId, personAId, date2)
            createAndSaveEntry(userAId, personAId, date3)

            val latestDate = entryRepository.findLatestMeetingDate(UserId(userAId), PersonId(personAId))

            latestDate.shouldNotBeNull()
            latestDate shouldBe date3
        }

        @Test
        fun `findLatestMeetingDate returns null when no entries exist`() {
            val latestDate = entryRepository.findLatestMeetingDate(UserId(userAId), PersonId(personAId))
            latestDate.shouldBeNull()
        }

        @Test
        fun `findLatestMeetingDate is scoped by userId and personId`() {
            val date1 = Instant.parse("2025-01-01T10:00:00Z")
            val date2 = Instant.parse("2025-06-01T10:00:00Z")

            createAndSaveEntry(userAId, personAId, date1)
            createAndSaveEntry(userBId, personBId, date2)

            // User A should only see their own latest date
            val latestA = entryRepository.findLatestMeetingDate(UserId(userAId), PersonId(personAId))
            latestA.shouldNotBeNull()
            latestA shouldBe date1

            // User B should only see their own latest date
            val latestB = entryRepository.findLatestMeetingDate(UserId(userBId), PersonId(personBId))
            latestB.shouldNotBeNull()
            latestB shouldBe date2
        }
    }

    // Helper method to create and save an entry
    private fun createAndSaveEntry(userId: UUID, personId: UUID, meetingDate: Instant): OneOnOneEntry {
        val entry = OneOnOneEntry(
            id = OneOnOneEntryId.generate(),
            userId = UserId(userId),
            personId = PersonId(personId),
            meetingDate = meetingDate,
            agendaItems = emptyList(),
            notesMarkdown = "Notes for meeting at $meetingDate",
            outcomesMarkdown = null,
            sensitive = false,
            createdAt = Instant.now().truncatedTo(ChronoUnit.MILLIS),
            updatedAt = Instant.now().truncatedTo(ChronoUnit.MILLIS)
        )
        return entryRepository.save(entry)
    }
}
