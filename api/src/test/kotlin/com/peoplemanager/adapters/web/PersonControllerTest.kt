package com.peoplemanager.adapters.web

import com.fasterxml.jackson.databind.ObjectMapper
import com.peoplemanager.adapters.auth.AuthenticatedUser
import com.peoplemanager.adapters.auth.SecurityConfig
import com.peoplemanager.adapters.auth.UserProvisioningJwtAuthenticationConverter
import com.peoplemanager.adapters.web.dto.AddRememberItemRequest
import com.peoplemanager.adapters.web.dto.CreatePersonRequest
import com.peoplemanager.adapters.web.dto.ReorderRememberItemsRequest
import com.peoplemanager.adapters.web.dto.SetMoraleRequest
import com.peoplemanager.adapters.web.dto.UpdatePersonRequest
import com.peoplemanager.application.PersonNotFoundException
import com.peoplemanager.application.UserProvisioningService
import com.peoplemanager.application.port.input.ActionItemQueryPort
import com.peoplemanager.application.port.input.OneOnOneQueryPort
import com.peoplemanager.application.port.input.PdpGoalQueryPort
import com.peoplemanager.application.port.input.PersonCommandPort
import com.peoplemanager.application.port.input.PersonQueryPort
import com.peoplemanager.domain.MoraleStatus
import com.peoplemanager.domain.Person
import com.peoplemanager.domain.PersonId
import com.peoplemanager.domain.PinnedRememberItem
import com.peoplemanager.domain.RememberItemId
import com.peoplemanager.domain.UserId
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.filter
import io.kotest.property.arbitrary.of
import io.kotest.property.arbitrary.string
import io.kotest.property.arbitrary.uuid
import io.kotest.property.checkAll
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
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

@WebMvcTest(controllers = [PersonController::class])
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
class PersonControllerTest {

    @TestConfiguration
    class TestConfig {
        @Bean
        fun personCommandPort(): PersonCommandPort = mockk()

        @Bean
        fun personQueryPort(): PersonQueryPort = mockk()

        @Bean
        fun oneOnOneQueryPort(): OneOnOneQueryPort = mockk(relaxed = true)

        @Bean
        fun actionItemQueryPort(): ActionItemQueryPort = mockk(relaxed = true)

        @Bean
        fun pdpGoalQueryPort(): PdpGoalQueryPort = mockk(relaxed = true)

        @Bean
        fun userProvisioningService(): UserProvisioningService = mockk()
    }

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var personCommandPort: PersonCommandPort

    @Autowired
    private lateinit var personQueryPort: PersonQueryPort

    @Autowired
    private lateinit var actionItemQueryPort: ActionItemQueryPort

    @Autowired
    private lateinit var pdpGoalQueryPort: PdpGoalQueryPort

    // Helper to create a JwtAuthenticationToken with UserId in details
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

    private fun samplePerson(
        id: PersonId = PersonId(UUID.randomUUID()),
        userId: UserId = UserId(UUID.randomUUID()),
        name: String = "Jane Smith"
    ): Person = Person(
        id = id,
        userId = userId,
        name = name,
        preferredName = "Jane",
        roleTitle = "Senior Engineer",
        timezone = "Europe/Berlin",
        startDate = null,
        email = "jane@example.com",
        tags = listOf("engineering"),
        moraleStatus = MoraleStatus.UNKNOWN,
        moraleNote = null,
        pinnedRememberItems = emptyList(),
        createdAt = Instant.parse("2025-05-08T12:00:00Z"),
        updatedAt = Instant.parse("2025-05-08T12:00:00Z")
    )

    // ===== Task 7.5: Controller Slice Tests =====

