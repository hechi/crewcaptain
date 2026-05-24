package com.peoplemanager.integration

import com.peoplemanager.adapters.persistence.JpaUserRepositoryAdapter
import com.peoplemanager.domain.OidcIdentity
import com.peoplemanager.domain.User
import com.peoplemanager.domain.UserId
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.transaction.annotation.Transactional
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.Instant

@SpringBootTest
@Testcontainers
@Transactional
class UserRepositoryIntegrationTest {

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
    lateinit var userRepository: JpaUserRepositoryAdapter

    @Test
    fun `save and findByOidcIdentity returns the saved user`() {
        val user = User(
            id = UserId.generate(),
            oidcSubject = "subject-123",
            oidcIssuer = "https://issuer.example.com",
            displayName = "Test User",
            email = "test@example.com",
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        val saved = userRepository.save(user)

        val found = userRepository.findByOidcIdentity(
            OidcIdentity(subject = "subject-123", issuer = "https://issuer.example.com")
        )

        found.shouldNotBeNull()
        found.id shouldBe saved.id
        found.oidcSubject shouldBe "subject-123"
        found.oidcIssuer shouldBe "https://issuer.example.com"
        found.displayName shouldBe "Test User"
        found.email shouldBe "test@example.com"
    }

    @Test
    fun `findByOidcIdentity returns null for non-existent user`() {
        val found = userRepository.findByOidcIdentity(
            OidcIdentity(subject = "non-existent-subject", issuer = "https://non-existent.example.com")
        )

        found.shouldBeNull()
    }

    @Test
    fun `unique constraint violation on duplicate oidc_subject and oidc_issuer`() {
        val user1 = User(
            id = UserId.generate(),
            oidcSubject = "duplicate-subject",
            oidcIssuer = "https://issuer.example.com",
            displayName = "User One",
            email = "one@example.com",
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        userRepository.save(user1)

        val user2 = User(
            id = UserId.generate(),
            oidcSubject = "duplicate-subject",
            oidcIssuer = "https://issuer.example.com",
            displayName = "User Two",
            email = "two@example.com",
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        assertThrows<Exception> {
            userRepository.save(user2)
            // Force flush to trigger the constraint violation
            @Suppress("UNUSED_VARIABLE")
            val result = userRepository.findByOidcIdentity(
                OidcIdentity(subject = "duplicate-subject", issuer = "https://issuer.example.com")
            )
        }
    }
}
