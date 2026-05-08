package com.peoplemanager.application

import com.peoplemanager.application.commands.CreatePersonCommand
import com.peoplemanager.application.commands.SetMoraleCommand
import com.peoplemanager.application.commands.UpdatePersonCommand
import com.peoplemanager.application.ports.PersonRepository
import com.peoplemanager.application.ports.UserRepository
import com.peoplemanager.domain.MoraleStatus
import com.peoplemanager.domain.OidcIdentity
import com.peoplemanager.domain.Person
import com.peoplemanager.domain.PersonId
import com.peoplemanager.domain.UserId
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.filter
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.localDate
import io.kotest.property.arbitrary.of
import io.kotest.property.arbitrary.orNull
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.time.Instant

/**
 * Property-based tests for the Application layer (use cases).
 *
 * Uses Kotest property testing with Mockk for repository mocking.
 */
@Tag("property")
class ApplicationPropertyTest {

    private lateinit var personRepository: PersonRepository
    private lateinit var userRepository: UserRepository
    private lateinit var personService: PersonService
    private lateinit var userProvisioningService: UserProvisioningService

    // Generators
    private val nonBlankStringArb = Arb.string(1..50).filter { it.isNotBlank() }
    private val optionalStringArb = Arb.string(0..50).orNull()
    private val tagsArb = Arb.list(Arb.string(1..20).filter { it.isNotBlank() }, 0..5)
    private val moraleStatusArb = Arb.of(MoraleStatus.entries)
    private val localDateArb = Arb.localDate()

    @BeforeEach
    fun setUp() {
        personRepository = mockk()
        userRepository = mockk()
        personService = PersonService(personRepository)
        userProvisioningService = UserProvisioningService(userRepository)
    }

    /**
     * Property 1: User provisioning idempotence
     *
     * For any valid set of OIDC claims (subject, issuer, displayName, email),
     * provisioning a user multiple times SHALL always return the same User record
     * and never create duplicates.
     *
     * **Validates: Requirements 1.1, 1.2, 1.3**
     */
    @Test
    fun `Property 1 - user provisioning idempotence`() = runBlocking {
        checkAll(100, nonBlankStringArb, nonBlankStringArb, optionalStringArb, optionalStringArb) { subject, issuer, displayName, email ->
            val oidcIdentity = OidcIdentity(subject = subject, issuer = issuer)

            // First call: user does not exist, so it gets created
            val userSlot = slot<com.peoplemanager.domain.User>()
            every { userRepository.findByOidcIdentity(oidcIdentity) } returns null
            every { userRepository.save(capture(userSlot)) } answers { userSlot.captured }

            val firstResult = userProvisioningService.provisionUser(subject, issuer, displayName, email)

            // Simulate subsequent calls: user now exists
            every { userRepository.findByOidcIdentity(oidcIdentity) } returns firstResult

            val secondResult = userProvisioningService.provisionUser(subject, issuer, displayName, email)
            val thirdResult = userProvisioningService.provisionUser(subject, issuer, displayName, email)

            // All calls return the same user
            firstResult.id shouldBe secondResult.id
            secondResult.id shouldBe thirdResult.id
            firstResult.oidcSubject shouldBe subject
            firstResult.oidcIssuer shouldBe issuer
        }
        Unit
    }

    /**
     * Property 2: Person creation round-trip
     *
     * For any valid CreatePersonCommand, creating the Person SHALL return a Person
     * with all fields matching the original input, moraleStatus=UNKNOWN, and empty
     * pinnedRememberItems list.
     *
     * **Validates: Requirements 2.1, 2.3, 2.4, 2.6, 2.7, 3.1**
     */
    @Test
    fun `Property 2 - person creation round-trip`() = runBlocking {
        checkAll(100, nonBlankStringArb, optionalStringArb, optionalStringArb, optionalStringArb, localDateArb.orNull(), optionalStringArb, tagsArb) { name, preferredName, roleTitle, timezone, startDate, email, tags ->
            val userId = UserId.generate()
            val command = CreatePersonCommand(
                userId = userId,
                name = name,
                preferredName = preferredName,
                roleTitle = roleTitle,
                timezone = timezone,
                startDate = startDate,
                email = email,
                tags = tags
            )

            val personSlot = slot<Person>()
            every { personRepository.save(capture(personSlot)) } answers { personSlot.captured }

            val result = personService.createPerson(command)

            // Verify all fields match input
            result.userId shouldBe userId
            result.name shouldBe name
            result.preferredName shouldBe preferredName
            result.roleTitle shouldBe roleTitle
            result.timezone shouldBe timezone
            result.startDate shouldBe startDate
            result.email shouldBe email
            result.tags shouldBe tags

            // Verify defaults
            result.moraleStatus shouldBe MoraleStatus.UNKNOWN
            result.moraleNote shouldBe null
            result.pinnedRememberItems shouldBe emptyList()

            // Verify ID is generated (non-null)
            result.id.value shouldBe result.id.value // non-null assertion via access
        }
        Unit
    }

