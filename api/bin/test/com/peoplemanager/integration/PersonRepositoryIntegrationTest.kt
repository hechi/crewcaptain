package com.peoplemanager.integration

import com.peoplemanager.adapters.persistence.JpaPersonRepositoryAdapter
import com.peoplemanager.adapters.persistence.JpaUserRepositoryAdapter
import com.peoplemanager.domain.*
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
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
class PersonRepositoryIntegrationTest {

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
        // Clean up tables in correct order (child first)
        jdbcTemplate.execute("DELETE FROM pinned_remember_items")
        jdbcTemplate.execute("DELETE FROM persons")
        jdbcTemplate.execute("DELETE FROM users")

        // Create two users for isolation tests
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

    private fun createPerson(
        userId: UserId,
        name: String,
        tags: List<String> = emptyList(),
        moraleStatus: MoraleStatus = MoraleStatus.UNKNOWN,
        pinnedRememberItems: List<PinnedRememberItem> = emptyList()
    ): Person {
        return Person(
            id = PersonId.generate(),
            userId = userId,
            name = name,
            tags = tags,
            moraleStatus = moraleStatus,
            pinnedRememberItems = pinnedRememberItems,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
    }

    @Test
    fun `save and findByIdAndUserId returns the saved person`() {
        val person = createPerson(userA.id, "Alice Smith")
        val saved = personRepository.save(person)

        val found = personRepository.findByIdAndUserId(saved.id, userA.id)

        found.shouldNotBeNull()
        found.id shouldBe saved.id
        found.name shouldBe "Alice Smith"
        found.userId shouldBe userA.id
    }

    @Test
    fun `findByIdAndUserId returns null for wrong userId (data isolation)`() {
        val person = createPerson(userA.id, "Alice Smith")
        val saved = personRepository.save(person)

        // User B should not be able to access User A's person
        val found = personRepository.findByIdAndUserId(saved.id, userB.id)

        found.shouldBeNull()
    }

    @Test
    fun `findAllByUserId returns only that user's persons`() {
        personRepository.save(createPerson(userA.id, "Alice"))
        personRepository.save(createPerson(userA.id, "Bob"))
        personRepository.save(createPerson(userB.id, "Charlie"))

        val pageable = PageRequest.of(0, 20, Sort.by("name"))
        val userAPersons = personRepository.findAllByUserId(userA.id, pageable, null, null)
        val userBPersons = personRepository.findAllByUserId(userB.id, pageable, null, null)

        userAPersons.totalElements shouldBe 2
        userBPersons.totalElements shouldBe 1
        userBPersons.content[0].name shouldBe "Charlie"
    }

    @Test
    fun `findAllByUserId with pagination returns correct page`() {
        // Create 5 persons for User A
        personRepository.save(createPerson(userA.id, "Alice"))
        personRepository.save(createPerson(userA.id, "Bob"))
        personRepository.save(createPerson(userA.id, "Charlie"))
        personRepository.save(createPerson(userA.id, "Diana"))
        personRepository.save(createPerson(userA.id, "Eve"))

        val page0 = personRepository.findAllByUserId(
            userA.id, PageRequest.of(0, 2, Sort.by("name")), null, null
        )
        val page1 = personRepository.findAllByUserId(
            userA.id, PageRequest.of(1, 2, Sort.by("name")), null, null
        )
        val page2 = personRepository.findAllByUserId(
            userA.id, PageRequest.of(2, 2, Sort.by("name")), null, null
        )

        page0.totalElements shouldBe 5
        page0.totalPages shouldBe 3
        page0.content shouldHaveSize 2
        page0.content[0].name shouldBe "Alice"
        page0.content[1].name shouldBe "Bob"

        page1.content shouldHaveSize 2
        page1.content[0].name shouldBe "Charlie"
        page1.content[1].name shouldBe "Diana"

        page2.content shouldHaveSize 1
        page2.content[0].name shouldBe "Eve"
    }

    @Test
    fun `findAllByUserId with tag filter returns matching persons`() {
        personRepository.save(createPerson(userA.id, "Alice", tags = listOf("engineering", "senior")))
        personRepository.save(createPerson(userA.id, "Bob", tags = listOf("engineering")))
        personRepository.save(createPerson(userA.id, "Charlie", tags = listOf("design")))

        val pageable = PageRequest.of(0, 20, Sort.by("name"))
        val result = personRepository.findAllByUserId(userA.id, pageable, "engineering", null)

        result.totalElements shouldBe 2
        result.content.map { it.name } shouldContainExactly listOf("Alice", "Bob")
    }

    @Test
    fun `findAllByUserId with morale filter returns matching persons`() {
        personRepository.save(createPerson(userA.id, "Alice", moraleStatus = MoraleStatus.GREEN))
        personRepository.save(createPerson(userA.id, "Bob", moraleStatus = MoraleStatus.RED))
        personRepository.save(createPerson(userA.id, "Charlie", moraleStatus = MoraleStatus.GREEN))

        val pageable = PageRequest.of(0, 20, Sort.by("name"))
        val result = personRepository.findAllByUserId(userA.id, pageable, null, MoraleStatus.GREEN)

        result.totalElements shouldBe 2
        result.content.map { it.name } shouldContainExactly listOf("Alice", "Charlie")
    }

    @Test
    fun `findAllByUserId with both filters returns intersection`() {
        personRepository.save(
            createPerson(userA.id, "Alice", tags = listOf("engineering"), moraleStatus = MoraleStatus.GREEN)
        )
        personRepository.save(
            createPerson(userA.id, "Bob", tags = listOf("engineering"), moraleStatus = MoraleStatus.RED)
        )
        personRepository.save(
            createPerson(userA.id, "Charlie", tags = listOf("design"), moraleStatus = MoraleStatus.GREEN)
        )

        val pageable = PageRequest.of(0, 20, Sort.by("name"))
        val result = personRepository.findAllByUserId(userA.id, pageable, "engineering", MoraleStatus.GREEN)

        result.totalElements shouldBe 1
        result.content[0].name shouldBe "Alice"
    }

    @Test
    fun `deleteByIdAndUserId removes the person`() {
        val person = createPerson(userA.id, "Alice")
        val saved = personRepository.save(person)

        val deleted = personRepository.deleteByIdAndUserId(saved.id, userA.id)

        deleted shouldBe true
        personRepository.findByIdAndUserId(saved.id, userA.id).shouldBeNull()
    }

    @Test
    fun `deleteByIdAndUserId returns false for wrong userId`() {
        val person = createPerson(userA.id, "Alice")
        val saved = personRepository.save(person)

        // User B should not be able to delete User A's person
        val deleted = personRepository.deleteByIdAndUserId(saved.id, userB.id)

        deleted shouldBe false
        // Person should still exist for User A
        personRepository.findByIdAndUserId(saved.id, userA.id).shouldNotBeNull()
    }

    @Test
    fun `cascade delete - deleting a person removes its remember items`() {
        val person = createPerson(
            userA.id, "Alice",
            pinnedRememberItems = listOf(
                PinnedRememberItem(
                    id = RememberItemId.generate(),
                    text = "Prefers async communication",
                    displayOrder = 0,
                    createdAt = Instant.now()
                ),
                PinnedRememberItem(
                    id = RememberItemId.generate(),
                    text = "Working on Kotlin migration",
                    displayOrder = 1,
                    createdAt = Instant.now()
                )
            )
        )
        val saved = personRepository.save(person)

        // Verify remember items exist
        val itemCountBefore = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM pinned_remember_items WHERE person_id = ?",
            Int::class.java,
            saved.id.value
        )
        itemCountBefore shouldBe 2

        // Delete the person
        personRepository.deleteByIdAndUserId(saved.id, userA.id)

        // Verify remember items are cascade deleted
        val itemCountAfter = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM pinned_remember_items WHERE person_id = ?",
            Int::class.java,
            saved.id.value
        )
        itemCountAfter shouldBe 0
    }

    @Test
    fun `default sort - persons returned alphabetically by name`() {
        personRepository.save(createPerson(userA.id, "Charlie"))
        personRepository.save(createPerson(userA.id, "Alice"))
        personRepository.save(createPerson(userA.id, "Bob"))

        val pageable = PageRequest.of(0, 20, Sort.by("name"))
        val result = personRepository.findAllByUserId(userA.id, pageable, null, null)

        result.content.map { it.name } shouldContainExactly listOf("Alice", "Bob", "Charlie")
    }

    @Test
    fun `save person with remember items and verify they persist`() {
        val person = createPerson(
            userA.id, "Alice",
            pinnedRememberItems = listOf(
                PinnedRememberItem(
                    id = RememberItemId.generate(),
                    text = "Prefers morning meetings",
                    displayOrder = 0,
                    createdAt = Instant.now()
                ),
                PinnedRememberItem(
                    id = RememberItemId.generate(),
                    text = "Working on certification",
                    displayOrder = 1,
                    createdAt = Instant.now()
                )
            )
        )

        val saved = personRepository.save(person)
        val found = personRepository.findByIdAndUserId(saved.id, userA.id)

        found.shouldNotBeNull()
        found.pinnedRememberItems shouldHaveSize 2
        found.pinnedRememberItems[0].text shouldBe "Prefers morning meetings"
        found.pinnedRememberItems[0].displayOrder shouldBe 0
        found.pinnedRememberItems[1].text shouldBe "Working on certification"
        found.pinnedRememberItems[1].displayOrder shouldBe 1
    }
}
