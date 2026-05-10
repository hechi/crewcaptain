package com.peoplemanager.adapters.web

import com.fasterxml.jackson.databind.ObjectMapper
import com.peoplemanager.adapters.auth.SecurityConfig
import com.peoplemanager.adapters.auth.UserProvisioningJwtAuthenticationConverter
import com.peoplemanager.adapters.web.dto.AssignQuickNoteToPersonRequest
import com.peoplemanager.adapters.web.dto.CreateQuickNoteRequest
import com.peoplemanager.adapters.web.dto.UpdateQuickNoteRequest
import com.peoplemanager.application.PersonNotFoundException
import com.peoplemanager.application.QuickNoteNotFoundException
import com.peoplemanager.application.UserProvisioningService
import com.peoplemanager.application.ports.QuickNoteCommandPort
import com.peoplemanager.application.ports.QuickNoteQueryPort
import com.peoplemanager.domain.*
import io.mockk.every
import io.mockk.mockk
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
import java.util.UUID

@WebMvcTest(controllers = [QuickNoteController::class])
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
class QuickNoteControllerTest {

    @TestConfiguration
    class TestConfig {
        @Bean
        fun quickNoteCommandPort(): QuickNoteCommandPort = mockk()

        @Bean
        fun quickNoteQueryPort(): QuickNoteQueryPort = mockk()

        @Bean
        fun userProvisioningService(): UserProvisioningService = mockk()
    }

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var quickNoteCommandPort: QuickNoteCommandPort

    @Autowired
    private lateinit var quickNoteQueryPort: QuickNoteQueryPort

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
    private val quickNoteId = QuickNoteId(UUID.randomUUID())

    private fun sampleQuickNote(
        id: QuickNoteId = quickNoteId,
        text: String = "Remember to follow up on project",
        personId: PersonId? = null,
        sensitive: Boolean = false,
        status: QuickNoteStatus = QuickNoteStatus.INBOX
    ) = QuickNote(
        id = id,
        userId = userId,
        personId = personId,
        text = text,
        sensitive = sensitive,
        status = status
    )

    // ===== Create Quick Note =====

