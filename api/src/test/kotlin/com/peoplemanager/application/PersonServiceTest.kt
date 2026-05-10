package com.peoplemanager.application

import com.peoplemanager.application.commands.AddRememberItemCommand
import com.peoplemanager.application.commands.CreatePersonCommand
import com.peoplemanager.application.commands.DeletePersonCommand
import com.peoplemanager.application.commands.RemoveRememberItemCommand
import com.peoplemanager.application.commands.ReorderRememberItemsCommand
import com.peoplemanager.application.commands.RestorePersonCommand
import com.peoplemanager.application.commands.SetMoraleCommand
import com.peoplemanager.application.commands.UpdatePersonCommand
import com.peoplemanager.application.ports.PersonRepository
import com.peoplemanager.application.queries.GetPersonQuery
import com.peoplemanager.application.queries.ListDeletedPersonsQuery
import com.peoplemanager.application.queries.ListPersonsQuery
import com.peoplemanager.domain.MoraleStatus
import com.peoplemanager.domain.Person
import com.peoplemanager.domain.PersonId
import com.peoplemanager.domain.PinnedRememberItem
import com.peoplemanager.domain.RememberItemId
import com.peoplemanager.domain.UserId
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import java.time.Instant
import java.time.LocalDate

class PersonServiceTest {

    private lateinit var personRepository: PersonRepository
    private lateinit var personService: PersonService

    private val userId = UserId.generate()
    private val personId = PersonId.generate()

    @BeforeEach
    fun setUp() {
        personRepository = mockk()
        personService = PersonService(personRepository)
    }

    private fun createTestPerson(
        id: PersonId = personId,
        uid: UserId = userId,
        name: String = "Alice Smith",
        moraleStatus: MoraleStatus = MoraleStatus.UNKNOWN,
        pinnedRememberItems: List<PinnedRememberItem> = emptyList()
    ) = Person(
        id = id,
        userId = uid,
        name = name,
        preferredName = "Ali",
        roleTitle = "Engineer",
        timezone = "UTC",
        startDate = LocalDate.of(2024, 1, 15),
        email = "alice@example.com",
        tags = listOf("team-a"),
        moraleStatus = moraleStatus,
        moraleNote = null,
        pinnedRememberItems = pinnedRememberItems
    )

    @Nested
    inner class CreatePerson {

        @Test
        fun `should save person with correct fields, UNKNOWN morale, and empty remember items`() {
            val command = CreatePersonCommand(
                userId = userId,
                name = "Bob Jones",
                preferredName = "Bobby",
                roleTitle = "Designer",
                timezone = "America/New_York",
                startDate = LocalDate.of(2024, 3, 1),
                email = "bob@example.com",
                tags = listOf("design", "frontend")
            )

            val personSlot = slot<Person>()
            every { personRepository.save(capture(personSlot)) } answers { personSlot.captured }

            val result = personService.createPerson(command)

            result.userId shouldBe userId
            result.name shouldBe "Bob Jones"
            result.preferredName shouldBe "Bobby"
            result.roleTitle shouldBe "Designer"
            result.timezone shouldBe "America/New_York"
            result.startDate shouldBe LocalDate.of(2024, 3, 1)
            result.email shouldBe "bob@example.com"
            result.tags shouldBe listOf("design", "frontend")
            result.moraleStatus shouldBe MoraleStatus.UNKNOWN
            result.moraleNote shouldBe null
            result.pinnedRememberItems shouldBe emptyList()

            verify(exactly = 1) { personRepository.save(any()) }
        }
    }

    @Nested
    inner class UpdatePerson {

        @Test
        fun `should load existing person, apply updates, and save`() {
            val existing = createTestPerson()
            val command = UpdatePersonCommand(
                userId = userId,
                personId = personId,
                name = "Alice Johnson",
                preferredName = "AJ",
                roleTitle = "Senior Engineer",
                timezone = "Europe/London",
                startDate = LocalDate.of(2024, 2, 1),
                email = "alice.j@example.com",
                tags = listOf("team-b")
            )

            every { personRepository.findByIdAndUserId(personId, userId) } returns existing
            val personSlot = slot<Person>()
            every { personRepository.save(capture(personSlot)) } answers { personSlot.captured }

            val result = personService.updatePerson(command)

            result.id shouldBe personId
            result.userId shouldBe userId
            result.name shouldBe "Alice Johnson"
            result.preferredName shouldBe "AJ"
            result.roleTitle shouldBe "Senior Engineer"
            result.timezone shouldBe "Europe/London"
            result.startDate shouldBe LocalDate.of(2024, 2, 1)
            result.email shouldBe "alice.j@example.com"
            result.tags shouldBe listOf("team-b")

            verify(exactly = 1) { personRepository.findByIdAndUserId(personId, userId) }
            verify(exactly = 1) { personRepository.save(any()) }
        }

        @Test
        fun `should throw PersonNotFoundException when person not found`() {
            val command = UpdatePersonCommand(
                userId = userId,
                personId = personId,
                name = "Updated Name"
            )

            every { personRepository.findByIdAndUserId(personId, userId) } returns null

            val exception = shouldThrow<PersonNotFoundException> {
                personService.updatePerson(command)
            }
            exception.personId shouldBe personId
        }
    }

