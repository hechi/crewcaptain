package com.peoplemanager.adapters.web

import com.fasterxml.jackson.databind.ObjectMapper
import com.peoplemanager.adapters.auth.SecurityConfig
import com.peoplemanager.adapters.auth.UserProvisioningJwtAuthenticationConverter
import com.peoplemanager.adapters.web.dto.CreateActionItemRequest
import com.peoplemanager.adapters.web.dto.UpdateActionItemRequest
import com.peoplemanager.application.ActionItemNotFoundException
import com.peoplemanager.application.PersonNotFoundException
import com.peoplemanager.application.UserProvisioningService
import com.peoplemanager.application.port.input.ActionItemCommandPort
import com.peoplemanager.application.port.input.ActionItemQueryPort
import com.peoplemanager.domain.*
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.MediaType
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@WebMvcTest(controllers = [ActionItemController::class])
@Import(SecurityConfig::class, UserProvisioningJwtAuthenticationConverter::class, GlobalExceptionHandler::class)
@TestPropertySource(properties = [
    "spring.datasource.url=jdbc:h2:mem:test",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.flyway.enabled=false",
    "spring.jpa.hibernate.ddl-auto=none",
    "spring.security.oauth2.resourceserver.jwt.issuer-uri=https://auth.example.com",
    "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://auth.example.com/jwks"
])
class ActionItemControllerTest {

    @TestConfiguration
    class TestConfig {
        @Bean
        fun actionItemCommandPort(): ActionItemCommandPort = mockk()

        @Bean
        fun actionItemQueryPort(): ActionItemQueryPort = mockk()

        @Bean
        fun userProvisioningService(): UserProvisioningService = mockk()
    }

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var actionItemCommandPort: ActionItemCommandPort

    @Autowired
    private lateinit var actionItemQueryPort: ActionItemQueryPort

    private fun authenticatedJwt(userId: UserId = UserId(UUID.randomUUID())): JwtAuthenticationToken {
        val jwt = Jwt.withTokenValue("test-token")
            .header("alg", "RS256")
            .subject("test-subject")
            .issuer("https://auth.example.com")
            .claim("name", "Test User")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .build()
        val authorities = listOf(SimpleGrantedAuthority("ROLE_USER"))
        val token = JwtAuthenticationToken(jwt, authorities, "test-subject")
        token.details = userId
        return token
    }

    private val userId = UserId(UUID.randomUUID())
    private val personId = PersonId(UUID.randomUUID())
    private val actionItemId = ActionItemId(UUID.randomUUID())

    private fun sampleActionItem(
        id: ActionItemId = actionItemId,
        title: String = "Follow up on project",
        status: ActionItemStatus = ActionItemStatus.OPEN
    ) = ActionItem(
        id = id,
        userId = userId,
        personId = personId,
        title = title,
        description = "Check progress",
        ownerType = ActionItemOwnerType.MANAGER,
        dueDate = LocalDate.of(2026, 5, 20),
        status = status,
        originatingEntryId = null
    )

    // ===== Create Action Item =====

