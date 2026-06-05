package com.peoplemanager.application

import com.peoplemanager.application.port.output.UserRepository
import com.peoplemanager.domain.OidcIdentity
import com.peoplemanager.domain.User
import com.peoplemanager.domain.UserId
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class UserProvisioningServiceTest {

    private lateinit var userRepository: UserRepository
    private lateinit var userProvisioningService: UserProvisioningService

    @BeforeEach
    fun setUp() {
        userRepository = mockk()
        userProvisioningService = UserProvisioningService(userRepository)
    }

    @Test
    fun `should return existing user when found by OidcIdentity`() {
        val existingUser = User(
            id = UserId.generate(),
            oidcSubject = "sub-123",
            oidcIssuer = "https://auth.example.com",
            displayName = "Existing User",
            email = "existing@example.com"
        )
        val oidcIdentity = OidcIdentity(subject = "sub-123", issuer = "https://auth.example.com")

        every { userRepository.findByOidcIdentity(oidcIdentity) } returns existingUser

        val result = userProvisioningService.provisionUser(
            subject = "sub-123",
            issuer = "https://auth.example.com",
            displayName = "Existing User",
            email = "existing@example.com"
        )

        result shouldBe existingUser
        verify(exactly = 1) { userRepository.findByOidcIdentity(oidcIdentity) }
        verify(exactly = 0) { userRepository.save(any()) }
    }

    @Test
    fun `should create and save new user when not found`() {
        val oidcIdentity = OidcIdentity(subject = "sub-new", issuer = "https://auth.example.com")

        every { userRepository.findByOidcIdentity(oidcIdentity) } returns null
        val userSlot = slot<User>()
        every { userRepository.save(capture(userSlot)) } answers { userSlot.captured }

        val result = userProvisioningService.provisionUser(
            subject = "sub-new",
            issuer = "https://auth.example.com",
            displayName = "New User",
            email = "new@example.com"
        )

        result.oidcSubject shouldBe "sub-new"
        result.oidcIssuer shouldBe "https://auth.example.com"
        result.displayName shouldBe "New User"
        result.email shouldBe "new@example.com"

        verify(exactly = 1) { userRepository.findByOidcIdentity(oidcIdentity) }
        verify(exactly = 1) { userRepository.save(any()) }
    }

    @Test
    fun `should never create duplicate - idempotent provisioning`() {
        val existingUser = User(
            id = UserId.generate(),
            oidcSubject = "sub-idem",
            oidcIssuer = "https://auth.example.com",
            displayName = "Idempotent User",
            email = "idem@example.com"
        )
        val oidcIdentity = OidcIdentity(subject = "sub-idem", issuer = "https://auth.example.com")

        every { userRepository.findByOidcIdentity(oidcIdentity) } returns existingUser

        // Call provision multiple times
        val result1 = userProvisioningService.provisionUser(
            subject = "sub-idem",
            issuer = "https://auth.example.com",
            displayName = "Idempotent User",
            email = "idem@example.com"
        )
        val result2 = userProvisioningService.provisionUser(
            subject = "sub-idem",
            issuer = "https://auth.example.com",
            displayName = "Idempotent User",
            email = "idem@example.com"
        )

        result1 shouldBe existingUser
        result2 shouldBe existingUser
        result1.id shouldBe result2.id

        // save should never be called since user already exists
        verify(exactly = 0) { userRepository.save(any()) }
        verify(exactly = 2) { userRepository.findByOidcIdentity(oidcIdentity) }
    }
}