    @Nested
    inner class DeletePerson {

        @Test
        fun `should call softDeleteByIdAndUserId and succeed`() {
            val command = DeletePersonCommand(userId = userId, personId = personId)

            every { personRepository.softDeleteByIdAndUserId(personId, userId) } returns true

            personService.deletePerson(command)

            verify(exactly = 1) { personRepository.softDeleteByIdAndUserId(personId, userId) }
        }

        @Test
        fun `should throw PersonNotFoundException when person not found`() {
            val command = DeletePersonCommand(userId = userId, personId = personId)

            every { personRepository.softDeleteByIdAndUserId(personId, userId) } returns false

            val exception = shouldThrow<PersonNotFoundException> {
                personService.deletePerson(command)
            }
            exception.personId shouldBe personId
        }
    }

    @Nested
    inner class RestorePerson {

        @Test
        fun `should call restoreByIdAndUserId and return restored person`() {
            val command = RestorePersonCommand(userId = userId, personId = personId)
            val restoredPerson = createTestPerson()

            every { personRepository.restoreByIdAndUserId(personId, userId) } returns true
            every { personRepository.findByIdAndUserId(personId, userId) } returns restoredPerson

            val result = personService.restorePerson(command)

            result shouldBe restoredPerson
            verify(exactly = 1) { personRepository.restoreByIdAndUserId(personId, userId) }
            verify(exactly = 1) { personRepository.findByIdAndUserId(personId, userId) }
        }

        @Test
        fun `should throw PersonNotFoundException when deleted person not found`() {
            val command = RestorePersonCommand(userId = userId, personId = personId)

            every { personRepository.restoreByIdAndUserId(personId, userId) } returns false

            val exception = shouldThrow<PersonNotFoundException> {
                personService.restorePerson(command)
            }
            exception.personId shouldBe personId
        }
    }

    @Nested
    inner class ListDeletedPersons {

        @Test
        fun `should delegate to repository with correct pageable`() {
            val query = ListDeletedPersonsQuery(userId = userId, page = 0, size = 20)
            val persons = listOf(createTestPerson())
            val page = PageImpl(persons, PageRequest.of(0, 20), 1)

            every { personRepository.findAllDeletedByUserId(userId, any<Pageable>()) } returns page

            val result = personService.listDeletedPersons(query)

            result.content shouldBe persons
            result.totalElements shouldBe 1

            verify(exactly = 1) { personRepository.findAllDeletedByUserId(userId, any<Pageable>()) }
        }
    }

    @Nested
    inner class GetPerson {

        @Test
        fun `should return person when found`() {
            val person = createTestPerson()
            val query = GetPersonQuery(userId = userId, personId = personId)

            every { personRepository.findByIdAndUserId(personId, userId) } returns person

            val result = personService.getPerson(query)

            result shouldBe person
            verify(exactly = 1) { personRepository.findByIdAndUserId(personId, userId) }
        }

        @Test
        fun `should throw PersonNotFoundException when not found`() {
            val query = GetPersonQuery(userId = userId, personId = personId)

            every { personRepository.findByIdAndUserId(personId, userId) } returns null

            val exception = shouldThrow<PersonNotFoundException> {
                personService.getPerson(query)
            }
            exception.personId shouldBe personId
        }
    }

    @Nested
    inner class ListPersons {

        @Test
        fun `should delegate to repository with correct pageable and filters`() {
            val query = ListPersonsQuery(
                userId = userId,
                page = 0,
                size = 20,
                tagFilter = "team-a",
                moraleFilter = MoraleStatus.GREEN
            )
            val expectedPageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "name"))
            val persons = listOf(createTestPerson())
            val page = PageImpl(persons, expectedPageable, 1)

            every {
                personRepository.findAllByUserId(userId, any<Pageable>(), "team-a", MoraleStatus.GREEN)
            } returns page

            val result = personService.listPersons(query)

            result.content shouldBe persons
            result.totalElements shouldBe 1

