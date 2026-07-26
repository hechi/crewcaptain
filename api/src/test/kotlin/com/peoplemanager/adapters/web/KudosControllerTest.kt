package com.peoplemanager.adapters.web

import tools.jackson.databind.ObjectMapper
import com.peoplemanager.adapters.auth.SecurityConfig
import com.peoplemanager.adapters.auth.UserProvisioningJwtAuthenticationConverter
import com.peoplemanager.adapters.web.dto.CreateKudosRequest
import com.peoplemanager.application.KudosNotFoundException
import com.peoplemanager.application.PersonNotFoundException
import com.peoplemanager.application.UserProvisioningService
import com.peoplemanager.application.port.input.KudosCommandPort
import com.peoplemanager.application.port.input.KudosQueryPort
import com.peoplemanager.domain.*
import io.mockk.every
import io.mockk.mockk
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
import java.time.LocalDate
import java.util.UUID

@WebMvcTest(controllers = [KudosController::class])
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
class KudosControllerTest {

    @TestConfiguration
    class TestConfig {
        @Bean
        fun kudosCommandPort(): KudosCommandPort = mockk()

        @Bean
        fun kudosQueryPort(): KudosQueryPort = mockk()

        @Bean
        fun userProvisioningService(): UserProvisioningService = mockk()
    }

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var kudosCommandPort: KudosCommandPort

    @Autowired
    private lateinit var kudosQueryPort: KudosQueryPort

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
    private val kudosId = KudosId(UUID.randomUUID())

    private fun sampleKudos(
        id: KudosId = kudosId,
        text: String = "Great job on the presentation!",
        date: LocalDate = LocalDate.of(2026, 5, 10),
        tags: List<String> = listOf("impact", "collaboration")
    ) = Kudos(
        id = id,
        userId = userId,
        personId = personId,
        date = date,
        text = text,
        tags = tags
    )

    // ===== Create Kudos =====

    @Test
    fun `POST kudos - creates kudos and returns 201`() {
        val kudos = sampleKudos()
        val request = CreateKudosRequest(
            date = LocalDate.of(2026, 5, 10),
            text = "Great job on the presentation!",
            tags = listOf("impact", "collaboration")
        )

        every { kudosCommandPort.createKudos(any()) } returns kudos

        mockMvc.perform(
            post("/api/v1/persons/${personId.value}/kudos")
                .with(authentication(authenticatedJwt(userId)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value(kudos.id.value.toString()))
            .andExpect(jsonPath("$.text").value("Great job on the presentation!"))
            .andExpect(jsonPath("$.date").value("2026-05-10"))
            .andExpect(jsonPath("$.tags[0]").value("impact"))
            .andExpect(jsonPath("$.tags[1]").value("collaboration"))
    }

    @Test
    fun `POST kudos - returns 400 when text is blank`() {
        val request = CreateKudosRequest(text = "")

        mockMvc.perform(
            post("/api/v1/persons/${personId.value}/kudos")
                .with(authentication(authenticatedJwt(userId)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `POST kudos - returns 404 when person not found`() {
        val request = CreateKudosRequest(
            text = "Great work!",
            date = LocalDate.of(2026, 5, 10)
        )

        every { kudosCommandPort.createKudos(any()) } throws PersonNotFoundException(personId)

        mockMvc.perform(
            post("/api/v1/persons/${personId.value}/kudos")
                .with(authentication(authenticatedJwt(userId)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("Person not found"))
    }

    @Test
    fun `POST kudos - returns 401 without authentication`() {
        val request = CreateKudosRequest(text = "Great work!")

        mockMvc.perform(
            post("/api/v1/persons/${personId.value}/kudos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isUnauthorized)
    }

    // ===== Get Kudos =====

    @Test
    fun `GET kudos by id - returns kudos`() {
        val kudos = sampleKudos()

        every { kudosQueryPort.getKudos(any()) } returns kudos

        mockMvc.perform(
            get("/api/v1/persons/${personId.value}/kudos/${kudosId.value}")
                .with(authentication(authenticatedJwt(userId)))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(kudos.id.value.toString()))
            .andExpect(jsonPath("$.text").value("Great job on the presentation!"))
    }

    @Test
    fun `GET kudos by id - returns 404 when not found`() {
        every { kudosQueryPort.getKudos(any()) } throws KudosNotFoundException(kudosId)

        mockMvc.perform(
            get("/api/v1/persons/${personId.value}/kudos/${kudosId.value}")
                .with(authentication(authenticatedJwt(userId)))
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("Kudos not found"))
    }

    // ===== List Kudos by Person =====

    @Test
    fun `GET kudos by person - returns paginated list`() {
        val kudosList = listOf(sampleKudos())
        val pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "date"))

        every { kudosQueryPort.listKudosByPerson(any()) } returns
            PageImpl(kudosList, pageable, 1)

        mockMvc.perform(
            get("/api/v1/persons/${personId.value}/kudos")
                .with(authentication(authenticatedJwt(userId)))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].text").value("Great job on the presentation!"))
    }

    @Test
    fun `GET kudos by person - returns 404 when person not found`() {
        every { kudosQueryPort.listKudosByPerson(any()) } throws PersonNotFoundException(personId)

        mockMvc.perform(
            get("/api/v1/persons/${personId.value}/kudos")
                .with(authentication(authenticatedJwt(userId)))
        )
            .andExpect(status().isNotFound)
    }

    // ===== Delete Kudos =====

    @Test
    fun `DELETE kudos - deletes and returns 204`() {
        every { kudosCommandPort.deleteKudos(any()) } returns Unit

        mockMvc.perform(
            delete("/api/v1/persons/${personId.value}/kudos/${kudosId.value}")
                .with(authentication(authenticatedJwt(userId)))
        )
            .andExpect(status().isNoContent)
    }

    @Test
    fun `DELETE kudos - returns 404 when not found`() {
        every { kudosCommandPort.deleteKudos(any()) } throws KudosNotFoundException(kudosId)

        mockMvc.perform(
            delete("/api/v1/persons/${personId.value}/kudos/${kudosId.value}")
                .with(authentication(authenticatedJwt(userId)))
        )
            .andExpect(status().isNotFound)
    }

    // ===== List All Kudos (Cross-Person) =====

    @Test
    fun `GET all kudos - returns paginated list`() {
        val kudosList = listOf(sampleKudos())
        val pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "date"))

        every { kudosQueryPort.listAllKudos(any()) } returns
            PageImpl(kudosList, pageable, 1)

        mockMvc.perform(
            get("/api/v1/kudos")
                .with(authentication(authenticatedJwt(userId)))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.totalElements").value(1))
    }

    @Test
    fun `GET all kudos - returns 401 without authentication`() {
        mockMvc.perform(
            get("/api/v1/kudos")
        )
            .andExpect(status().isUnauthorized)
    }
}