    @Test
    fun `POST action-items - creates action item and returns 201`() {
        val item = sampleActionItem()
        val request = CreateActionItemRequest(
            title = "Follow up on project",
            description = "Check progress",
            ownerType = ActionItemOwnerType.MANAGER,
            dueDate = LocalDate.of(2026, 5, 20)
        )

        every { actionItemCommandPort.createActionItem(any()) } returns item

        mockMvc.perform(
            post("/api/v1/persons/${personId.value}/action-items")
                .with(authentication(authenticatedJwt(userId)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value(item.id.value.toString()))
            .andExpect(jsonPath("$.title").value("Follow up on project"))
            .andExpect(jsonPath("$.description").value("Check progress"))
            .andExpect(jsonPath("$.ownerType").value("MANAGER"))
            .andExpect(jsonPath("$.status").value("OPEN"))
            .andExpect(jsonPath("$.dueDate").value("2026-05-20"))
    }

    @Test
    fun `POST action-items - returns 400 when title is blank`() {
        val request = CreateActionItemRequest(title = "")

        mockMvc.perform(
            post("/api/v1/persons/${personId.value}/action-items")
                .with(authentication(authenticatedJwt(userId)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `POST action-items - returns 404 when person not found`() {
        val request = CreateActionItemRequest(title = "Task")

        every { actionItemCommandPort.createActionItem(any()) } throws PersonNotFoundException(personId)

        mockMvc.perform(
            post("/api/v1/persons/${personId.value}/action-items")
                .with(authentication(authenticatedJwt(userId)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("Person not found"))
    }

    @Test
    fun `POST action-items - returns 401 without authentication`() {
        val request = CreateActionItemRequest(title = "Task")

        mockMvc.perform(
            post("/api/v1/persons/${personId.value}/action-items")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isUnauthorized)
    }

    // ===== Get Action Item =====

    @Test
    fun `GET action-items by id - returns action item`() {
        val item = sampleActionItem()

        every { actionItemQueryPort.getActionItem(any()) } returns item

        mockMvc.perform(
            get("/api/v1/persons/${personId.value}/action-items/${actionItemId.value}")
                .with(authentication(authenticatedJwt(userId)))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(item.id.value.toString()))
            .andExpect(jsonPath("$.title").value("Follow up on project"))
    }

    @Test
    fun `GET action-items by id - returns 404 when not found`() {
        every { actionItemQueryPort.getActionItem(any()) } throws ActionItemNotFoundException(actionItemId)

        mockMvc.perform(
            get("/api/v1/persons/${personId.value}/action-items/${actionItemId.value}")
                .with(authentication(authenticatedJwt(userId)))
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("Action item not found"))
    }

    // ===== List Action Items by Person =====

    @Test
    fun `GET action-items by person - returns paginated list`() {
        val items = listOf(sampleActionItem())
        val pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "dueDate"))

        every { actionItemQueryPort.listActionItemsByPerson(any()) } returns
            PageImpl(items, pageable, 1)

        mockMvc.perform(
            get("/api/v1/persons/${personId.value}/action-items")
                .with(authentication(authenticatedJwt(userId)))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].title").value("Follow up on project"))
    }

    @Test
    fun `GET action-items by person - supports status filter`() {
        val pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "dueDate"))

        every { actionItemQueryPort.listActionItemsByPerson(any()) } returns
            PageImpl(emptyList(), pageable, 0)

        mockMvc.perform(
            get("/api/v1/persons/${personId.value}/action-items?status=OPEN")
                .with(authentication(authenticatedJwt(userId)))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content.length()").value(0))
    }

    @Test
    fun `GET action-items by person - supports originatingEntryId filter`() {
        val entryId = OneOnOneEntryId(UUID.randomUUID())
        val item = sampleActionItem().copy(originatingEntryId = entryId)
        val pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "dueDate"))

        every { actionItemQueryPort.listActionItemsByPerson(match {
            it.originatingEntryId == entryId
        }) } returns PageImpl(listOf(item), pageable, 1)

        mockMvc.perform(
            get("/api/v1/persons/${personId.value}/action-items?originatingEntryId=${entryId.value}")
                .with(authentication(authenticatedJwt(userId)))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.content[0].originatingEntryId").value(entryId.value.toString()))
    }

    // ===== Update Action Item =====

    @Test
    fun `PUT action-items - updates and returns action item`() {
        val updated = sampleActionItem(title = "Updated title")
        val request = UpdateActionItemRequest(title = "Updated title")

        every { actionItemCommandPort.updateActionItem(any()) } returns updated

        mockMvc.perform(
            put("/api/v1/persons/${personId.value}/action-items/${actionItemId.value}")
                .with(authentication(authenticatedJwt(userId)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.title").value("Updated title"))
    }

    @Test
    fun `PUT action-items - returns 404 when not found`() {
        val request = UpdateActionItemRequest(title = "Updated")

        every { actionItemCommandPort.updateActionItem(any()) } throws ActionItemNotFoundException(actionItemId)

        mockMvc.perform(
            put("/api/v1/persons/${personId.value}/action-items/${actionItemId.value}")
                .with(authentication(authenticatedJwt(userId)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isNotFound)
    }

    // ===== Complete Action Item =====

    @Test
    fun `POST complete - completes action item`() {
        val completed = sampleActionItem(status = ActionItemStatus.DONE)

        every { actionItemCommandPort.completeActionItem(any()) } returns completed

        mockMvc.perform(
            post("/api/v1/persons/${personId.value}/action-items/${actionItemId.value}/complete")
                .with(authentication(authenticatedJwt(userId)))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("DONE"))
    }

    @Test
    fun `POST complete - returns 400 when already done`() {
        every { actionItemCommandPort.completeActionItem(any()) } throws
            IllegalArgumentException("Can only complete an action item with status OPEN, current status is DONE")

        mockMvc.perform(
            post("/api/v1/persons/${personId.value}/action-items/${actionItemId.value}/complete")
                .with(authentication(authenticatedJwt(userId)))
        )
            .andExpect(status().isBadRequest)
    }

    // ===== Cancel Action Item =====

    @Test
    fun `POST cancel - cancels action item`() {
        val canceled = sampleActionItem(status = ActionItemStatus.CANCELED)

        every { actionItemCommandPort.cancelActionItem(any()) } returns canceled

        mockMvc.perform(
            post("/api/v1/persons/${personId.value}/action-items/${actionItemId.value}/cancel")
                .with(authentication(authenticatedJwt(userId)))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("CANCELED"))
    }

    // ===== Delete Action Item =====

    @Test
    fun `DELETE action-items - deletes and returns 204`() {
        every { actionItemCommandPort.deleteActionItem(any()) } returns Unit

        mockMvc.perform(
            delete("/api/v1/persons/${personId.value}/action-items/${actionItemId.value}")
                .with(authentication(authenticatedJwt(userId)))
        )
            .andExpect(status().isNoContent)
    }

    @Test
    fun `DELETE action-items - returns 404 when not found`() {
        every { actionItemCommandPort.deleteActionItem(any()) } throws ActionItemNotFoundException(actionItemId)

        mockMvc.perform(
            delete("/api/v1/persons/${personId.value}/action-items/${actionItemId.value}")
                .with(authentication(authenticatedJwt(userId)))
        )
            .andExpect(status().isNotFound)
    }

    // ===== List All Action Items (Cross-Person) =====

    @Test
    fun `GET all action-items - returns paginated list`() {
        val items = listOf(sampleActionItem())
        val pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "dueDate"))

        every { actionItemQueryPort.listAllActionItems(any()) } returns
            PageImpl(items, pageable, 1)

        mockMvc.perform(
            get("/api/v1/action-items")
                .with(authentication(authenticatedJwt(userId)))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.totalElements").value(1))
    }

    @Test
    fun `GET all action-items - supports overdueOnly filter`() {
        val pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "dueDate"))

        every { actionItemQueryPort.listAllActionItems(any()) } returns
            PageImpl(emptyList(), pageable, 0)

        mockMvc.perform(
            get("/api/v1/action-items?overdueOnly=true")
                .with(authentication(authenticatedJwt(userId)))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content.length()").value(0))
    }

    @Test
    fun `GET all action-items - returns 401 without authentication`() {
        mockMvc.perform(
            get("/api/v1/action-items")
        )
            .andExpect(status().isUnauthorized)
    }
}
