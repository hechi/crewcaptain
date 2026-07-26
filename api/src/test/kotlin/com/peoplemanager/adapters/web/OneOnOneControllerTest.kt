package com.peoplemanager.adapters.web

import tools.jackson.databind.ObjectMapper
import com.peoplemanager.adapters.auth.SecurityConfig
import com.peoplemanager.adapters.auth.UserProvisioningJwtAuthenticationConverter
import com.peoplemanager.adapters.web.dto.AgendaItemRequest
import com.peoplemanager.adapters.web.dto.CreateOneOnOneEntryRequest
import com.peoplemanager.adapters.web.dto.UpdateOneOnOneEntryRequest
import com.peoplemanager.adapters.web.dto.UpsertSeriesRequest
import com.peoplemanager.application.OneOnOneEntryNotFoundException
import com.peoplemanager.application.PersonNotFoundException
import com.peoplemanager.application.UserProvisioningService
import com.peoplemanager.application.port.input.OneOnOneCommandPort
import com.peoplemanager.application.port.input.OneOnOneQueryPort
import com.peoplemanager.domain.*
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
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

@WebMvcTest(controllers = [OneOnOneController::class])
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
class OneOnOneControllerTest {

    @TestConfiguration
    class TestConfig {
        @Bean
        fun oneOnOneCommandPort(): OneOnOneCommandPort = mockk()

        @Bean
        fun oneOnOneQueryPort(): OneOnOneQueryPort = mockk()

        @Bean
        fun userProvisioningService(): UserProvisioningService = mockk()
    }

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var oneOnOneCommandPort: OneOnOneCommandPort

    @Autowired
    private lateinit var oneOnOneQueryPort: OneOnOneQueryPort

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

    // ===== Series Endpoints =====