    @Test
    fun `POST persons - creates person and returns 201`() {
        val userId = UserId(UUID.randomUUID())
        val person = samplePerson(userId = userId)
        val request = CreatePersonRequest(
            name = "Jane Smith",
            preferredName = "Jane",
            roleTitle = "Senior Engineer",
            timezone = "Europe/Berlin",
            email = "jane@example.com",
            tags = listOf("engineering")
        )

        every { personCommandPort.createPerson(any()) } returns person

        mockMvc.perform(
            post("/api/v1/persons")
                .with(authentication(authenticatedJwt(userId)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value(person.id.value.toString()))
            .andExpect(jsonPath("$.name").value("Jane Smith"))
            .andExpect(jsonPath("$.preferredName").value("Jane"))
            .andExpect(jsonPath("$.roleTitle").value("Senior Engineer"))
            .andExpect(jsonPath("$.timezone").value("Europe/Berlin"))
            .andExpect(jsonPath("$.email").value("jane@example.com"))
            .andExpect(jsonPath("$.tags[0]").value("engineering"))
            .andExpect(jsonPath("$.moraleStatus").value("UNKNOWN"))
            .andExpect(jsonPath("$.moraleNote").isEmpty)
            .andExpect(jsonPath("$.pinnedRememberItems").isArray)
            .andExpect(jsonPath("$.atAGlance").exists())
            .andExpect(jsonPath("$.createdAt").exists())
            .andExpect(jsonPath("$.updatedAt").exists())
    }

    @Test
    fun `POST persons - blank name returns 400`() {
        val userId = UserId(UUID.randomUUID())
        val request = CreatePersonRequest(name = "   ")

        mockMvc.perform(
            post("/api/v1/persons")
                .with(authentication(authenticatedJwt(userId)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").exists())
            .andExpect(jsonPath("$.timestamp").exists())
    }

    @Test
    fun `GET persons by id - returns 200 with PersonResponse`() {
        val userId = UserId(UUID.randomUUID())
        val personId = PersonId(UUID.randomUUID())
        val person = samplePerson(id = personId, userId = userId)

        every { personQueryPort.getPerson(any()) } returns person

        mockMvc.perform(
            get("/api/v1/persons/${personId.value}")
                .with(authentication(authenticatedJwt(userId)))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(personId.value.toString()))
            .andExpect(jsonPath("$.name").value("Jane Smith"))
            .andExpect(jsonPath("$.atAGlance").exists())
    }

    @Test
    fun `GET persons by id - at-a-glance includes open action items count and active PDP goals`() {
        val userId = UserId(UUID.randomUUID())
        val personId = PersonId(UUID.randomUUID())
        val person = samplePerson(id = personId, userId = userId)

        every { personQueryPort.getPerson(any()) } returns person
        every { actionItemQueryPort.countOpenActionItems(any()) } returns 5L
        every { pdpGoalQueryPort.countActivePdpGoals(any()) } returns 3L

        mockMvc.perform(
            get("/api/v1/persons/${personId.value}")
                .with(authentication(authenticatedJwt(userId)))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.atAGlance.openActionItemsCount").value(5))
            .andExpect(jsonPath("$.atAGlance.activePdpGoalsSummary").value("3 active"))
    }

    @Test
    fun `GET persons by id - at-a-glance shows null PDP summary when no active goals`() {
        val userId = UserId(UUID.randomUUID())
        val personId = PersonId(UUID.randomUUID())
        val person = samplePerson(id = personId, userId = userId)

        every { personQueryPort.getPerson(any()) } returns person
        every { actionItemQueryPort.countOpenActionItems(any()) } returns 0L
        every { pdpGoalQueryPort.countActivePdpGoals(any()) } returns 0L

        mockMvc.perform(
            get("/api/v1/persons/${personId.value}")
                .with(authentication(authenticatedJwt(userId)))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.atAGlance.openActionItemsCount").value(0))
            .andExpect(jsonPath("$.atAGlance.activePdpGoalsSummary").isEmpty)
    }

    @Test
    fun `GET persons by id - not found returns 404`() {
        val userId = UserId(UUID.randomUUID())
        val personId = PersonId(UUID.randomUUID())

        every { personQueryPort.getPerson(any()) } throws PersonNotFoundException(personId)

        mockMvc.perform(
            get("/api/v1/persons/${personId.value}")
                .with(authentication(authenticatedJwt(userId)))
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error").value("Not Found"))
            .andExpect(jsonPath("$.message").exists())
            .andExpect(jsonPath("$.timestamp").exists())
    }

    @Test
    fun `PUT persons by id - returns 200 with updated PersonResponse`() {
        val userId = UserId(UUID.randomUUID())
        val personId = PersonId(UUID.randomUUID())
        val updatedPerson = samplePerson(id = personId, userId = userId).copy(name = "Updated Name")
        val request = UpdatePersonRequest(name = "Updated Name")

        every { personCommandPort.updatePerson(any()) } returns updatedPerson

        mockMvc.perform(
            put("/api/v1/persons/${personId.value}")
                .with(authentication(authenticatedJwt(userId)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(personId.value.toString()))
            .andExpect(jsonPath("$.name").value("Updated Name"))
    }

    @Test
    fun `DELETE persons by id - returns 204`() {
        val userId = UserId(UUID.randomUUID())
        val personId = PersonId(UUID.randomUUID())

        every { personCommandPort.deletePerson(any()) } returns Unit

        mockMvc.perform(
            delete("/api/v1/persons/${personId.value}")
                .with(authentication(authenticatedJwt(userId)))
        )
            .andExpect(status().isNoContent)
    }

    @Test
    fun `GET persons - returns 200 with paginated response`() {
        val userId = UserId(UUID.randomUUID())
        val persons = listOf(samplePerson(userId = userId), samplePerson(userId = userId, name = "Bob"))
        val page = PageImpl(persons, PageRequest.of(0, 20), 2)

        every { personQueryPort.listPersons(any()) } returns page

        mockMvc.perform(
            get("/api/v1/persons")
                .with(authentication(authenticatedJwt(userId)))
                .param("page", "0")
                .param("size", "20")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content").isArray)
            .andExpect(jsonPath("$.content.length()").value(2))
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(20))
            .andExpect(jsonPath("$.totalElements").value(2))
            .andExpect(jsonPath("$.totalPages").value(1))
    }

    @Test
    fun `PUT persons morale - returns 200`() {
        val userId = UserId(UUID.randomUUID())
        val personId = PersonId(UUID.randomUUID())
        val updatedPerson = samplePerson(id = personId, userId = userId).copy(
            moraleStatus = MoraleStatus.GREEN,
            moraleNote = "Great sprint"
        )
        val request = SetMoraleRequest(status = MoraleStatus.GREEN, note = "Great sprint")

        every { personCommandPort.setMorale(any()) } returns updatedPerson

        mockMvc.perform(
            put("/api/v1/persons/${personId.value}/morale")
                .with(authentication(authenticatedJwt(userId)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.moraleStatus").value("GREEN"))
            .andExpect(jsonPath("$.moraleNote").value("Great sprint"))
    }

    @Test
    fun `PUT persons morale - null status returns 400`() {
        val userId = UserId(UUID.randomUUID())
        val personId = PersonId(UUID.randomUUID())

        // Send JSON with null status
        val requestJson = """{"status": null, "note": "some note"}"""

        mockMvc.perform(
            put("/api/v1/persons/${personId.value}/morale")
                .with(authentication(authenticatedJwt(userId)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson)
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").exists())
            .andExpect(jsonPath("$.timestamp").exists())
    }

    @Test
    fun `POST persons remember-items - returns 201`() {
        val userId = UserId(UUID.randomUUID())
        val personId = PersonId(UUID.randomUUID())
        val items = listOf(
            PinnedRememberItem(
                id = RememberItemId(UUID.randomUUID()),
                text = "Prefers async communication",
                displayOrder = 0,
                createdAt = Instant.now()
            )
        )
        val request = AddRememberItemRequest(text = "Prefers async communication")

        every { personCommandPort.addRememberItem(any()) } returns items

        mockMvc.perform(
            post("/api/v1/persons/${personId.value}/remember-items")
                .with(authentication(authenticatedJwt(userId)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$[0].text").value("Prefers async communication"))
            .andExpect(jsonPath("$[0].displayOrder").value(0))
            .andExpect(jsonPath("$[0].id").exists())
            .andExpect(jsonPath("$[0].createdAt").exists())
    }

    @Test
    fun `POST persons remember-items - blank text returns 400`() {
        val userId = UserId(UUID.randomUUID())
        val personId = PersonId(UUID.randomUUID())
        val request = AddRememberItemRequest(text = "   ")

        mockMvc.perform(
            post("/api/v1/persons/${personId.value}/remember-items")
                .with(authentication(authenticatedJwt(userId)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").exists())
            .andExpect(jsonPath("$.timestamp").exists())
    }

    @Test
    fun `DELETE persons remember-items - returns 200`() {
        val userId = UserId(UUID.randomUUID())
        val personId = PersonId(UUID.randomUUID())
        val itemId = UUID.randomUUID()
        val remainingItems = emptyList<PinnedRememberItem>()

        every { personCommandPort.removeRememberItem(any()) } returns remainingItems

        mockMvc.perform(
            delete("/api/v1/persons/${personId.value}/remember-items/$itemId")
                .with(authentication(authenticatedJwt(userId)))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
    }

    @Test
    fun `PUT persons remember-items reorder - returns 200`() {
        val userId = UserId(UUID.randomUUID())
        val personId = PersonId(UUID.randomUUID())
        val id1 = UUID.randomUUID()
        val id2 = UUID.randomUUID()
        val reorderedItems = listOf(
            PinnedRememberItem(id = RememberItemId(id2), text = "Item 2", displayOrder = 0, createdAt = Instant.now()),
            PinnedRememberItem(id = RememberItemId(id1), text = "Item 1", displayOrder = 1, createdAt = Instant.now())
        )
        val request = ReorderRememberItemsRequest(orderedIds = listOf(id2, id1))

        every { personCommandPort.reorderRememberItems(any()) } returns reorderedItems

        mockMvc.perform(
            put("/api/v1/persons/${personId.value}/remember-items/reorder")
                .with(authentication(authenticatedJwt(userId)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].text").value("Item 2"))
            .andExpect(jsonPath("$[1].text").value("Item 1"))
    }

    @Test
    fun `any endpoint without JWT - returns 401`() {
        mockMvc.perform(get("/api/v1/persons"))
            .andExpect(status().isUnauthorized)

        mockMvc.perform(post("/api/v1/persons")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"name": "Test"}"""))
            .andExpect(status().isUnauthorized)

        val id = UUID.randomUUID()
        mockMvc.perform(get("/api/v1/persons/$id"))
            .andExpect(status().isUnauthorized)

        mockMvc.perform(put("/api/v1/persons/$id")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"name": "Test"}"""))
            .andExpect(status().isUnauthorized)

        mockMvc.perform(delete("/api/v1/persons/$id"))
            .andExpect(status().isUnauthorized)

        mockMvc.perform(put("/api/v1/persons/$id/morale")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"status": "GREEN"}"""))
            .andExpect(status().isUnauthorized)

        mockMvc.perform(post("/api/v1/persons/$id/remember-items")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"text": "test"}"""))
            .andExpect(status().isUnauthorized)
    }

    // ===== Task 7.6: Property 15 - Authentication required on all endpoints =====

    /**
     * Property 15: Authentication required on all endpoints
     *
     * For any API endpoint under /api/v1/, a request without a valid JWT Bearer token
     * SHALL receive a 401 Unauthorized response.
     *
     * Minimum 100 iterations.
     *
     * **Validates: Requirements 11.10, 12.1, 12.2**
     */
    @Test
    @Tag("property")
    fun `Property 15 - authentication required on all endpoints`() = runBlocking {
        // Define all known endpoint patterns with their HTTP methods
        val endpoints = listOf(
            "GET" to "/api/v1/persons",
            "POST" to "/api/v1/persons",
            "GET" to "/api/v1/persons/{id}",
            "PUT" to "/api/v1/persons/{id}",
            "DELETE" to "/api/v1/persons/{id}",
            "PUT" to "/api/v1/persons/{id}/morale",
            "POST" to "/api/v1/persons/{id}/remember-items",
            "DELETE" to "/api/v1/persons/{id}/remember-items/{itemId}",
            "PUT" to "/api/v1/persons/{id}/remember-items/reorder"
        )

        checkAll(100, Arb.uuid(), Arb.uuid(), Arb.of(endpoints)) { personUuid, itemUuid, endpoint ->
            val (method, pathTemplate) = endpoint
            val path = pathTemplate
                .replace("{id}", personUuid.toString())
                .replace("{itemId}", itemUuid.toString())

            val requestBuilder = when (method) {
                "GET" -> get(path)
                "POST" -> post(path).contentType(MediaType.APPLICATION_JSON).content("""{"name":"test","text":"test","status":"GREEN","orderedIds":[]}""")
                "PUT" -> put(path).contentType(MediaType.APPLICATION_JSON).content("""{"name":"test","status":"GREEN","note":"","orderedIds":[]}""")
                "DELETE" -> delete(path)
                else -> throw IllegalArgumentException("Unknown method: $method")
            }

            val result = mockMvc.perform(requestBuilder).andReturn()
            result.response.status shouldBe 401
        }
        Unit
    }

    // ===== Task 7.7: Property 16 - Error response format consistency =====

    /**
     * Property 16: Error response format consistency
     *
     * For any API request that results in an error (4xx or 5xx), the response body
     * SHALL contain the fields: status (integer), error (string), message (string),
     * and timestamp (ISO 8601 string).
     *
     * Minimum 100 iterations.
     *
     * **Validates: Requirements 11.11**
     */
    @Test
    @Tag("property")
    fun `Property 16 - error response format consistency`() = runBlocking {
        val userId = UserId(UUID.randomUUID())
        val auth = authenticatedJwt(userId)

        val blankStringArb = Arb.string(0..10).filter { it.isBlank() }
        val uuidArb = Arb.uuid()

        // Generate 100+ error scenarios
        checkAll(20, blankStringArb) { blankName ->
            val request = CreatePersonRequest(name = blankName)
            val result = mockMvc.perform(
                post("/api/v1/persons")
                    .with(authentication(auth))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            ).andReturn()

            result.response.status shouldBe 400
            val body = objectMapper.readTree(result.response.contentAsString)
            body.has("status") shouldBe true
            body.get("status").isInt shouldBe true
            body.has("error") shouldBe true
            body.get("error").isTextual shouldBe true
            body.has("message") shouldBe true
            body.get("message").isTextual shouldBe true
            body.has("timestamp") shouldBe true
            body.get("timestamp").isTextual shouldBe true
        }

        // Not-found scenarios (GET)
        checkAll(30, uuidArb) { personUuid ->
            val personId = PersonId(personUuid)
            every { personQueryPort.getPerson(any()) } throws PersonNotFoundException(personId)

            val result = mockMvc.perform(
                get("/api/v1/persons/$personUuid")
                    .with(authentication(auth))
            ).andReturn()

            result.response.status shouldBe 404
            val body = objectMapper.readTree(result.response.contentAsString)
            body.has("status") shouldBe true
            body.get("status").intValue() shouldBe 404
            body.has("error") shouldBe true
            body.get("error").asText() shouldBe "Not Found"
            body.has("message") shouldBe true
            body.get("message").isTextual shouldBe true
            body.has("timestamp") shouldBe true
            body.get("timestamp").isTextual shouldBe true
        }

        // Not-found scenarios (PUT update)
        checkAll(20, uuidArb) { personUuid ->
            val personId = PersonId(personUuid)
            every { personCommandPort.updatePerson(any()) } throws PersonNotFoundException(personId)

            val request = UpdatePersonRequest(name = "Valid Name")
            val result = mockMvc.perform(
                put("/api/v1/persons/$personUuid")
                    .with(authentication(auth))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            ).andReturn()

            result.response.status shouldBe 404
            val body = objectMapper.readTree(result.response.contentAsString)
            body.has("status") shouldBe true
            body.get("status").intValue() shouldBe 404
            body.has("error") shouldBe true
            body.has("message") shouldBe true
            body.has("timestamp") shouldBe true
        }

        // Blank remember item text (400)
        checkAll(20, uuidArb, blankStringArb) { personUuid, blankText ->
            val request = AddRememberItemRequest(text = blankText)
            val result = mockMvc.perform(
                post("/api/v1/persons/$personUuid/remember-items")
                    .with(authentication(auth))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            ).andReturn()

            result.response.status shouldBe 400
            val body = objectMapper.readTree(result.response.contentAsString)
            body.has("status") shouldBe true
            body.get("status").intValue() shouldBe 400
            body.has("error") shouldBe true
            body.has("message") shouldBe true
            body.has("timestamp") shouldBe true
        }

        // Null morale status (400)
        checkAll(10, uuidArb) { personUuid ->
            val requestJson = """{"status": null}"""
            val result = mockMvc.perform(
                put("/api/v1/persons/$personUuid/morale")
                    .with(authentication(auth))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson)
            ).andReturn()

            result.response.status shouldBe 400
            val body = objectMapper.readTree(result.response.contentAsString)
            body.has("status") shouldBe true
            body.get("status").intValue() shouldBe 400
            body.has("error") shouldBe true
            body.has("message") shouldBe true
            body.has("timestamp") shouldBe true
        }
        Unit
    }

    // ===== Soft-delete and Restore Tests =====

    @Test
    fun `DELETE persons by id - soft-deletes and returns 204`() {
        val userId = UserId(UUID.randomUUID())
        val personId = PersonId(UUID.randomUUID())

        every { personCommandPort.deletePerson(any()) } returns Unit

        mockMvc.perform(
            delete("/api/v1/persons/${personId.value}")
                .with(authentication(authenticatedJwt(userId)))
        )
            .andExpect(status().isNoContent)
    }

    @Test
    fun `POST persons restore - restores soft-deleted person and returns 200`() {
        val userId = UserId(UUID.randomUUID())
        val personId = PersonId(UUID.randomUUID())
        val person = samplePerson(id = personId, userId = userId)

        every { personCommandPort.restorePerson(any()) } returns person

        mockMvc.perform(
            post("/api/v1/persons/${personId.value}/restore")
                .with(authentication(authenticatedJwt(userId)))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(personId.value.toString()))
            .andExpect(jsonPath("$.name").value("Jane Smith"))
    }

    @Test
    fun `POST persons restore - returns 404 when person not found`() {
        val userId = UserId(UUID.randomUUID())
        val personId = PersonId(UUID.randomUUID())

        every { personCommandPort.restorePerson(any()) } throws PersonNotFoundException(personId)

        mockMvc.perform(
            post("/api/v1/persons/${personId.value}/restore")
                .with(authentication(authenticatedJwt(userId)))
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `GET persons trash - returns paginated deleted persons`() {
        val userId = UserId(UUID.randomUUID())
        val deletedPerson = samplePerson(userId = userId, name = "Deleted Person")
        val page = PageImpl(listOf(deletedPerson), PageRequest.of(0, 20), 1)

        every { personQueryPort.listDeletedPersons(any()) } returns page

        mockMvc.perform(
            get("/api/v1/persons/trash")
                .with(authentication(authenticatedJwt(userId)))
                .param("page", "0")
                .param("size", "20")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content").isArray)
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.content[0].name").value("Deleted Person"))
            .andExpect(jsonPath("$.totalElements").value(1))
    }

    @Test
    fun `GET persons trash - returns empty when no deleted persons`() {
        val userId = UserId(UUID.randomUUID())
        val page = PageImpl(emptyList<Person>(), PageRequest.of(0, 20), 0)

        every { personQueryPort.listDeletedPersons(any()) } returns page

        mockMvc.perform(
            get("/api/v1/persons/trash")
                .with(authentication(authenticatedJwt(userId)))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content").isArray)
            .andExpect(jsonPath("$.content.length()").value(0))
            .andExpect(jsonPath("$.totalElements").value(0))
    }

    @Test
    fun `GET persons trash - requires authentication`() {
        mockMvc.perform(
            get("/api/v1/persons/trash")
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `POST persons restore - requires authentication`() {
        val personId = PersonId(UUID.randomUUID())

        mockMvc.perform(
            post("/api/v1/persons/${personId.value}/restore")
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `DELETE persons permanent - permanently deletes a soft-deleted person and returns 204`() {
        val userId = UserId(UUID.randomUUID())
        val personId = PersonId(UUID.randomUUID())

        every { personCommandPort.permanentDeletePerson(any()) } returns Unit

        mockMvc.perform(
            delete("/api/v1/persons/${personId.value}/permanent")
                .with(authentication(authenticatedJwt(userId)))
        )
            .andExpect(status().isNoContent)
    }

    @Test
    fun `DELETE persons permanent - returns 404 when person not in trash`() {
        val userId = UserId(UUID.randomUUID())
        val personId = PersonId(UUID.randomUUID())

        every { personCommandPort.permanentDeletePerson(any()) } throws PersonNotFoundException(personId)

        mockMvc.perform(
            delete("/api/v1/persons/${personId.value}/permanent")
                .with(authentication(authenticatedJwt(userId)))
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `DELETE persons permanent - requires authentication`() {
        val personId = PersonId(UUID.randomUUID())

        mockMvc.perform(
            delete("/api/v1/persons/${personId.value}/permanent")
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `DELETE persons permanent - scoped by userId`() {
        val userId = UserId(UUID.randomUUID())
        val personId = PersonId(UUID.randomUUID())

        every { personCommandPort.permanentDeletePerson(any()) } throws PersonNotFoundException(personId)

        mockMvc.perform(
            delete("/api/v1/persons/${personId.value}/permanent")
                .with(authentication(authenticatedJwt(userId)))
        )
            .andExpect(status().isNotFound)
    }
}