    @Test
    fun `POST quick-notes - creates quick note and returns 201`() {
        val quickNote = sampleQuickNote()
        val request = CreateQuickNoteRequest(
            text = "Remember to follow up on project"
        )

        every { quickNoteCommandPort.createQuickNote(any()) } returns quickNote

        mockMvc.perform(
            post("/api/v1/quick-notes")
                .with(authentication(authenticatedJwt(userId)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value(quickNote.id.value.toString()))
            .andExpect(jsonPath("$.text").value("Remember to follow up on project"))
            .andExpect(jsonPath("$.status").value("INBOX"))
            .andExpect(jsonPath("$.sensitive").value(false))
            .andExpect(jsonPath("$.personId").isEmpty)
    }

    @Test
    fun `POST quick-notes - creates with person and sensitive flag`() {
        val quickNote = sampleQuickNote(personId = personId, sensitive = true)
        val request = CreateQuickNoteRequest(
            text = "Sensitive note about health",
            personId = personId.value,
            sensitive = true
        )

        every { quickNoteCommandPort.createQuickNote(any()) } returns quickNote

        mockMvc.perform(
            post("/api/v1/quick-notes")
                .with(authentication(authenticatedJwt(userId)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.personId").value(personId.value.toString()))
            .andExpect(jsonPath("$.sensitive").value(true))
    }

    @Test
    fun `POST quick-notes - returns 400 when text is blank`() {
        val request = CreateQuickNoteRequest(text = "")

        mockMvc.perform(
            post("/api/v1/quick-notes")
                .with(authentication(authenticatedJwt(userId)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `POST quick-notes - returns 404 when person not found`() {
        val request = CreateQuickNoteRequest(
            text = "Note for person",
            personId = personId.value
        )

        every { quickNoteCommandPort.createQuickNote(any()) } throws PersonNotFoundException(personId)

        mockMvc.perform(
            post("/api/v1/quick-notes")
                .with(authentication(authenticatedJwt(userId)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("Person not found"))
    }

    @Test
    fun `POST quick-notes - returns 401 without authentication`() {
        val request = CreateQuickNoteRequest(text = "Note")

        mockMvc.perform(
            post("/api/v1/quick-notes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isUnauthorized)
    }

    // ===== Get Quick Note =====

    @Test
    fun `GET quick-notes by id - returns quick note`() {
        val quickNote = sampleQuickNote()

        every { quickNoteQueryPort.getQuickNote(any()) } returns quickNote

        mockMvc.perform(
            get("/api/v1/quick-notes/${quickNoteId.value}")
                .with(authentication(authenticatedJwt(userId)))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(quickNote.id.value.toString()))
            .andExpect(jsonPath("$.text").value("Remember to follow up on project"))
    }

    @Test
    fun `GET quick-notes by id - returns 404 when not found`() {
        every { quickNoteQueryPort.getQuickNote(any()) } throws QuickNoteNotFoundException(quickNoteId)

        mockMvc.perform(
            get("/api/v1/quick-notes/${quickNoteId.value}")
                .with(authentication(authenticatedJwt(userId)))
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("Quick note not found"))
    }

    // ===== List Quick Notes =====

    @Test
    fun `GET quick-notes - returns paginated list`() {
        val notesList = listOf(sampleQuickNote())
        val pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"))

        every { quickNoteQueryPort.listQuickNotes(any()) } returns
            PageImpl(notesList, pageable, 1)

        mockMvc.perform(
            get("/api/v1/quick-notes")
                .with(authentication(authenticatedJwt(userId)))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].text").value("Remember to follow up on project"))
    }

    @Test
    fun `GET quick-notes - filters by status`() {
        val notesList = listOf(sampleQuickNote())
        val pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"))

        every { quickNoteQueryPort.listQuickNotes(any()) } returns
            PageImpl(notesList, pageable, 1)

        mockMvc.perform(
            get("/api/v1/quick-notes?status=INBOX")
                .with(authentication(authenticatedJwt(userId)))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content.length()").value(1))
    }

    @Test
    fun `GET quick-notes - returns 401 without authentication`() {
        mockMvc.perform(
            get("/api/v1/quick-notes")
        )
            .andExpect(status().isUnauthorized)
    }

    // ===== Update Quick Note =====

    @Test
    fun `PUT quick-notes - updates and returns 200`() {
        val updated = sampleQuickNote(text = "Updated text")
        val request = UpdateQuickNoteRequest(text = "Updated text")

        every { quickNoteCommandPort.updateQuickNote(any()) } returns updated

        mockMvc.perform(
            put("/api/v1/quick-notes/${quickNoteId.value}")
                .with(authentication(authenticatedJwt(userId)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.text").value("Updated text"))
    }

    @Test
    fun `PUT quick-notes - returns 404 when not found`() {
        val request = UpdateQuickNoteRequest(text = "Updated")

        every { quickNoteCommandPort.updateQuickNote(any()) } throws QuickNoteNotFoundException(quickNoteId)

        mockMvc.perform(
            put("/api/v1/quick-notes/${quickNoteId.value}")
                .with(authentication(authenticatedJwt(userId)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isNotFound)
    }

    // ===== Assign to Person =====

    @Test
    fun `POST assign - assigns quick note to person`() {
        val assigned = sampleQuickNote(personId = personId)
        val request = AssignQuickNoteToPersonRequest(personId = personId.value)

        every { quickNoteCommandPort.assignToPerson(any()) } returns assigned

        mockMvc.perform(
            post("/api/v1/quick-notes/${quickNoteId.value}/assign")
                .with(authentication(authenticatedJwt(userId)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.personId").value(personId.value.toString()))
    }

    // ===== Status Transitions =====

    @Test
    fun `POST attach - marks quick note as attached`() {
        val entryId = UUID.randomUUID()
        val attached = sampleQuickNote(status = QuickNoteStatus.ATTACHED)

        every { quickNoteCommandPort.attachQuickNote(any()) } returns attached

        mockMvc.perform(
            post("/api/v1/quick-notes/${quickNoteId.value}/attach")
                .with(authentication(authenticatedJwt(userId)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"entryId": "$entryId"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("ATTACHED"))
    }

    @Test
    fun `POST convert - marks quick note as converted`() {
        val converted = sampleQuickNote(status = QuickNoteStatus.CONVERTED)

        every { quickNoteCommandPort.convertQuickNote(any()) } returns converted

        mockMvc.perform(
            post("/api/v1/quick-notes/${quickNoteId.value}/convert")
                .with(authentication(authenticatedJwt(userId)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"personId": "${personId.value}"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("CONVERTED"))
    }

    @Test
    fun `POST archive - marks quick note as archived`() {
        val archived = sampleQuickNote(status = QuickNoteStatus.ARCHIVED)

        every { quickNoteCommandPort.archiveQuickNote(any()) } returns archived

        mockMvc.perform(
            post("/api/v1/quick-notes/${quickNoteId.value}/archive")
                .with(authentication(authenticatedJwt(userId)))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("ARCHIVED"))
    }

    @Test
    fun `POST attach - returns 400 when not in INBOX status`() {
        every { quickNoteCommandPort.attachQuickNote(any()) } throws
            IllegalArgumentException("Can only attach a quick note with status INBOX, current status is ARCHIVED")

        mockMvc.perform(
            post("/api/v1/quick-notes/${quickNoteId.value}/attach")
                .with(authentication(authenticatedJwt(userId)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"entryId": "${UUID.randomUUID()}"}""")
        )
            .andExpect(status().isBadRequest)
    }

    // ===== Delete Quick Note =====

    @Test
    fun `DELETE quick-notes - deletes and returns 204`() {
        every { quickNoteCommandPort.deleteQuickNote(any()) } returns Unit

        mockMvc.perform(
            delete("/api/v1/quick-notes/${quickNoteId.value}")
                .with(authentication(authenticatedJwt(userId)))
        )
            .andExpect(status().isNoContent)
    }

    @Test
    fun `DELETE quick-notes - returns 404 when not found`() {
        every { quickNoteCommandPort.deleteQuickNote(any()) } throws QuickNoteNotFoundException(quickNoteId)

        mockMvc.perform(
            delete("/api/v1/quick-notes/${quickNoteId.value}")
                .with(authentication(authenticatedJwt(userId)))
        )
            .andExpect(status().isNotFound)
    }
}