    @Test
    fun `PUT one-on-one-series - creates series and returns 200`() {
        val series = OneOnOneSeries(
            id = OneOnOneSeriesId.generate(),
            userId = userId,
            personId = personId,
            cadenceType = CadenceType.BIWEEKLY,
            templateMarkdown = "## Template"
        )
        val request = UpsertSeriesRequest(
            cadenceType = CadenceType.BIWEEKLY,
            templateMarkdown = "## Template"
        )

        every { oneOnOneCommandPort.upsertSeries(any()) } returns series

        mockMvc.perform(
            put("/api/v1/persons/${personId.value}/one-on-one-series")
                .with(authentication(authenticatedJwt(userId)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(series.id.value.toString()))
            .andExpect(jsonPath("$.personId").value(personId.value.toString()))
            .andExpect(jsonPath("$.cadenceType").value("BIWEEKLY"))
            .andExpect(jsonPath("$.templateMarkdown").value("## Template"))
    }

    @Test
    fun `PUT one-on-one-series - null cadence type returns 400`() {
        val requestJson = """{"cadenceType": null}"""

        mockMvc.perform(
            put("/api/v1/persons/${personId.value}/one-on-one-series")
                .with(authentication(authenticatedJwt(userId)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson)
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.status").value(400))
    }

    @Test
    fun `PUT one-on-one-series - person not found returns 404`() {
        val request = UpsertSeriesRequest(cadenceType = CadenceType.WEEKLY)

        every { oneOnOneCommandPort.upsertSeries(any()) } throws PersonNotFoundException(personId)

        mockMvc.perform(
            put("/api/v1/persons/${personId.value}/one-on-one-series")
                .with(authentication(authenticatedJwt(userId)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.status").value(404))
    }

    @Test
    fun `GET one-on-one-series - returns 200 when exists`() {
        val series = OneOnOneSeries(
            id = OneOnOneSeriesId.generate(),
            userId = userId,
            personId = personId,
            cadenceType = CadenceType.MONTHLY
        )

        every { oneOnOneQueryPort.getSeries(any()) } returns series

        mockMvc.perform(
            get("/api/v1/persons/${personId.value}/one-on-one-series")
                .with(authentication(authenticatedJwt(userId)))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.cadenceType").value("MONTHLY"))
    }

    @Test
    fun `GET one-on-one-series - returns 404 when not configured`() {
        every { oneOnOneQueryPort.getSeries(any()) } returns null

        mockMvc.perform(
            get("/api/v1/persons/${personId.value}/one-on-one-series")
                .with(authentication(authenticatedJwt(userId)))
        )
            .andExpect(status().isNotFound)
    }

    // ===== Entry Endpoints =====

    @Test
    fun `POST one-on-one-entries - creates entry and returns 201`() {
        val entry = OneOnOneEntry(
            id = OneOnOneEntryId.generate(),
            userId = userId,
            personId = personId,
            meetingDate = Instant.parse("2025-05-08T14:00:00Z"),
            agendaItems = listOf(
                AgendaItem(id = AgendaItemId.generate(), text = "Review goals", displayOrder = 0)
            ),
            notesMarkdown = "## Notes",
            outcomesMarkdown = "Agreed on timeline",
            sensitive = false
        )
        val request = CreateOneOnOneEntryRequest(
            meetingDate = Instant.parse("2025-05-08T14:00:00Z"),
            agendaItems = listOf(AgendaItemRequest(text = "Review goals")),
            notesMarkdown = "## Notes",
            outcomesMarkdown = "Agreed on timeline"
        )

        every { oneOnOneCommandPort.createEntry(any()) } returns entry

        mockMvc.perform(
            post("/api/v1/persons/${personId.value}/one-on-one-entries")
                .with(authentication(authenticatedJwt(userId)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value(entry.id.value.toString()))
            .andExpect(jsonPath("$.personId").value(personId.value.toString()))
            .andExpect(jsonPath("$.meetingDate").value("2025-05-08T14:00:00Z"))
            .andExpect(jsonPath("$.agendaItems[0].text").value("Review goals"))
            .andExpect(jsonPath("$.notesMarkdown").value("## Notes"))
            .andExpect(jsonPath("$.outcomesMarkdown").value("Agreed on timeline"))
            .andExpect(jsonPath("$.sensitive").value(false))
    }

    @Test
    fun `POST one-on-one-entries - null meeting date returns 400`() {
        val requestJson = """{"meetingDate": null, "notesMarkdown": "notes"}"""

        mockMvc.perform(
            post("/api/v1/persons/${personId.value}/one-on-one-entries")
                .with(authentication(authenticatedJwt(userId)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson)
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.status").value(400))
    }

    @Test
    fun `POST one-on-one-entries - blank agenda item text returns 400`() {
        val request = CreateOneOnOneEntryRequest(
            meetingDate = Instant.now(),
            agendaItems = listOf(AgendaItemRequest(text = "   "))
        )

        mockMvc.perform(
            post("/api/v1/persons/${personId.value}/one-on-one-entries")
                .with(authentication(authenticatedJwt(userId)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.status").value(400))
    }

    @Test
    fun `POST one-on-one-entries - person not found returns 404`() {
        val request = CreateOneOnOneEntryRequest(
            meetingDate = Instant.now(),
            notesMarkdown = "Notes"
        )

        every { oneOnOneCommandPort.createEntry(any()) } throws PersonNotFoundException(personId)

        mockMvc.perform(
            post("/api/v1/persons/${personId.value}/one-on-one-entries")
                .with(authentication(authenticatedJwt(userId)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `GET one-on-one-entries by id - returns 200`() {
        val entryId = OneOnOneEntryId.generate()
        val entry = OneOnOneEntry(
            id = entryId,
            userId = userId,
            personId = personId,
            meetingDate = Instant.parse("2025-05-08T14:00:00Z"),
            notesMarkdown = "Notes"
        )

        every { oneOnOneQueryPort.getEntry(any()) } returns entry

        mockMvc.perform(
            get("/api/v1/persons/${personId.value}/one-on-one-entries/${entryId.value}")
                .with(authentication(authenticatedJwt(userId)))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(entryId.value.toString()))
            .andExpect(jsonPath("$.notesMarkdown").value("Notes"))
    }

    @Test
    fun `GET one-on-one-entries by id - not found returns 404`() {
        val entryId = OneOnOneEntryId.generate()

        every { oneOnOneQueryPort.getEntry(any()) } throws OneOnOneEntryNotFoundException(entryId)

        mockMvc.perform(
            get("/api/v1/persons/${personId.value}/one-on-one-entries/${entryId.value}")
                .with(authentication(authenticatedJwt(userId)))
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.status").value(404))
    }

    @Test
    fun `PUT one-on-one-entries - updates entry and returns 200`() {
        val entryId = OneOnOneEntryId.generate()
        val updatedEntry = OneOnOneEntry(
            id = entryId,
            userId = userId,
            personId = personId,
            meetingDate = Instant.parse("2025-05-08T14:00:00Z"),
            notesMarkdown = "Updated notes",
            sensitive = true
        )
        val request = UpdateOneOnOneEntryRequest(
            notesMarkdown = "Updated notes",
            sensitive = true
        )

        every { oneOnOneCommandPort.updateEntry(any()) } returns updatedEntry

        mockMvc.perform(
            put("/api/v1/persons/${personId.value}/one-on-one-entries/${entryId.value}")
                .with(authentication(authenticatedJwt(userId)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.notesMarkdown").value("Updated notes"))
            .andExpect(jsonPath("$.sensitive").value(true))
    }

    @Test
    fun `DELETE one-on-one-entries - returns 204`() {
        val entryId = OneOnOneEntryId.generate()

        every { oneOnOneCommandPort.deleteEntry(any()) } returns Unit

        mockMvc.perform(
            delete("/api/v1/persons/${personId.value}/one-on-one-entries/${entryId.value}")
                .with(authentication(authenticatedJwt(userId)))
        )
            .andExpect(status().isNoContent)
    }

    @Test
    fun `DELETE one-on-one-entries - not found returns 404`() {
        val entryId = OneOnOneEntryId.generate()

        every { oneOnOneCommandPort.deleteEntry(any()) } throws OneOnOneEntryNotFoundException(entryId)

        mockMvc.perform(
            delete("/api/v1/persons/${personId.value}/one-on-one-entries/${entryId.value}")
                .with(authentication(authenticatedJwt(userId)))
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `GET one-on-one-entries list - returns paginated response`() {
        val entries = listOf(
            OneOnOneEntry(
                id = OneOnOneEntryId.generate(),
                userId = userId,
                personId = personId,
                meetingDate = Instant.parse("2025-05-08T14:00:00Z"),
                notesMarkdown = "Entry 1"
            )
        )
        val pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "meetingDate"))
        val page = PageImpl(entries, pageable, 1)

        every { oneOnOneQueryPort.listEntries(any()) } returns page

        mockMvc.perform(
            get("/api/v1/persons/${personId.value}/one-on-one-entries")
                .with(authentication(authenticatedJwt(userId)))
                .param("page", "0")
                .param("size", "20")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content").isArray)
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(20))
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.totalPages").value(1))
    }

    @Test
    fun `GET one-on-one-entries list - person not found returns 404`() {
        every { oneOnOneQueryPort.listEntries(any()) } throws PersonNotFoundException(personId)

        mockMvc.perform(
            get("/api/v1/persons/${personId.value}/one-on-one-entries")
                .with(authentication(authenticatedJwt(userId)))
        )
            .andExpect(status().isNotFound)
    }

    // ===== Authentication Tests =====

    @Test
    fun `all 1-1 endpoints without JWT return 401`() {
        val pid = UUID.randomUUID()
        val eid = UUID.randomUUID()

        mockMvc.perform(put("/api/v1/persons/$pid/one-on-one-series")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"cadenceType":"WEEKLY"}"""))
            .andExpect(status().isUnauthorized)

        mockMvc.perform(get("/api/v1/persons/$pid/one-on-one-series"))
            .andExpect(status().isUnauthorized)

        mockMvc.perform(post("/api/v1/persons/$pid/one-on-one-entries")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"meetingDate":"2025-05-08T14:00:00Z"}"""))
            .andExpect(status().isUnauthorized)

        mockMvc.perform(get("/api/v1/persons/$pid/one-on-one-entries/$eid"))
            .andExpect(status().isUnauthorized)

        mockMvc.perform(put("/api/v1/persons/$pid/one-on-one-entries/$eid")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"notesMarkdown":"test"}"""))
            .andExpect(status().isUnauthorized)

        mockMvc.perform(delete("/api/v1/persons/$pid/one-on-one-entries/$eid"))
            .andExpect(status().isUnauthorized)

        mockMvc.perform(get("/api/v1/persons/$pid/one-on-one-entries"))
            .andExpect(status().isUnauthorized)
    }
}
