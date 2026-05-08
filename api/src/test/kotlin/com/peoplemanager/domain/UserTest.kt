package com.peoplemanager.domain

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant

class UserTest {

    @Test
    fun `should create User with all fields`() {
        val id = UserId.generate()
        val now = Instant.now()

        val user = User(
            id = id,
            oidcSubject = "auth0|12345",
            oidcIssuer = "https://issuer.example.com",
            displayName = "Jane Manager",
            email = "jane@example.com",
            createdAt = now,
            updatedAt = now
        )

        user.id shouldBe id
        user.oidcSubject shouldBe "auth0|12345"
        user.oidcIssuer shouldBe "https://issuer.example.com"
        user.displayName shouldBe "Jane Manager"
        user.email shouldBe "jane@example.com"
        user.createdAt shouldBe now
        user.updatedAt shouldBe now
    }

    @Test
    fun `should create User with optional fields as null`() {
        val user = User(
            id = UserId.generate(),
            oidcSubject = "sub-123",
            oidcIssuer = "https://issuer.example.com"
        )

        user.displayName shouldBe null
        user.email shouldBe null
        user.createdAt shouldNotBe null
        user.updatedAt shouldNotBe null
    }

    @Test
    fun `should reject blank oidcSubject`() {
        val exception = assertThrows<IllegalArgumentException> {
            User(
                id = UserId.generate(),
                oidcSubject = "   ",
                oidcIssuer = "https://issuer.example.com"
            )
        }
        exception.message shouldBe "OIDC subject must not be blank"
    }

    @Test
    fun `should reject empty oidcSubject`() {
        assertThrows<IllegalArgumentException> {
            User(
                id = UserId.generate(),
                oidcSubject = "",
                oidcIssuer = "https://issuer.example.com"
            )
        }
    }

    @Test
    fun `should reject blank oidcIssuer`() {
        val exception = assertThrows<IllegalArgumentException> {
            User(
                id = UserId.generate(),
                oidcSubject = "sub-123",
                oidcIssuer = "   "
            )
        }
        exception.message shouldBe "OIDC issuer must not be blank"
    }

    @Test
    fun `should reject empty oidcIssuer`() {
        assertThrows<IllegalArgumentException> {
            User(
                id = UserId.generate(),
                oidcSubject = "sub-123",
                oidcIssuer = ""
            )
        }
    }
}
