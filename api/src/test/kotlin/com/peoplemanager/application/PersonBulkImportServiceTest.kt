package com.peoplemanager.application

import com.peoplemanager.application.commands.BulkImportPersonsCommand
import com.peoplemanager.application.port.output.PersonRepository
import com.peoplemanager.domain.MoraleStatus
import com.peoplemanager.domain.Person
import com.peoplemanager.domain.PersonId
import com.peoplemanager.domain.UserId
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.time.LocalDate

class PersonBulkImportServiceTest {

    private lateinit var personRepository: PersonRepository
    private lateinit var service: PersonBulkImportService

    private val userId = UserId.generate()

    @BeforeEach
    fun setUp() {
        personRepository = mockk()
        service = PersonBulkImportService(personRepository)
    }

    private fun csvStream(content: String) = ByteArrayInputStream(content.toByteArray(Charsets.UTF_8))

    private fun command(csv: String) = BulkImportPersonsCommand(
        userId = userId,
        csvInputStream = csvStream(csv)
    )

    @Test
    fun `should import valid CSV and create persons`() {
        val csv = """
            name,email,role_title
            Alice,alice@example.com,Engineer
            Bob,bob@example.com,Designer
        """.trimIndent()

        val personSlot = slot<Person>()
        every { personRepository.save(capture(personSlot)) } answers { personSlot.captured }

        val result = service.importPersonsFromCsv(command(csv))

        result.successCount shouldBe 2
        result.errorCount shouldBe 0
        result.createdPersonIds shouldHaveSize 2
        result.errors.shouldBeEmpty()
        verify(exactly = 2) { personRepository.save(any()) }
    }

    @Test
    fun `should set userId on all created persons`() {
        val csv = """
            name
            Alice
        """.trimIndent()

        val savedPersons = mutableListOf<Person>()
        every { personRepository.save(any()) } answers {
            val person = firstArg<Person>()
            savedPersons.add(person)
            person
        }

        service.importPersonsFromCsv(command(csv))

        savedPersons shouldHaveSize 1
        savedPersons[0].userId shouldBe userId
    }

    @Test
    fun `should set default morale status to UNKNOWN`() {
        val csv = """
            name
            Alice
        """.trimIndent()

        val savedPersons = mutableListOf<Person>()
        every { personRepository.save(any()) } answers {
            val person = firstArg<Person>()
            savedPersons.add(person)
            person
        }

        service.importPersonsFromCsv(command(csv))

        savedPersons[0].moraleStatus shouldBe MoraleStatus.UNKNOWN
    }

    @Test
    fun `should map all CSV fields to person correctly`() {
        val csv = """
            name,preferred_name,role_title,timezone,start_date,email,tags
            Alice Smith,Ali,Senior Engineer,America/New_York,2023-01-15,alice@example.com,engineering|senior
        """.trimIndent()

        val savedPersons = mutableListOf<Person>()
        every { personRepository.save(any()) } answers {
            val person = firstArg<Person>()
            savedPersons.add(person)
            person
        }

        service.importPersonsFromCsv(command(csv))

        savedPersons[0].apply {
            name shouldBe "Alice Smith"
            preferredName shouldBe "Ali"
            roleTitle shouldBe "Senior Engineer"
            timezone shouldBe "America/New_York"
            startDate shouldBe LocalDate.of(2023, 1, 15)
            email shouldBe "alice@example.com"
            tags shouldBe listOf("engineering", "senior")
        }
    }

    @Test
    fun `should return errors for invalid rows without stopping valid ones`() {
        val csv = """
            name,start_date
            Alice,2023-01-15
            ,2023-02-01
            Charlie,2023-03-01
        """.trimIndent()

        val personSlot = slot<Person>()
        every { personRepository.save(capture(personSlot)) } answers { personSlot.captured }

        val result = service.importPersonsFromCsv(command(csv))

        result.successCount shouldBe 2
        result.errorCount shouldBe 1
        result.errors shouldHaveSize 1
        result.errors[0] shouldContain "Row 3"
    }

    @Test
    fun `should return error when CSV is empty`() {
        val result = service.importPersonsFromCsv(command(""))

        result.successCount shouldBe 0
        result.errorCount shouldBe 1
        result.errors shouldHaveSize 1
        result.errors[0] shouldContain "empty"
    }

    @Test
    fun `should return error when CSV has no name header`() {
        val csv = """
            email,role_title
            alice@example.com,Engineer
        """.trimIndent()

        val result = service.importPersonsFromCsv(command(csv))

        result.successCount shouldBe 0
        result.errorCount shouldBe 1
        result.errors[0] shouldContain "name"
    }

    @Test
    fun `should return error when CSV exceeds max rows`() {
        val header = "name"
        val rows = (1..501).joinToString("\n") { "Person $it" }
        val csv = "$header\n$rows"

        val result = service.importPersonsFromCsv(command(csv))

        result.successCount shouldBe 0
        result.errorCount shouldBe 1
        result.errors[0] shouldContain "500"
    }

    @Test
    fun `should handle repository save failure gracefully`() {
        val csv = """
            name
            Alice
            Bob
        """.trimIndent()

        var callCount = 0
        every { personRepository.save(any()) } answers {
            callCount++
            if (callCount == 1) throw RuntimeException("DB connection failed")
            firstArg()
        }

        val result = service.importPersonsFromCsv(command(csv))

        result.successCount shouldBe 1
        result.errorCount shouldBe 1
        result.errors[0] shouldContain "Alice"
        result.errors[0] shouldContain "DB connection failed"
    }

    @Test
    fun `should generate unique PersonId for each imported person`() {
        val csv = """
            name
            Alice
            Bob
            Charlie
        """.trimIndent()

        val savedPersons = mutableListOf<Person>()
        every { personRepository.save(any()) } answers {
            val person = firstArg<Person>()
            savedPersons.add(person)
            person
        }

        service.importPersonsFromCsv(command(csv))

        val ids = savedPersons.map { it.id }
        ids.toSet() shouldHaveSize 3
    }

    @Test
    fun `should return empty createdPersonIds when all rows fail`() {
        val csv = """
            name,start_date
            ,invalid
            ,also-invalid
        """.trimIndent()

        val result = service.importPersonsFromCsv(command(csv))

        result.successCount shouldBe 0
        result.createdPersonIds.shouldBeEmpty()
        result.errorCount shouldBe result.errors.size
    }

    @Test
    fun `should handle CSV with header only`() {
        val csv = "name,email,role_title"

        val result = service.importPersonsFromCsv(command(csv))

        result.successCount shouldBe 0
        result.errors shouldHaveSize 1
        result.errors[0] shouldContain "no data rows"
    }
}
