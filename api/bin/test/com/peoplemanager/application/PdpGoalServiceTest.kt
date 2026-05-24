package com.peoplemanager.application

import com.peoplemanager.application.commands.*
import com.peoplemanager.application.ports.PdpGoalRepository
import com.peoplemanager.application.ports.PdpUpdateRepository
import com.peoplemanager.application.ports.PersonRepository
import com.peoplemanager.application.queries.CountActivePdpGoalsQuery
import com.peoplemanager.application.queries.GetPdpGoalQuery
import com.peoplemanager.application.queries.ListPdpGoalsByPersonQuery
import com.peoplemanager.application.queries.ListPdpUpdatesByGoalQuery
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

class PdpGoalServiceTest {

    private val personRepository = mockk<PersonRepository>()
    private val pdpGoalRepository = mockk<PdpGoalRepository>()
    private val pdpUpdateRepository = mockk<PdpUpdateRepository>()
    private val auditLogService = mockk<AuditLogService>(relaxed = true)

    private val service = PdpGoalService(personRepository, pdpGoalRepository, pdpUpdateRepository, auditLogService)

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
    inner class CreatePdpGoalTests {

        @Test
        fun `should create PDP goal with all fields`() {
            every { personRepository.findByIdAndUserId(personId, userId) } returns person
            every { pdpGoalRepository.save(any()) } answers { firstArg() }

            val command = CreatePdpGoalCommand(
                userId = userId,
                personId = personId,
                title = "Improve public speaking",
                description = "Practice presentations monthly",
                targetDate = LocalDate.of(2026, 12, 31)
            )

            val result = service.createPdpGoal(command)

            result.title shouldBe "Improve public speaking"
            result.description shouldBe "Practice presentations monthly"
            result.targetDate shouldBe LocalDate.of(2026, 12, 31)
            result.status shouldBe PdpGoalStatus.ACTIVE
            result.userId shouldBe userId
            result.personId shouldBe personId
            verify { pdpGoalRepository.save(any()) }
        }

        @Test
        fun `should create PDP goal with minimal fields`() {
            every { personRepository.findByIdAndUserId(personId, userId) } returns person
            every { pdpGoalRepository.save(any()) } answers { firstArg() }

            val command = CreatePdpGoalCommand(
                userId = userId,
                personId = personId,
                title = "Learn Kotlin"
            )

            val result = service.createPdpGoal(command)

            result.title shouldBe "Learn Kotlin"
            result.description shouldBe null
            result.targetDate shouldBe null
        }

        @Test
        fun `should throw PersonNotFoundException when person not found`() {
            every { personRepository.findByIdAndUserId(personId, userId) } returns null

            val command = CreatePdpGoalCommand(
                userId = userId,
                personId = personId,
                title = "Goal"
            )

            shouldThrow<PersonNotFoundException> {
                service.createPdpGoal(command)
            }
        }
    }

    @Nested
    inner class UpdatePdpGoalTests {

        private val goalId = PdpGoalId.generate()
        private val existingGoal = PdpGoal(
            id = goalId,
            userId = userId,
            personId = personId,
            title = "Original title",
            description = "Original description",
            targetDate = LocalDate.of(2026, 12, 31)
        )

        @Test
        fun `should update PDP goal fields`() {
            every { pdpGoalRepository.findByIdAndUserIdAndPersonId(goalId, userId, personId) } returns existingGoal
            every { pdpGoalRepository.save(any()) } answers { firstArg() }

            val command = UpdatePdpGoalCommand(
                userId = userId,
                personId = personId,
                goalId = goalId,
                title = "Updated title"
            )

            val result = service.updatePdpGoal(command)

            result.title shouldBe "Updated title"
            result.description shouldBe "Original description"
        }

        @Test
        fun `should throw PdpGoalNotFoundException when not found`() {
            every { pdpGoalRepository.findByIdAndUserIdAndPersonId(goalId, userId, personId) } returns null

            val command = UpdatePdpGoalCommand(
                userId = userId,
                personId = personId,
                goalId = goalId,
                title = "Updated"
            )

            shouldThrow<PdpGoalNotFoundException> {
                service.updatePdpGoal(command)
            }
        }
    }

