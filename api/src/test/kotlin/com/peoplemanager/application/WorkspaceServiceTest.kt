package com.peoplemanager.application

import com.peoplemanager.application.commands.AssignPersonToWorkspaceCommand
import com.peoplemanager.application.commands.CreateWorkspaceCommand
import com.peoplemanager.application.commands.DeleteWorkspaceCommand
import com.peoplemanager.application.commands.UpdateWorkspaceCommand
import com.peoplemanager.application.ports.PersonRepository
import com.peoplemanager.application.ports.WorkspaceRepository
import com.peoplemanager.application.queries.GetWorkspaceQuery
import com.peoplemanager.application.queries.ListWorkspacesQuery
import com.peoplemanager.domain.MoraleStatus
import com.peoplemanager.domain.Person
import com.peoplemanager.domain.PersonId
import com.peoplemanager.domain.UserId
import com.peoplemanager.domain.Workspace
import com.peoplemanager.domain.WorkspaceId
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Instant

class WorkspaceServiceTest {

    private lateinit var workspaceRepository: WorkspaceRepository
    private lateinit var personRepository: PersonRepository
    private lateinit var auditLogService: AuditLogService
    private lateinit var workspaceService: WorkspaceService

    private val userId = UserId.generate()
    private val workspaceId = WorkspaceId.generate()

    @BeforeEach
    fun setUp() {
        workspaceRepository = mockk()
        personRepository = mockk()
        auditLogService = mockk(relaxed = true)
        workspaceService = WorkspaceService(workspaceRepository, personRepository, auditLogService)
    }

    private fun createTestWorkspace(
        id: WorkspaceId = workspaceId,
        uid: UserId = userId,
        name: String = "My Team",
        description: String? = "Direct reports",
        displayOrder: Int = 0
    ) = Workspace(
        id = id,
        userId = uid,
        name = name,
        description = description,
        displayOrder = displayOrder,
        createdAt = Instant.now(),
        updatedAt = Instant.now()
    )

    private fun createTestPerson(
        id: PersonId = PersonId.generate(),
        uid: UserId = userId,
        wsId: WorkspaceId? = null
    ) = Person(
        id = id,
        userId = uid,
        name = "Alice Smith",
        preferredName = "Ali",
        roleTitle = "Engineer",
        timezone = "UTC",
        moraleStatus = MoraleStatus.UNKNOWN,
        workspaceId = wsId
    )

    @Nested
    inner class CreateWorkspace {

        @Test
        fun `should create workspace with correct fields`() {
            val command = CreateWorkspaceCommand(
                userId = userId,
                name = "Mentees",
                description = "People I mentor"
            )

            every { workspaceRepository.countByUserId(userId) } returns 2
            val workspaceSlot = slot<Workspace>()
            every { workspaceRepository.save(capture(workspaceSlot)) } answers { workspaceSlot.captured }

            val result = workspaceService.createWorkspace(command)

            result.userId shouldBe userId
            result.name shouldBe "Mentees"
            result.description shouldBe "People I mentor"
            result.displayOrder shouldBe 2
        }

        @Test
        fun `should record audit log on create`() {
            val command = CreateWorkspaceCommand(userId = userId, name = "Team A")

            every { workspaceRepository.countByUserId(userId) } returns 0
            val workspaceSlot = slot<Workspace>()
            every { workspaceRepository.save(capture(workspaceSlot)) } answers { workspaceSlot.captured }

            workspaceService.createWorkspace(command)

            verify { auditLogService.record(any()) }
        }
    }

    @Nested
    inner class UpdateWorkspace {

        @Test
        fun `should update workspace name`() {
            val existing = createTestWorkspace()
            val command = UpdateWorkspaceCommand(
                userId = userId,
                workspaceId = workspaceId,
                name = "Updated Name"
            )

            every { workspaceRepository.findByIdAndUserId(workspaceId, userId) } returns existing
            val workspaceSlot = slot<Workspace>()
            every { workspaceRepository.save(capture(workspaceSlot)) } answers { workspaceSlot.captured }

            val result = workspaceService.updateWorkspace(command)

            result.name shouldBe "Updated Name"
            result.description shouldBe existing.description
        }

        @Test
        fun `should throw WorkspaceNotFoundException when workspace does not exist`() {
            val command = UpdateWorkspaceCommand(
                userId = userId,
                workspaceId = workspaceId,
                name = "New Name"
            )

            every { workspaceRepository.findByIdAndUserId(workspaceId, userId) } returns null

            shouldThrow<WorkspaceNotFoundException> {
                workspaceService.updateWorkspace(command)
            }
        }

        @Test
        fun `should record audit log on update`() {
            val existing = createTestWorkspace()
            val command = UpdateWorkspaceCommand(userId = userId, workspaceId = workspaceId, name = "New")

            every { workspaceRepository.findByIdAndUserId(workspaceId, userId) } returns existing
            val workspaceSlot = slot<Workspace>()
            every { workspaceRepository.save(capture(workspaceSlot)) } answers { workspaceSlot.captured }

            workspaceService.updateWorkspace(command)

            verify { auditLogService.record(any()) }
        }
    }