            verify(exactly = 1) {
                personRepository.findAllByUserId(userId, any<Pageable>(), "team-a", MoraleStatus.GREEN)
            }
        }
    }

    @Nested
    inner class SetMorale {

        @Test
        fun `should load person, update morale, and save`() {
            val person = createTestPerson()
            val command = SetMoraleCommand(
                userId = userId,
                personId = personId,
                status = MoraleStatus.GREEN,
                note = "Doing great"
            )

            every { personRepository.findByIdAndUserId(personId, userId) } returns person
            val personSlot = slot<Person>()
            every { personRepository.save(capture(personSlot)) } answers { personSlot.captured }

            val result = personService.setMorale(command)

            result.moraleStatus shouldBe MoraleStatus.GREEN
            result.moraleNote shouldBe "Doing great"
            result.id shouldBe personId

            verify(exactly = 1) { personRepository.findByIdAndUserId(personId, userId) }
            verify(exactly = 1) { personRepository.save(any()) }
        }

        @Test
        fun `should throw PersonNotFoundException when person not found`() {
            val command = SetMoraleCommand(
                userId = userId,
                personId = personId,
                status = MoraleStatus.RED,
                note = null
            )

            every { personRepository.findByIdAndUserId(personId, userId) } returns null

            val exception = shouldThrow<PersonNotFoundException> {
                personService.setMorale(command)
            }
            exception.personId shouldBe personId
        }
    }

    @Nested
    inner class AddRememberItem {

        @Test
        fun `should load person, add item, save, and return updated list`() {
            val person = createTestPerson()
            val command = AddRememberItemCommand(
                userId = userId,
                personId = personId,
                text = "Remember to follow up on project"
            )

            every { personRepository.findByIdAndUserId(personId, userId) } returns person
            val personSlot = slot<Person>()
            every { personRepository.save(capture(personSlot)) } answers { personSlot.captured }

            val result = personService.addRememberItem(command)

            result shouldHaveSize 1
            result[0].text shouldBe "Remember to follow up on project"
            result[0].displayOrder shouldBe 0

            verify(exactly = 1) { personRepository.save(any()) }
        }
    }

    @Nested
    inner class RemoveRememberItem {

        @Test
        fun `should load person, remove item, save, and return updated list`() {
            val itemId = RememberItemId.generate()
            val item1 = PinnedRememberItem(
                id = itemId,
                text = "Item to remove",
                displayOrder = 0,
                createdAt = Instant.now()
            )
            val item2 = PinnedRememberItem(
                id = RememberItemId.generate(),
                text = "Item to keep",
                displayOrder = 1,
                createdAt = Instant.now()
            )
            val person = createTestPerson(pinnedRememberItems = listOf(item1, item2))
            val command = RemoveRememberItemCommand(
                userId = userId,
                personId = personId,
                itemId = itemId
            )

            every { personRepository.findByIdAndUserId(personId, userId) } returns person
            val personSlot = slot<Person>()
            every { personRepository.save(capture(personSlot)) } answers { personSlot.captured }

            val result = personService.removeRememberItem(command)

            result shouldHaveSize 1
            result[0].text shouldBe "Item to keep"
            result[0].displayOrder shouldBe 0

            verify(exactly = 1) { personRepository.save(any()) }
        }
    }

    @Nested
    inner class ReorderRememberItems {

        @Test
        fun `should load person, reorder items, save, and return updated list`() {
            val itemId1 = RememberItemId.generate()
            val itemId2 = RememberItemId.generate()
            val itemId3 = RememberItemId.generate()
            val item1 = PinnedRememberItem(id = itemId1, text = "First", displayOrder = 0, createdAt = Instant.now())
            val item2 = PinnedRememberItem(id = itemId2, text = "Second", displayOrder = 1, createdAt = Instant.now())
            val item3 = PinnedRememberItem(id = itemId3, text = "Third", displayOrder = 2, createdAt = Instant.now())
            val person = createTestPerson(pinnedRememberItems = listOf(item1, item2, item3))

            val command = ReorderRememberItemsCommand(
                userId = userId,
                personId = personId,
                orderedIds = listOf(itemId3, itemId1, itemId2)
            )

            every { personRepository.findByIdAndUserId(personId, userId) } returns person
            val personSlot = slot<Person>()
            every { personRepository.save(capture(personSlot)) } answers { personSlot.captured }

            val result = personService.reorderRememberItems(command)

            result shouldHaveSize 3
            result[0].text shouldBe "Third"
            result[0].displayOrder shouldBe 0
            result[1].text shouldBe "First"
            result[1].displayOrder shouldBe 1
            result[2].text shouldBe "Second"
            result[2].displayOrder shouldBe 2

            verify(exactly = 1) { personRepository.save(any()) }
        }
    }
}
