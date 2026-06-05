package com.peoplemanager.application

import com.peoplemanager.application.port.output.AuditLogRepository
import com.peoplemanager.application.queries.GetAuditLogQuery
import com.peoplemanager.domain.*
import io.kotest.matchers.shouldBe
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest

class AuditLogServiceTest {

    private val auditLogRepository = mockk<AuditLogRepository>()
    private val service = AuditLogService(auditLogRepository)

    private val userId = UserId.generate()
    private val personId = PersonId.generate()

    @BeforeEach
    fun setup() {
        clearAllMocks()
    }

    @Nested
    inner class RecordTests {

        @Test
        fun `should save audit log entry`() {
            val entry = AuditLogEntry.personCreated(userId, personId, "John Doe")
            every { auditLogRepository.save(entry) } returns entry

            val result = service.record(entry)

            result shouldBe entry
            verify(exactly = 1) { auditLogRepository.save(entry) }
        }

        @Test
        fun `should save entry with all fields`() {
            val entry = AuditLogEntry(
                userId = userId,
                action = AuditAction.DELETE,
                entityType = AuditEntityType.ACTION_ITEM,
                entityId = "some-id",
                personId = personId,
                summary = "Deleted action item"
            )
            every { auditLogRepository.save(entry) } returns entry

            val result = service.record(entry)

            result.action shouldBe AuditAction.DELETE
            result.entityType shouldBe AuditEntityType.ACTION_ITEM
        }
    }

    @Nested
    inner class GetAuditLogTests {

        @Test
        fun `should return paginated audit log for user`() {
            val entry = AuditLogEntry.personCreated(userId, personId, "John")
            val pageable = PageRequest.of(0, 20)
            every { auditLogRepository.findAllByUserId(userId, null, null, pageable) } returns PageImpl(listOf(entry))

            val query = GetAuditLogQuery(userId = userId, pageable = pageable)
            val result = service.getAuditLog(query)

            result.content.size shouldBe 1
            result.content[0] shouldBe entry
        }

        @Test
        fun `should filter by entity type`() {
            val entry = AuditLogEntry.personCreated(userId, personId, "John")
            val pageable = PageRequest.of(0, 20)
            every { auditLogRepository.findAllByUserId(userId, AuditEntityType.PERSON, null, pageable) } returns PageImpl(listOf(entry))

            val query = GetAuditLogQuery(userId = userId, entityType = AuditEntityType.PERSON, pageable = pageable)
            val result = service.getAuditLog(query)

            result.content.size shouldBe 1
            verify { auditLogRepository.findAllByUserId(userId, AuditEntityType.PERSON, null, pageable) }
        }

        @Test
        fun `should filter by action`() {
            val pageable = PageRequest.of(0, 20)
            every { auditLogRepository.findAllByUserId(userId, null, AuditAction.CREATE, pageable) } returns PageImpl(emptyList())

            val query = GetAuditLogQuery(userId = userId, action = AuditAction.CREATE, pageable = pageable)
            val result = service.getAuditLog(query)

            result.content.size shouldBe 0
            verify { auditLogRepository.findAllByUserId(userId, null, AuditAction.CREATE, pageable) }
        }

        @Test
        fun `should filter by both entity type and action`() {
            val pageable = PageRequest.of(0, 20)
            every { auditLogRepository.findAllByUserId(userId, AuditEntityType.ACTION_ITEM, AuditAction.DELETE, pageable) } returns PageImpl(emptyList())

            val query = GetAuditLogQuery(userId = userId, entityType = AuditEntityType.ACTION_ITEM, action = AuditAction.DELETE, pageable = pageable)
            val result = service.getAuditLog(query)

            result.content.size shouldBe 0
            verify { auditLogRepository.findAllByUserId(userId, AuditEntityType.ACTION_ITEM, AuditAction.DELETE, pageable) }
        }

        @Test
        fun `should return empty page when no entries exist`() {
            val pageable = PageRequest.of(0, 20)
            every { auditLogRepository.findAllByUserId(userId, null, null, pageable) } returns PageImpl(emptyList())

            val query = GetAuditLogQuery(userId = userId, pageable = pageable)
            val result = service.getAuditLog(query)

            result.content.size shouldBe 0
            result.totalElements shouldBe 0
        }

        @Test
        fun `should respect pagination parameters`() {
            val pageable = PageRequest.of(2, 10)
            every { auditLogRepository.findAllByUserId(userId, null, null, pageable) } returns PageImpl(emptyList(), pageable, 25)

            val query = GetAuditLogQuery(userId = userId, pageable = pageable)
            val result = service.getAuditLog(query)

            result.number shouldBe 2
            result.size shouldBe 10
            result.totalElements shouldBe 25
        }
    }
}