    @Nested
    inner class StatusTransitionTests {

        private val goalId = PdpGoalId.generate()
        private val activeGoal = PdpGoal(
            id = goalId,
            userId = userId,
            personId = personId,
            title = "Active goal",
            status = PdpGoalStatus.ACTIVE
        )
        private val pausedGoal = activeGoal.copy(status = PdpGoalStatus.PAUSED)

        @Test
        fun `should achieve an active goal`() {
            every { pdpGoalRepository.findByIdAndUserIdAndPersonId(goalId, userId, personId) } returns activeGoal
            every { pdpGoalRepository.save(any()) } answers { firstArg() }

            val result = service.achievePdpGoal(AchievePdpGoalCommand(userId, personId, goalId))

            result.status shouldBe PdpGoalStatus.ACHIEVED
        }

        @Test
        fun `should pause an active goal`() {
            every { pdpGoalRepository.findByIdAndUserIdAndPersonId(goalId, userId, personId) } returns activeGoal
            every { pdpGoalRepository.save(any()) } answers { firstArg() }

            val result = service.pausePdpGoal(PausePdpGoalCommand(userId, personId, goalId))

            result.status shouldBe PdpGoalStatus.PAUSED
        }

        @Test
        fun `should drop an active goal`() {
            every { pdpGoalRepository.findByIdAndUserIdAndPersonId(goalId, userId, personId) } returns activeGoal
            every { pdpGoalRepository.save(any()) } answers { firstArg() }

            val result = service.dropPdpGoal(DropPdpGoalCommand(userId, personId, goalId))

            result.status shouldBe PdpGoalStatus.DROPPED
        }

        @Test
        fun `should resume a paused goal`() {
            every { pdpGoalRepository.findByIdAndUserIdAndPersonId(goalId, userId, personId) } returns pausedGoal
            every { pdpGoalRepository.save(any()) } answers { firstArg() }

            val result = service.resumePdpGoal(ResumePdpGoalCommand(userId, personId, goalId))

            result.status shouldBe PdpGoalStatus.ACTIVE
        }

        @Test
        fun `should throw PdpGoalNotFoundException for achieve when not found`() {
            every { pdpGoalRepository.findByIdAndUserIdAndPersonId(goalId, userId, personId) } returns null

            shouldThrow<PdpGoalNotFoundException> {
                service.achievePdpGoal(AchievePdpGoalCommand(userId, personId, goalId))
            }
        }

        @Test
        fun `should throw IllegalArgumentException when achieving a paused goal`() {
            every { pdpGoalRepository.findByIdAndUserIdAndPersonId(goalId, userId, personId) } returns pausedGoal

            shouldThrow<IllegalArgumentException> {
                service.achievePdpGoal(AchievePdpGoalCommand(userId, personId, goalId))
            }
        }

        @Test
        fun `should throw IllegalArgumentException when resuming an active goal`() {
            every { pdpGoalRepository.findByIdAndUserIdAndPersonId(goalId, userId, personId) } returns activeGoal

            shouldThrow<IllegalArgumentException> {
                service.resumePdpGoal(ResumePdpGoalCommand(userId, personId, goalId))
            }
        }
    }

    @Nested
    inner class DeletePdpGoalTests {

        private val goalId = PdpGoalId.generate()

        @Test
        fun `should delete PDP goal successfully`() {
            val existing = PdpGoal(
                id = goalId,
                userId = userId,
                personId = personId,
                title = "Test Goal"
            )
            every { pdpGoalRepository.findByIdAndUserIdAndPersonId(goalId, userId, personId) } returns existing
            every { pdpGoalRepository.deleteByIdAndUserIdAndPersonId(goalId, userId, personId) } returns true

            service.deletePdpGoal(DeletePdpGoalCommand(userId, personId, goalId))

            verify { pdpGoalRepository.deleteByIdAndUserIdAndPersonId(goalId, userId, personId) }
        }

        @Test
        fun `should throw PdpGoalNotFoundException when not found`() {
            every { pdpGoalRepository.findByIdAndUserIdAndPersonId(goalId, userId, personId) } returns null

            shouldThrow<PdpGoalNotFoundException> {
                service.deletePdpGoal(DeletePdpGoalCommand(userId, personId, goalId))
            }
        }
    }

    @Nested
    inner class GetPdpGoalTests {

        private val goalId = PdpGoalId.generate()

        @Test
        fun `should return PDP goal when found`() {
            val goal = PdpGoal(
                id = goalId,
                userId = userId,
                personId = personId,
                title = "Test goal"
            )
            every { pdpGoalRepository.findByIdAndUserIdAndPersonId(goalId, userId, personId) } returns goal

            val result = service.getPdpGoal(GetPdpGoalQuery(userId, personId, goalId))

            result.id shouldBe goalId
            result.title shouldBe "Test goal"
        }

        @Test
        fun `should throw PdpGoalNotFoundException when not found`() {
            every { pdpGoalRepository.findByIdAndUserIdAndPersonId(goalId, userId, personId) } returns null

            shouldThrow<PdpGoalNotFoundException> {
                service.getPdpGoal(GetPdpGoalQuery(userId, personId, goalId))
            }
        }
    }

