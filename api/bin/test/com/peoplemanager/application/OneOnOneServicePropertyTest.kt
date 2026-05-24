package com.peoplemanager.application

import com.peoplemanager.application.commands.AgendaItemInput
import com.peoplemanager.application.commands.CreateOneOnOneEntryCommand
import com.peoplemanager.application.commands.UpsertOneOnOneSeriesCommand
import com.peoplemanager.application.ports.OneOnOneEntryRepository
import com.peoplemanager.application.ports.OneOnOneSeriesRepository
import com.peoplemanager.application.ports.PersonRepository
import com.peoplemanager.application.queries.GetOneOnOneEntryQuery
import com.peoplemanager.domain.*
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.time.Instant

/**
 * Property-based tests for OneOnOneService application layer.
 *
 * **Validates: Requirements 1.1, 1.2, 1.7**
 */
@Tag("property")
class OneOnOneServicePropertyTest {

    private val personRepository = mockk<PersonRepository>()
    private val seriesRepository = mockk<OneOnOneSeriesRepository>()
    private val entryRepository = mockk<OneOnOneEntryRepository>()
    private val auditLogService = mockk<AuditLogService>(relaxed = true)

    private val service = OneOnOneService(personRepository, seriesRepository, entryRepository, auditLogService)

    @BeforeEach
    fun setup() {
        clearAllMocks()
    }

    /**
     * Property 1: Series upsert idempotence
     *
     * For any valid series configuration (cadenceType, customIntervalDays, templateMarkdown),
     * upserting the series for the same (userId, personId) multiple times SHALL always result
     * in exactly one series record with the latest values.
     *
     * **Validates: Requirements 1.1, 1.2, 1.7**
     */
    @Test
    fun `Property 1 - upserting series multiple times results in single record with latest values`() = runBlocking {
        // Arb for CadenceType
        val cadenceTypeArb: Arb<CadenceType> = Arb.enum<CadenceType>()

        // Arb for customIntervalDays (positive int for CUSTOM, null for others)
        val positiveIntArb: Arb<Int> = Arb.int(1..365)

        // Arb for templateMarkdown (nullable string)
        val templateArb: Arb<String?> = Arb.choice(
            Arb.string(1..200).map { it as String? },
            Arb.constant(null)
        )

        // Arb for number of upserts (2 to 5 times)
        val upsertCountArb: Arb<Int> = Arb.int(2..5)

        checkAll(100, cadenceTypeArb, positiveIntArb, templateArb, upsertCountArb) { cadenceType, intervalDays, template, upsertCount ->
            clearAllMocks()

            val userId = UserId.generate()
            val personId = PersonId.generate()
            val person = Person(id = personId, userId = userId, name = "Test Person")

            // Determine valid customIntervalDays based on cadenceType
            val customIntervalDays = if (cadenceType == CadenceType.CUSTOM) intervalDays else null

            // Track the "stored" series to simulate repository state
            var storedSeries: OneOnOneSeries? = null

            every { personRepository.findByIdAndUserId(personId, userId) } returns person
            every { seriesRepository.findByUserIdAndPersonId(userId, personId) } answers { storedSeries }
            every { seriesRepository.save(any()) } answers {
                val saved = firstArg<OneOnOneSeries>()
                storedSeries = saved
                saved
            }

            // Perform multiple upserts with the same configuration
            var lastResult: OneOnOneSeries? = null
            repeat(upsertCount) {
                val command = UpsertOneOnOneSeriesCommand(
                    userId = userId,
                    personId = personId,
                    cadenceType = cadenceType,
                    customIntervalDays = customIntervalDays,
                    templateMarkdown = template
                )
                lastResult = service.upsertSeries(command)
            }

            // Verify: exactly one series record exists (storedSeries is the single record)
            storedSeries shouldBe lastResult

            // Verify: the final result has the latest values
            lastResult!!.userId shouldBe userId
            lastResult!!.personId shouldBe personId
            lastResult!!.cadenceType shouldBe cadenceType
            lastResult!!.customIntervalDays shouldBe customIntervalDays
            lastResult!!.templateMarkdown shouldBe template

            // Verify: save was called exactly upsertCount times (once per upsert)
            verify(exactly = upsertCount) { seriesRepository.save(any()) }

            // Verify: after the first upsert, subsequent upserts reuse the same series ID
            // (the series ID should remain constant across all upserts after the first)
            val seriesId = storedSeries!!.id
            lastResult!!.id shouldBe seriesId
        }
        Unit
    }

