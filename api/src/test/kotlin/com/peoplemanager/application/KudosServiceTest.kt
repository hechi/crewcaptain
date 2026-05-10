package com.peoplemanager.application

import com.peoplemanager.application.commands.CreateKudosCommand
import com.peoplemanager.application.commands.DeleteKudosCommand
import com.peoplemanager.application.ports.KudosRepository
import com.peoplemanager.application.ports.PersonRepository
import com.peoplemanager.application.queries.GetKudosQuery
import com.peoplemanager.application.queries.ListAllKudosQuery
import com.peoplemanager.application.queries.ListKudosByPersonQuery
import com.peoplemanager.domain.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import java.time.LocalDate

class KudosServiceTest {

    private val personRepository = mockk<PersonRepository>()
    private val kudosRepository = mockk<KudosRepository>()

    private val service = KudosService(personRepository, kudosRepository)

    private val userId = UserId.generate()
    private val personId = PersonId.generate()
    private val person = Person(
        id = personId,
        userId = userId,
        name = "Test Person"
    )

    @BeforeEach
    fun setup() {
        clearAllMocks()
    }

    @Nested
    inner class CreateKudosTests {

        @Test
        fun `should create kudos with all fields`() {
            every { personRepository.findByIdAndUserId(personId, userId) } returns person
            every { kudosRepository.save(any()) } answers { firstArg() }

            val command = CreateKudosCommand(
                userId = userId,
                personId = personId,
                date = LocalDate.of(2026, 5, 10),
                text = "Great job on the presentation!",
                tags = listOf("impact", "collaboration")
            )

            val result = service.createKudos(command)

            result.text shouldBe "Great job on the presentation!"
            result.date shouldBe LocalDate.of(2026, 5, 10)
            result.tags shouldBe listOf("impact", "collaboration")
            result.userId shouldBe userId
            result.personId shouldBe personId
            verify { kudosRepository.save(any()) }
        }

        @Test
        fun `should create kudos with minimal fields`() {
            every { personRepository.findByIdAndUserId(personId, userId) } returns person
            every { kudosRepository.save(any()) } answers { firstArg() }

            val command = CreateKudosCommand(
                userId = userId,
                personId = personId,
                date = LocalDate.of(2026, 5, 10),
                text = "Well done!"
            )

            val result = service.createKudos(command)

            result.text shouldBe "Well done!"
            result.tags shouldBe emptyList()
        }

        @Test
        fun `should throw PersonNotFoundException when person not found`() {
            every { personRepository.findByIdAndUserId(personId, userId) } returns null

            val command = CreateKudosCommand(
                userId = userId,
                personId = personId,
                date = LocalDate.of(2026, 5, 10),
                text = "Kudos!"
            )

            shouldThrow<PersonNotFoundException> {
                service.createKudos(command)
            }
        }
    }

    @Nested
    inner class DeleteKudosTests {

        private val kudosId = KudosId.generate()

        @Test
        fun `should delete kudos successfully`() {
            every { kudosRepository.deleteByIdAndUserIdAndPersonId(kudosId, userId, personId) } returns true

            service.deleteKudos(DeleteKudosCommand(userId, personId, kudosId))

            verify { kudosRepository.deleteByIdAndUserIdAndPersonId(kudosId, userId, personId) }
        }

        @Test
        fun `should throw KudosNotFoundException when not found`() {
            every { kudosRepository.deleteByIdAndUserIdAndPersonId(kudosId, userId, personId) } returns false

            shouldThrow<KudosNotFoundException> {
                service.deleteKudos(DeleteKudosCommand(userId, personId, kudosId))
            }
        }
    }

    @Nested
    inner class GetKudosTests {

        private val kudosId = KudosId.generate()

        @Test
        fun `should return kudos when found`() {
            val kudos = Kudos(
                id = kudosId,
                userId = userId,
                personId = personId,
                date = LocalDate.of(2026, 5, 10),
                text = "Great work!"
            )
            every { kudosRepository.findByIdAndUserIdAndPersonId(kudosId, userId, personId) } returns kudos

            val result = service.getKudos(GetKudosQuery(userId, personId, kudosId))

            result.id shouldBe kudosId
            result.text shouldBe "Great work!"
        }

        @Test
        fun `should throw KudosNotFoundException when not found`() {
            every { kudosRepository.findByIdAndUserIdAndPersonId(kudosId, userId, personId) } returns null

            shouldThrow<KudosNotFoundException> {
                service.getKudos(GetKudosQuery(userId, personId, kudosId))
            }
        }
    }

    @Nested
    inner class ListKudosByPersonTests {

        @Test
        fun `should return paginated kudos for person`() {
            val kudosList = listOf(
                Kudos(
                    id = KudosId.generate(),
                    userId = userId,
                    personId = personId,
                    date = LocalDate.of(2026, 5, 10),
                    text = "Great work!"
                )
            )
            val pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "date"))
            every { personRepository.findByIdAndUserId(personId, userId) } returns person
            every { kudosRepository.findAllByUserIdAndPersonId(userId, personId, pageable) } returns
                PageImpl(kudosList, pageable, 1)

            val result = service.listKudosByPerson(ListKudosByPersonQuery(userId, personId))

            result.totalElements shouldBe 1
            result.content.size shouldBe 1
        }

        @Test
        fun `should throw PersonNotFoundException when person not found`() {
            every { personRepository.findByIdAndUserId(personId, userId) } returns null

            shouldThrow<PersonNotFoundException> {
                service.listKudosByPerson(ListKudosByPersonQuery(userId, personId))
            }
        }
    }

    @Nested
    inner class ListAllKudosTests {

        @Test
        fun `should return all kudos for user`() {
            val kudosList = listOf(
                Kudos(
                    id = KudosId.generate(),
                    userId = userId,
                    personId = personId,
                    date = LocalDate.of(2026, 5, 10),
                    text = "Great work!"
                )
            )
            val pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "date"))
            every { kudosRepository.findAllByUserId(userId, pageable) } returns
                PageImpl(kudosList, pageable, 1)

            val result = service.listAllKudos(ListAllKudosQuery(userId))

            result.totalElements shouldBe 1
        }
    }
}
