package com.peoplemanager.adapters.web

import tools.jackson.databind.ObjectMapper
import com.peoplemanager.adapters.auth.SecurityConfig
import com.peoplemanager.adapters.auth.UserProvisioningJwtAuthenticationConverter
import com.peoplemanager.application.UserProvisioningService
import com.peoplemanager.application.WorkspaceNotFoundException
import com.peoplemanager.application.PersonNotFoundException
import com.peoplemanager.application.port.input.WorkspaceCommandPort
import com.peoplemanager.application.port.input.WorkspaceQueryPort
import com.peoplemanager.domain.MoraleStatus
import com.peoplemanager.domain.Person
import com.peoplemanager.domain.PersonId
import com.peoplemanager.domain.UserId
import com.peoplemanager.domain.Workspace
import com.peoplemanager.domain.WorkspaceId
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant
import java.util.UUID

@WebMvcTest(controllers = [WorkspaceController::class])
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
class WorkspaceControllerTest {

    @TestConfiguration
    class TestConfig {
        @Bean
        fun workspaceCommandPort(): WorkspaceCommandPort = mockk()

        @Bean
        fun workspaceQueryPort(): WorkspaceQueryPort = mockk()

        @Bean
        fun userProvisioningService(): UserProvisioningService = mockk(relaxed = true)
    }

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var workspaceCommandPort: WorkspaceCommandPort

    @Autowired
    private lateinit var workspaceQueryPort: WorkspaceQueryPort

    private val userId = UUID.randomUUID()
    private val workspaceId = UUID.randomUUID()

    private fun jwtAuth(): JwtAuthenticationToken {
        val jwt = Jwt.withTokenValue("token")
            .header("alg", "RS256")
            .subject("test-subject")
            .issuer("https://auth.example.com")
            .claim("name", "Test User")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .build()
        val authorities = listOf(SimpleGrantedAuthority("SCOPE_openid"))
        val token = JwtAuthenticationToken(jwt, authorities, "test-subject")
        token.details = UserId(userId)
        return token
    }

    private fun createTestWorkspace(
        id: UUID = workspaceId,
        name: String = "My Team",
        description: String? = "Direct reports"
    ) = Workspace(
        id = WorkspaceId(id),
        userId = UserId(userId),
        name = name,
        description = description,
        displayOrder = 0,
        createdAt = Instant.parse("2026-05-11T10:00:00Z"),
        updatedAt = Instant.parse("2026-05-11T10:00:00Z")
    )

    // --- Create Workspace ---