    /**
     * Property 2: Entry creation round-trip
     *
     * For any valid entry creation request (valid meetingDate, optional agenda items with non-blank text,
     * optional notes/outcomes, optional sensitive flag), creating the entry and then retrieving it by ID
     * SHALL return an entry with all fields matching the original input.
     *
     * **Validates: Requirements 2.1, 2.3, 2.6, 3.1**
     */
    @Test
    fun `Property 2 - creating an entry and retrieving it returns all fields matching the original input`() = runBlocking {
        // Arb for meetingDate (arbitrary Instant values within a reasonable range)
        val meetingDateArb: Arb<Instant> = Arb.long(0L..4_000_000_000L).map { Instant.ofEpochSecond(it) }

        // Arb for non-blank agenda item text
        val agendaItemTextArb: Arb<String> = Arb.string(1..100, Codepoint.alphanumeric()).filter { it.isNotBlank() }

        // Arb for agenda items list (0 to 5 items)
        val agendaItemsArb: Arb<List<AgendaItemInput>> = Arb.list(
            Arb.bind(agendaItemTextArb, Arb.boolean()) { text, checked ->
                AgendaItemInput(text = text, checked = checked)
            },
            range = 0..5
        )

        // Arb for nullable notes/outcomes markdown
        val nullableStringArb: Arb<String?> = Arb.choice(
            Arb.string(1..200, Codepoint.alphanumeric()).map { it as String? },
            Arb.constant(null)
        )

        // Arb for sensitive flag
        val sensitiveArb: Arb<Boolean> = Arb.boolean()

        checkAll(100, meetingDateArb, agendaItemsArb, nullableStringArb, nullableStringArb, sensitiveArb) { meetingDate, agendaItems, notes, outcomes, sensitive ->
            clearAllMocks()

            val userId = UserId.generate()
            val personId = PersonId.generate()
            val person = Person(id = personId, userId = userId, name = "Test Person")

            // Mock person repository to return a valid person
            every { personRepository.findByIdAndUserId(personId, userId) } returns person

            // Mock series repository to return null (no template - testing direct round-trip)
            every { seriesRepository.findByUserIdAndPersonId(userId, personId) } returns null

            // Track the saved entry to simulate persistence
            var savedEntry: OneOnOneEntry? = null

            every { entryRepository.save(any()) } answers {
                val entry = firstArg<OneOnOneEntry>()
                savedEntry = entry
                entry
            }

            every { entryRepository.findByIdAndUserIdAndPersonId(any(), any(), any()) } answers {
                savedEntry
            }

            // Create the entry
            val command = CreateOneOnOneEntryCommand(
                userId = userId,
                personId = personId,
                meetingDate = meetingDate,
                agendaItems = agendaItems,
                notesMarkdown = notes,
                outcomesMarkdown = outcomes,
                sensitive = sensitive
            )

            val createdEntry = service.createEntry(command)

            // Retrieve the entry
            val query = GetOneOnOneEntryQuery(
                userId = userId,
                personId = personId,
                entryId = createdEntry.id
            )
            val retrievedEntry = service.getEntry(query)

            // Verify all fields match the original input
            retrievedEntry.userId shouldBe userId
            retrievedEntry.personId shouldBe personId
            retrievedEntry.meetingDate shouldBe meetingDate
            retrievedEntry.notesMarkdown shouldBe notes
            retrievedEntry.outcomesMarkdown shouldBe outcomes
            retrievedEntry.sensitive shouldBe sensitive

            // Verify agenda items match
            retrievedEntry.agendaItems.size shouldBe agendaItems.size
            retrievedEntry.agendaItems.forEachIndexed { index, item ->
                item.text shouldBe agendaItems[index].text
                item.checked shouldBe agendaItems[index].checked
                item.displayOrder shouldBe index
            }

            // Verify the created and retrieved entries are the same object (round-trip)
            retrievedEntry shouldBe createdEntry
        }
        Unit
    }