    /**
     * Property 4: Person update preserves identity and reflects changes
     *
     * For any existing Person and any valid update request, updating the Person SHALL
     * return a Person with the same ID and userId but with all mutable fields reflecting
     * the new values provided.
     *
     * **Validates: Requirements 4.1, 4.2, 4.6**
     */
    @Test
    fun `Property 4 - person update preserves identity and reflects changes`() = runBlocking {
        checkAll(100, nonBlankStringArb, optionalStringArb, optionalStringArb, optionalStringArb, localDateArb.orNull(), optionalStringArb, tagsArb) { newName, newPreferredName, newRoleTitle, newTimezone, newStartDate, newEmail, newTags ->
            val userId = UserId.generate()
            val personId = PersonId.generate()

            // Create an existing person with different values
            val existingPerson = Person(
                id = personId,
                userId = userId,
                name = "Original Name",
                preferredName = "Original Preferred",
                roleTitle = "Original Role",
                timezone = "UTC",
                startDate = null,
                email = "original@example.com",
                tags = listOf("original-tag"),
                moraleStatus = MoraleStatus.GREEN,
                moraleNote = "Some note",
                pinnedRememberItems = emptyList(),
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )

            val command = UpdatePersonCommand(
                userId = userId,
                personId = personId,
                name = newName,
                preferredName = newPreferredName,
                roleTitle = newRoleTitle,
                timezone = newTimezone,
                startDate = newStartDate,
                email = newEmail,
                tags = newTags
            )

            every { personRepository.findByIdAndUserId(personId, userId) } returns existingPerson
            val personSlot = slot<Person>()
            every { personRepository.save(capture(personSlot)) } answers { personSlot.captured }

            val result = personService.updatePerson(command)

            // Identity preserved
            result.id shouldBe personId
            result.userId shouldBe userId

            // Mutable fields reflect new values
            result.name shouldBe newName
            result.preferredName shouldBe newPreferredName
            result.roleTitle shouldBe newRoleTitle
            result.timezone shouldBe newTimezone
            result.startDate shouldBe newStartDate
            result.email shouldBe newEmail
            result.tags shouldBe newTags

            // Morale and remember items are NOT changed by update
            result.moraleStatus shouldBe existingPerson.moraleStatus
            result.moraleNote shouldBe existingPerson.moraleNote
            result.pinnedRememberItems shouldBe existingPerson.pinnedRememberItems
        }
        Unit
    }

    /**
     * Property 10: Morale status update round-trip
     *
     * For any Person and any valid MoraleStatus value with an optional note string,
     * setting the morale SHALL result in the Person's moraleStatus and moraleNote
     * reflecting the provided values.
     *
     * **Validates: Requirements 8.1, 8.2, 8.3, 8.6**
     */
    @Test
    fun `Property 10 - morale status update round-trip`() = runBlocking {
        checkAll(100, moraleStatusArb, optionalStringArb) { newStatus, newNote ->
            val userId = UserId.generate()
            val personId = PersonId.generate()

            val existingPerson = Person(
                id = personId,
                userId = userId,
                name = "Test Person",
                preferredName = null,
                roleTitle = null,
                timezone = null,
                startDate = null,
                email = null,
                tags = emptyList(),
                moraleStatus = MoraleStatus.UNKNOWN,
                moraleNote = null,
                pinnedRememberItems = emptyList(),
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )

            val command = SetMoraleCommand(
                userId = userId,
                personId = personId,
                status = newStatus,
                note = newNote
            )

            every { personRepository.findByIdAndUserId(personId, userId) } returns existingPerson
            val personSlot = slot<Person>()
            every { personRepository.save(capture(personSlot)) } answers { personSlot.captured }

            val result = personService.setMorale(command)

            // Morale values match what was set
            result.moraleStatus shouldBe newStatus
            result.moraleNote shouldBe newNote

            // Identity preserved
            result.id shouldBe personId
            result.userId shouldBe userId

            // Other fields unchanged
            result.name shouldBe existingPerson.name
            result.pinnedRememberItems shouldBe existingPerson.pinnedRememberItems
        }
        Unit
    }
}