    @Test
    fun `POST workspaces should create workspace and return 201`() {
        val workspace = createTestWorkspace()
        every { workspaceCommandPort.createWorkspace(any()) } returns workspace

        mockMvc.perform(
            post("/api/v1/workspaces")
                .with(authentication(jwtAuth()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name": "My Team", "description": "Direct reports"}""")
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value(workspaceId.toString()))
            .andExpect(jsonPath("$.name").value("My Team"))
            .andExpect(jsonPath("$.description").value("Direct reports"))
    }

    @Test
    fun `POST workspaces should return 400 when name is blank`() {
        mockMvc.perform(
            post("/api/v1/workspaces")
                .with(authentication(jwtAuth()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name": ""}""")
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `POST workspaces should return 401 when unauthenticated`() {
        mockMvc.perform(
            post("/api/v1/workspaces")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name": "My Team"}""")
        )
            .andExpect(status().isUnauthorized)
    }

    // --- List Workspaces ---

    @Test
    fun `GET workspaces should return list of workspaces`() {
        val workspaces = listOf(
            createTestWorkspace(id = UUID.randomUUID(), name = "Team A"),
            createTestWorkspace(id = UUID.randomUUID(), name = "Mentees")
        )
        every { workspaceQueryPort.listWorkspaces(any()) } returns workspaces

        mockMvc.perform(
            get("/api/v1/workspaces")
                .with(authentication(jwtAuth()))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].name").value("Team A"))
            .andExpect(jsonPath("$[1].name").value("Mentees"))
    }

    @Test
    fun `GET workspaces should return empty list when no workspaces exist`() {
        every { workspaceQueryPort.listWorkspaces(any()) } returns emptyList()

        mockMvc.perform(
            get("/api/v1/workspaces")
                .with(authentication(jwtAuth()))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(0))
    }

    // --- Get Workspace ---

    @Test
    fun `GET workspaces by id should return workspace`() {
        val workspace = createTestWorkspace()
        every { workspaceQueryPort.getWorkspace(any()) } returns workspace

        mockMvc.perform(
            get("/api/v1/workspaces/$workspaceId")
                .with(authentication(jwtAuth()))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(workspaceId.toString()))
            .andExpect(jsonPath("$.name").value("My Team"))
    }

    @Test
    fun `GET workspaces by id should return 404 when not found`() {
        every { workspaceQueryPort.getWorkspace(any()) } throws WorkspaceNotFoundException(WorkspaceId(workspaceId))

        mockMvc.perform(
            get("/api/v1/workspaces/$workspaceId")
                .with(authentication(jwtAuth()))
        )
            .andExpect(status().isNotFound)
    }

    // --- Update Workspace ---

    @Test
    fun `PUT workspaces should update workspace`() {
        val updated = createTestWorkspace(name = "Updated Name")
        every { workspaceCommandPort.updateWorkspace(any()) } returns updated

        mockMvc.perform(
            put("/api/v1/workspaces/$workspaceId")
                .with(authentication(jwtAuth()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name": "Updated Name"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("Updated Name"))
    }

    @Test
    fun `PUT workspaces should return 404 when workspace not found`() {
        every { workspaceCommandPort.updateWorkspace(any()) } throws WorkspaceNotFoundException(WorkspaceId(workspaceId))

        mockMvc.perform(
            put("/api/v1/workspaces/$workspaceId")
                .with(authentication(jwtAuth()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name": "New Name"}""")
        )
            .andExpect(status().isNotFound)
    }

    // --- Delete Workspace ---

    @Test
    fun `DELETE workspaces should return 204`() {
        every { workspaceCommandPort.deleteWorkspace(any()) } returns Unit

        mockMvc.perform(
            delete("/api/v1/workspaces/$workspaceId")
                .with(authentication(jwtAuth()))
        )
            .andExpect(status().isNoContent)
    }

    @Test
    fun `DELETE workspaces should return 404 when not found`() {
        every { workspaceCommandPort.deleteWorkspace(any()) } throws WorkspaceNotFoundException(WorkspaceId(workspaceId))

        mockMvc.perform(
            delete("/api/v1/workspaces/$workspaceId")
                .with(authentication(jwtAuth()))
        )
            .andExpect(status().isNotFound)
    }

    // --- Assign Person to Workspace ---

    @Test
    fun `PUT persons workspace should assign person to workspace`() {
        val personId = UUID.randomUUID()
        val person = Person(
            id = PersonId(personId),
            userId = UserId(userId),
            name = "Alice",
            moraleStatus = MoraleStatus.UNKNOWN,
            workspaceId = WorkspaceId(workspaceId)
        )
        every { workspaceCommandPort.assignPersonToWorkspace(any()) } returns person

        mockMvc.perform(
            put("/api/v1/workspaces/persons/$personId/workspace")
                .with(authentication(jwtAuth()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"workspaceId": "$workspaceId"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.workspaceId").value(workspaceId.toString()))
    }

    @Test
    fun `PUT persons workspace should unassign person when workspaceId is null`() {
        val personId = UUID.randomUUID()
        val person = Person(
            id = PersonId(personId),
            userId = UserId(userId),
            name = "Alice",
            moraleStatus = MoraleStatus.UNKNOWN,
            workspaceId = null
        )
        every { workspaceCommandPort.assignPersonToWorkspace(any()) } returns person

        mockMvc.perform(
            put("/api/v1/workspaces/persons/$personId/workspace")
                .with(authentication(jwtAuth()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"workspaceId": null}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.workspaceId").isEmpty)
    }

    @Test
    fun `PUT persons workspace should return 404 when person not found`() {
        val personId = UUID.randomUUID()
        every { workspaceCommandPort.assignPersonToWorkspace(any()) } throws PersonNotFoundException(PersonId(personId))

        mockMvc.perform(
            put("/api/v1/workspaces/persons/$personId/workspace")
                .with(authentication(jwtAuth()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"workspaceId": "$workspaceId"}""")
        )
            .andExpect(status().isNotFound)
    }
}