    /**
     * Property 3: Template prefill when notes absent
     *
     * For any Person with a configured series template and any entry creation request
     * where notesMarkdown is null/absent, the created entry's notesMarkdown SHALL equal
     * the series template content.
     *
     * **Validates: Requirements 2.4**
     */
    @Test
    fun `Property 3 - template prefills notes when notes are absent`() = runBlocking {
        // Arb for non-blank template strings (1 to 500 chars)
        val templateArb: Arb<String> = Arb.string(1..500, Codepoint.alphanumeric()).filter { it.isNotBlank() }

        // Arb for meetingDate
        val meetingDateArb: Arb<Instant> = Arb.long(0L..4_000_000_000L).map { Instant.ofEpochSecond(it) }

        // Arb for cadenceType (non-CUSTOM to avoid needing positive interval)
        val cadenceTypeArb: Arb<CadenceType> = Arb.of(CadenceType.WEEKLY, CadenceType.BIWEEKLY, CadenceType.MONTHLY)

        checkAll(100, templateArb, meetingDateArb, cadenceTypeArb) { template, meetingDate, cadenceType ->
            clearAllMocks()

            val userId = UserId.generate()
            val personId = PersonId.generate()
            val person = Person(id = personId, userId = userId, name = "Test Person")

            // Set up a series with the generated template
            val series = OneOnOneSeries(
                id = OneOnOneSeriesId.generate(),
                userId = userId,
                personId = personId,
                cadenceType = cadenceType,
                templateMarkdown = template
            )

            every { personRepository.findByIdAndUserId(personId, userId) } returns person
            every { seriesRepository.findByUserIdAndPersonId(userId, personId) } returns series
            every { entryRepository.save(any()) } answers { firstArg() }

            // Create entry WITHOUT notes (notesMarkdown = null)
            val command = CreateOneOnOneEntryCommand(
                userId = userId,
                personId = personId,
                meetingDate = meetingDate,
                notesMarkdown = null
            )

            val createdEntry = service.createEntry(command)

            // Verify: notes should equal the template content
            createdEntry.notesMarkdown shouldBe template
        }
        Unit
    }

    /**
     * Property 4: Template NOT applied when notes provided
     *
     * For any Person with a configured series template and any entry creation request
     * where notesMarkdown is explicitly provided (even empty string), the created entry's
     * notesMarkdown SHALL equal the provided value, NOT the template.
     *
     * **Validates: Requirements 2.4**
     */
    @Test
    fun `Property 4 - template not applied when notes are explicitly provided`() = runBlocking {
        // Arb for non-blank template strings
        val templateArb: Arb<String> = Arb.string(1..500, Codepoint.alphanumeric()).filter { it.isNotBlank() }

        // Arb for explicitly provided notes (including empty string)
        val providedNotesArb: Arb<String> = Arb.choice(
            Arb.constant(""),
            Arb.string(0..300, Codepoint.alphanumeric())
        )

        // Arb for meetingDate
        val meetingDateArb: Arb<Instant> = Arb.long(0L..4_000_000_000L).map { Instant.ofEpochSecond(it) }

        // Arb for cadenceType (non-CUSTOM to avoid needing positive interval)
        val cadenceTypeArb: Arb<CadenceType> = Arb.of(CadenceType.WEEKLY, CadenceType.BIWEEKLY, CadenceType.MONTHLY)

        checkAll(100, templateArb, providedNotesArb, meetingDateArb, cadenceTypeArb) { template, providedNotes, meetingDate, cadenceType ->
            clearAllMocks()

            val userId = UserId.generate()
            val personId = PersonId.generate()
            val person = Person(id = personId, userId = userId, name = "Test Person")

            // Set up a series with the generated template
            val series = OneOnOneSeries(
                id = OneOnOneSeriesId.generate(),
                userId = userId,
                personId = personId,
                cadenceType = cadenceType,
                templateMarkdown = template
            )

            every { personRepository.findByIdAndUserId(personId, userId) } returns person
            every { seriesRepository.findByUserIdAndPersonId(userId, personId) } returns series
            every { entryRepository.save(any()) } answers { firstArg() }

            // Create entry WITH explicit notes (even empty string)
            val command = CreateOneOnOneEntryCommand(
                userId = userId,
                personId = personId,
                meetingDate = meetingDate,
                notesMarkdown = providedNotes
            )

            val createdEntry = service.createEntry(command)

            // Verify: notes should equal the provided value, NOT the template
            createdEntry.notesMarkdown shouldBe providedNotes
        }
        Unit
    }
}