    @Nested
    inner class ListPdpGoalsByPersonTests {

        @Test
        fun `should return paginated PDP goals for person`() {
            val goals = listOf(
                PdpGoal(
                    id = PdpGoalId.generate(),
                    userId = userId,
                    personId = personId,
                    title = "Goal 1"
                )
            )
            val pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"))
            every { personRepository.findByIdAndUserId(personId, userId) } returns person
            every { pdpGoalRepository.findAllByUserIdAndPersonId(userId, personId, null, pageable) } returns
                PageImpl(goals, pageable, 1)

            val result = service.listPdpGoalsByPerson(
                ListPdpGoalsByPersonQuery(userId, personId)
            )

            result.totalElements shouldBe 1
            result.content.size shouldBe 1
        }

        @Test
        fun `should filter by status`() {
            val pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"))
            every { personRepository.findByIdAndUserId(personId, userId) } returns person
            every { pdpGoalRepository.findAllByUserIdAndPersonId(userId, personId, PdpGoalStatus.ACTIVE, pageable) } returns
                PageImpl(emptyList(), pageable, 0)

            val result = service.listPdpGoalsByPerson(
                ListPdpGoalsByPersonQuery(userId, personId, status = PdpGoalStatus.ACTIVE)
            )

            result.totalElements shouldBe 0
            verify { pdpGoalRepository.findAllByUserIdAndPersonId(userId, personId, PdpGoalStatus.ACTIVE, pageable) }
        }

        @Test
        fun `should throw PersonNotFoundException when person not found`() {
            every { personRepository.findByIdAndUserId(personId, userId) } returns null

            shouldThrow<PersonNotFoundException> {
                service.listPdpGoalsByPerson(ListPdpGoalsByPersonQuery(userId, personId))
            }
        }
    }

    @Nested
    inner class PdpUpdateTests {

        private val goalId = PdpGoalId.generate()
        private val goal = PdpGoal(
            id = goalId,
            userId = userId,
            personId = personId,
            title = "Test goal"
        )

        @Test
        fun `should add progress update to goal`() {
            every { pdpGoalRepository.findByIdAndUserIdAndPersonId(goalId, userId, personId) } returns goal
            every { pdpUpdateRepository.save(any()) } answers { firstArg() }

            val command = AddPdpUpdateCommand(
                userId = userId,
                personId = personId,
                goalId = goalId,
                textMarkdown = "Completed first milestone",
                sensitive = false
            )

            val result = service.addPdpUpdate(command)

            result.textMarkdown shouldBe "Completed first milestone"
            result.sensitive shouldBe false
            result.goalId shouldBe goalId
        }

        @Test
        fun `should add sensitive progress update`() {
            every { pdpGoalRepository.findByIdAndUserIdAndPersonId(goalId, userId, personId) } returns goal
            every { pdpUpdateRepository.save(any()) } answers { firstArg() }

            val command = AddPdpUpdateCommand(
                userId = userId,
                personId = personId,
                goalId = goalId,
                textMarkdown = "Private note about progress",
                sensitive = true
            )

            val result = service.addPdpUpdate(command)

            result.sensitive shouldBe true
        }

        @Test
        fun `should throw PdpGoalNotFoundException when goal not found for update`() {
            every { pdpGoalRepository.findByIdAndUserIdAndPersonId(goalId, userId, personId) } returns null

            shouldThrow<PdpGoalNotFoundException> {
                service.addPdpUpdate(
                    AddPdpUpdateCommand(userId, personId, goalId, "text")
                )
            }
        }

        @Test
        fun `should delete progress update`() {
            val updateId = PdpUpdateId.generate()
            every { pdpGoalRepository.findByIdAndUserIdAndPersonId(goalId, userId, personId) } returns goal
            every { pdpUpdateRepository.deleteByIdAndGoalIdAndUserId(updateId, goalId, userId) } returns true

            service.deletePdpUpdate(DeletePdpUpdateCommand(userId, personId, goalId, updateId))

            verify { pdpUpdateRepository.deleteByIdAndGoalIdAndUserId(updateId, goalId, userId) }
        }

        @Test
        fun `should throw PdpUpdateNotFoundException when update not found`() {
            val updateId = PdpUpdateId.generate()
            every { pdpGoalRepository.findByIdAndUserIdAndPersonId(goalId, userId, personId) } returns goal
            every { pdpUpdateRepository.deleteByIdAndGoalIdAndUserId(updateId, goalId, userId) } returns false

            shouldThrow<PdpUpdateNotFoundException> {
                service.deletePdpUpdate(DeletePdpUpdateCommand(userId, personId, goalId, updateId))
            }
        }

        @Test
        fun `should list progress updates for goal`() {
            val updates = listOf(
                PdpUpdate(
                    id = PdpUpdateId.generate(),
                    goalId = goalId,
                    userId = userId,
                    textMarkdown = "Update 1"
                )
            )
            val pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"))
            every { pdpGoalRepository.findByIdAndUserIdAndPersonId(goalId, userId, personId) } returns goal
            every { pdpUpdateRepository.findAllByGoalIdAndUserId(goalId, userId, pageable) } returns
                PageImpl(updates, pageable, 1)

            val result = service.listPdpUpdatesByGoal(
                ListPdpUpdatesByGoalQuery(userId, personId, goalId)
            )

            result.totalElements shouldBe 1
            result.content[0].textMarkdown shouldBe "Update 1"
        }
    }

    @Nested
    inner class CountActivePdpGoalsTests {

        @Test
        fun `should return count of active PDP goals`() {
            every { pdpGoalRepository.countActiveByUserIdAndPersonId(userId, personId) } returns 3

            val result = service.countActivePdpGoals(CountActivePdpGoalsQuery(userId, personId))

            result shouldBe 3
        }
    }
}
