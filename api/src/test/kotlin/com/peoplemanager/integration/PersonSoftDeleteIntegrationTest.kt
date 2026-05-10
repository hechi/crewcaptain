package com.peoplemanager.integration

import com.peoplemanager.adapters.persistence.JpaPersonRepositoryAdapter
import com.peoplemanager.adapters.persistence.JpaUserRepositoryAdapter
import com.peoplemanager.domain.*
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.Instant

@SpringBootTest
@Testcontainers
class PersonSoftDeleteIntegrationTest {

    companion object {
        @Container
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:16")

        @DynamicPropertySource
        @JvmStatic
        fun configureProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { postgres.jdbcUrl }
            registry.add("spring.datasource.username") { postgres.username }
            registry.add("spring.datasource.password") { postgres.password }
            registry.add("spring.flyway.enabled") { "true" }
            registry.add("spring.jpa.hibernate.ddl-auto") { "validate" }
            registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri") { "http://localhost:9000" }
            registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri") { "http://localhost:9000/jwks" }
        }
    }

    @Autowired
    lateinit var personRepository: JpaPersonRepositoryAdapter

    @Autowired
    lateinit var userRepository: JpaUserRepositoryAdapter

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    private lateinit var userA: User
    private lateinit var userB: User

    @BeforeEach
    fun setUp() {
        jdbcTemplate.execute("DELETE FROM pinned_remember_items")
        jdbcTemplate.execute("DELETE FROM persons")
        jdbcTemplate.execute("DELETE FROM users")

        userA = userRepository.save(
            User(
                id = UserId.generate(),
                oidcSubject = "user-a-subject",
                oidcIssuer = "https://issuer.example.com",
                displayName = "User A",
                email = "usera@example.com",
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )
        )

        userB = userRepository.save(
            User(
                id = UserId.generate(),
                oidcSubject = "user-b-subject",
                oidcIssuer = "https://issuer.example.com",
                displayName = "User B",
                email = "userb@example.com",
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )
        )
    }

    private fun createPerson(userId: UserId, name: String): Person {
        return Person(
            id = PersonId.generate(),
            userId = userId,
            name = name,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
    }

    @Test
    fun `softDeleteByIdAndUserId marks person as deleted`() {
        val person = personRepository.save(createPerson(userA.id, "Alice"))

        val result = personRepository.softDeleteByIdAndUserId(person.id, userA.id)

        result shouldBe true
        // Should not be found via normal query
        personRepository.findByIdAndUserId(person.id, userA.id).shouldBeNull()
    }

    @Test
    fun `softDeleteByIdAndUserId returns false for wrong userId`() {
        val person = personRepository.save(createPerson(userA.id, "Alice"))

        val result = personRepository.softDeleteByIdAndUserId(person.id, userB.id)

        result shouldBe false
        // Person should still be accessible for User A
        personRepository.findByIdAndUserId(person.id, userA.id).shouldNotBeNull()
    }

    @Test
    fun `soft-deleted person is excluded from findAllByUserId`() {
        personRepository.save(createPerson(userA.id, "Alice"))
        val bob = personRepository.save(createPerson(userA.id, "Bob"))

        personRepository.softDeleteByIdAndUserId(bob.id, userA.id)

        val pageable = PageRequest.of(0, 20, Sort.by("name"))
        val result = personRepository.findAllByUserId(userA.id, pageable, null, null)

        result.totalElements shouldBe 1
        result.content[0].name shouldBe "Alice"
    }

    @Test
    fun `findDeletedByIdAndUserId returns soft-deleted person`() {
        val person = personRepository.save(createPerson(userA.id, "Alice"))
        personRepository.softDeleteByIdAndUserId(person.id, userA.id)

        val found = personRepository.findDeletedByIdAndUserId(person.id, userA.id)

        found.shouldNotBeNull()
        found.name shouldBe "Alice"
        found.deletedAt.shouldNotBeNull()
    }

    @Test
    fun `findDeletedByIdAndUserId returns null for non-deleted person`() {
        val person = personRepository.save(createPerson(userA.id, "Alice"))

        val found = personRepository.findDeletedByIdAndUserId(person.id, userA.id)

        found.shouldBeNull()
    }

    @Test
    fun `findDeletedByIdAndUserId enforces userId scoping`() {
        val person = personRepository.save(createPerson(userA.id, "Alice"))
        personRepository.softDeleteByIdAndUserId(person.id, userA.id)

        // User B should not see User A's deleted person
        val found = personRepository.findDeletedByIdAndUserId(person.id, userB.id)

        found.shouldBeNull()
    }

    @Test
    fun `findAllDeletedByUserId returns only deleted persons for that user`() {
        val alice = personRepository.save(createPerson(userA.id, "Alice"))
        personRepository.save(createPerson(userA.id, "Bob"))
        val charlie = personRepository.save(createPerson(userB.id, "Charlie"))

        personRepository.softDeleteByIdAndUserId(alice.id, userA.id)
        personRepository.softDeleteByIdAndUserId(charlie.id, userB.id)

        val pageable = PageRequest.of(0, 20)
        val userADeleted = personRepository.findAllDeletedByUserId(userA.id, pageable)
        val userBDeleted = personRepository.findAllDeletedByUserId(userB.id, pageable)

        userADeleted.totalElements shouldBe 1
        userADeleted.content[0].name shouldBe "Alice"

        userBDeleted.totalElements shouldBe 1
        userBDeleted.content[0].name shouldBe "Charlie"
    }

    @Test
    fun `restoreByIdAndUserId restores a soft-deleted person`() {
        val person = personRepository.save(createPerson(userA.id, "Alice"))
        personRepository.softDeleteByIdAndUserId(person.id, userA.id)

        // Verify it's deleted
        personRepository.findByIdAndUserId(person.id, userA.id).shouldBeNull()

        val result = personRepository.restoreByIdAndUserId(person.id, userA.id)

        result shouldBe true
        // Should be accessible again
        val restored = personRepository.findByIdAndUserId(person.id, userA.id)
        restored.shouldNotBeNull()
        restored.name shouldBe "Alice"
        restored.deletedAt.shouldBeNull()
    }

    @Test
    fun `restoreByIdAndUserId returns false for non-deleted person`() {
        val person = personRepository.save(createPerson(userA.id, "Alice"))

        val result = personRepository.restoreByIdAndUserId(person.id, userA.id)

        result shouldBe false
    }

    @Test
    fun `restoreByIdAndUserId returns false for wrong userId`() {
        val person = personRepository.save(createPerson(userA.id, "Alice"))
        personRepository.softDeleteByIdAndUserId(person.id, userA.id)

        // User B should not be able to restore User A's person
        val result = personRepository.restoreByIdAndUserId(person.id, userB.id)

        result shouldBe false
    }

    @Test
    fun `soft-deleted person preserves remember items for restore`() {
        val person = createPerson(userA.id, "Alice").addRememberItem("Important note")
        val saved = personRepository.save(person)

        personRepository.softDeleteByIdAndUserId(saved.id, userA.id)
        personRepository.restoreByIdAndUserId(saved.id, userA.id)

        val restored = personRepository.findByIdAndUserId(saved.id, userA.id)
        restored.shouldNotBeNull()
        restored.pinnedRememberItems shouldHaveSize 1
        restored.pinnedRememberItems[0].text shouldBe "Important note"
    }

    @Test
    fun `soft-deleted person is excluded from findAllByUserIdUnpaged`() {
        val alice = personRepository.save(createPerson(userA.id, "Alice"))
        personRepository.save(createPerson(userA.id, "Bob"))

        personRepository.softDeleteByIdAndUserId(alice.id, userA.id)

        val result = personRepository.findAllByUserIdUnpaged(userA.id)

        result shouldHaveSize 1
        result[0].name shouldBe "Bob"
    }

    @Test
    fun `double soft-delete returns false`() {
        val person = personRepository.save(createPerson(userA.id, "Alice"))
        personRepository.softDeleteByIdAndUserId(person.id, userA.id) shouldBe true

        // Second soft-delete should return false (already deleted)
        personRepository.softDeleteByIdAndUserId(person.id, userA.id) shouldBe false
    }
}