    @Nested
    inner class DeleteWorkspace {

        @Test
        fun `should delete workspace`() {
            val existing = createTestWorkspace()
            val command = DeleteWorkspaceCommand(userId = userId, workspaceId = workspaceId)

            every { workspaceRepository.findByIdAndUserId(workspaceId, userId) } returns existing
            every { workspaceRepository.deleteByIdAndUserId(workspaceId, userId) } returns true

            workspaceService.deleteWorkspace(command)

            verify { workspaceRepository.deleteByIdAndUserId(workspaceId, userId) }
        }

        @Test
        fun `should throw WorkspaceNotFoundException when workspace does not exist`() {
            val command = DeleteWorkspaceCommand(userId = userId, workspaceId = workspaceId)

            every { workspaceRepository.findByIdAndUserId(workspaceId, userId) } returns null

            shouldThrow<WorkspaceNotFoundException> {
                workspaceService.deleteWorkspace(command)
            }
        }

        @Test
        fun `should record audit log on delete`() {
            val existing = createTestWorkspace()
            val command = DeleteWorkspaceCommand(userId = userId, workspaceId = workspaceId)

            every { workspaceRepository.findByIdAndUserId(workspaceId, userId) } returns existing
            every { workspaceRepository.deleteByIdAndUserId(workspaceId, userId) } returns true

            workspaceService.deleteWorkspace(command)

            verify { auditLogService.record(any()) }
        }
    }

    @Nested
    inner class AssignPersonToWorkspace {

        @Test
        fun `should assign person to workspace`() {
            val personId = PersonId.generate()
            val person = createTestPerson(id = personId)
            val workspace = createTestWorkspace()
            val command = AssignPersonToWorkspaceCommand(
                userId = userId,
                personId = personId,
                workspaceId = workspaceId
            )

            every { personRepository.findByIdAndUserId(personId, userId) } returns person
            every { workspaceRepository.findByIdAndUserId(workspaceId, userId) } returns workspace
            val personSlot = slot<Person>()
            every { personRepository.save(capture(personSlot)) } answers { personSlot.captured }

            val result = workspaceService.assignPersonToWorkspace(command)

            result.workspaceId shouldBe workspaceId
        }

        @Test
        fun `should unassign person from workspace when workspaceId is null`() {
            val personId = PersonId.generate()
            val person = createTestPerson(id = personId, wsId = workspaceId)
            val command = AssignPersonToWorkspaceCommand(
                userId = userId,
                personId = personId,
                workspaceId = null
            )

            every { personRepository.findByIdAndUserId(personId, userId) } returns person
            val personSlot = slot<Person>()
            every { personRepository.save(capture(personSlot)) } answers { personSlot.captured }

            val result = workspaceService.assignPersonToWorkspace(command)

            result.workspaceId shouldBe null
        }

        @Test
        fun `should throw PersonNotFoundException when person does not exist`() {
            val personId = PersonId.generate()
            val command = AssignPersonToWorkspaceCommand(
                userId = userId,
                personId = personId,
                workspaceId = workspaceId
            )

            every { personRepository.findByIdAndUserId(personId, userId) } returns null

            shouldThrow<PersonNotFoundException> {
                workspaceService.assignPersonToWorkspace(command)
            }
        }

        @Test
        fun `should throw WorkspaceNotFoundException when workspace does not exist`() {
            val personId = PersonId.generate()
            val person = createTestPerson(id = personId)
            val command = AssignPersonToWorkspaceCommand(
                userId = userId,
                personId = personId,
                workspaceId = workspaceId
            )

            every { personRepository.findByIdAndUserId(personId, userId) } returns person
            every { workspaceRepository.findByIdAndUserId(workspaceId, userId) } returns null

            shouldThrow<WorkspaceNotFoundException> {
                workspaceService.assignPersonToWorkspace(command)
            }
        }
    }

    @Nested
    inner class GetWorkspace {

        @Test
        fun `should return workspace when found`() {
            val workspace = createTestWorkspace()
            val query = GetWorkspaceQuery(userId = userId, workspaceId = workspaceId)

            every { workspaceRepository.findByIdAndUserId(workspaceId, userId) } returns workspace

            val result = workspaceService.getWorkspace(query)

            result shouldBe workspace
        }

        @Test
        fun `should throw WorkspaceNotFoundException when not found`() {
            val query = GetWorkspaceQuery(userId = userId, workspaceId = workspaceId)

            every { workspaceRepository.findByIdAndUserId(workspaceId, userId) } returns null

            shouldThrow<WorkspaceNotFoundException> {
                workspaceService.getWorkspace(query)
            }
        }
    }

    @Nested
    inner class ListWorkspaces {

        @Test
        fun `should return all workspaces for user`() {
            val workspaces = listOf(
                createTestWorkspace(id = WorkspaceId.generate(), name = "Team A", displayOrder = 0),
                createTestWorkspace(id = WorkspaceId.generate(), name = "Mentees", displayOrder = 1)
            )
            val query = ListWorkspacesQuery(userId = userId)

            every { workspaceRepository.findAllByUserId(userId) } returns workspaces

            val result = workspaceService.listWorkspaces(query)

            result shouldBe workspaces
        }

        @Test
        fun `should return empty list when no workspaces exist`() {
            val query = ListWorkspacesQuery(userId = userId)

            every { workspaceRepository.findAllByUserId(userId) } returns emptyList()

            val result = workspaceService.listWorkspaces(query)

            result shouldBe emptyList()
        }
    }
}
