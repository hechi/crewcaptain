package com.peoplemanager.domain

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

class PdpGoalTest {

    private val userId = UserId.generate()
    private val personId = PersonId.generate()

    private fun createGoal(
        title: String = "Improve public speaking",
        description: String? = "Practice presentations monthly",
        targetDate: LocalDate? = LocalDate.of(2026, 12, 31),
        status: PdpGoalStatus = PdpGoalStatus.ACTIVE
    ) = PdpGoal(
        id = PdpGoalId.generate(),
        userId = userId,
        personId = personId,
        title = title,
        description = description,
        targetDate = targetDate,
        status = status
    )

    @Nested
    inner class CreationTests {

        @Test
        fun `should create goal with valid fields`() {
            val goal = createGoal()

            goal.title shouldBe "Improve public speaking"
            goal.description shouldBe "Practice presentations monthly"
            goal.targetDate shouldBe LocalDate.of(2026, 12, 31)
            goal.status shouldBe PdpGoalStatus.ACTIVE
            goal.userId shouldBe userId
            goal.personId shouldBe personId
        }

        @Test
        fun `should create goal with minimal fields`() {
            val goal = PdpGoal(
                id = PdpGoalId.generate(),
                userId = userId,
                personId = personId,
                title = "Learn Kotlin"
            )

            goal.title shouldBe "Learn Kotlin"
            goal.description shouldBe null
            goal.targetDate shouldBe null
            goal.status shouldBe PdpGoalStatus.ACTIVE
        }

        @Test
        fun `should reject blank title`() {
            shouldThrow<IllegalArgumentException> {
                createGoal(title = "")
            }.message shouldBe "PDP goal title must not be blank"
        }

        @Test
        fun `should reject whitespace-only title`() {
            shouldThrow<IllegalArgumentException> {
                createGoal(title = "   ")
            }.message shouldBe "PDP goal title must not be blank"
        }
    }

    @Nested
    inner class StatusTransitionTests {

        @Test
        fun `should achieve an active goal`() {
            val goal = createGoal(status = PdpGoalStatus.ACTIVE)

            val achieved = goal.achieve()

            achieved.status shouldBe PdpGoalStatus.ACHIEVED
        }

        @Test
        fun `should pause an active goal`() {
            val goal = createGoal(status = PdpGoalStatus.ACTIVE)

            val paused = goal.pause()

            paused.status shouldBe PdpGoalStatus.PAUSED
        }

        @Test
        fun `should drop an active goal`() {
            val goal = createGoal(status = PdpGoalStatus.ACTIVE)

            val dropped = goal.drop()

            dropped.status shouldBe PdpGoalStatus.DROPPED
        }

        @Test
        fun `should resume a paused goal`() {
            val goal = createGoal(status = PdpGoalStatus.PAUSED)

            val resumed = goal.resume()

            resumed.status shouldBe PdpGoalStatus.ACTIVE
        }

        @Test
        fun `should not achieve a paused goal`() {
            val goal = createGoal(status = PdpGoalStatus.PAUSED)

            shouldThrow<IllegalArgumentException> {
                goal.achieve()
            }.message shouldBe "Can only achieve a goal with status ACTIVE, current status is PAUSED"
        }

        @Test
        fun `should not achieve a dropped goal`() {
            val goal = createGoal(status = PdpGoalStatus.DROPPED)

            shouldThrow<IllegalArgumentException> {
                goal.achieve()
            }.message shouldBe "Can only achieve a goal with status ACTIVE, current status is DROPPED"
        }

        @Test
        fun `should not achieve an already achieved goal`() {
            val goal = createGoal(status = PdpGoalStatus.ACHIEVED)

            shouldThrow<IllegalArgumentException> {
                goal.achieve()
            }.message shouldBe "Can only achieve a goal with status ACTIVE, current status is ACHIEVED"
        }

        @Test
        fun `should not pause a dropped goal`() {
            val goal = createGoal(status = PdpGoalStatus.DROPPED)

            shouldThrow<IllegalArgumentException> {
                goal.pause()
            }.message shouldBe "Can only pause a goal with status ACTIVE, current status is DROPPED"
        }

        @Test
        fun `should not pause an achieved goal`() {
            val goal = createGoal(status = PdpGoalStatus.ACHIEVED)

            shouldThrow<IllegalArgumentException> {
                goal.pause()
            }.message shouldBe "Can only pause a goal with status ACTIVE, current status is ACHIEVED"
        }

        @Test
        fun `should not drop a paused goal`() {
            val goal = createGoal(status = PdpGoalStatus.PAUSED)

            shouldThrow<IllegalArgumentException> {
                goal.drop()
            }.message shouldBe "Can only drop a goal with status ACTIVE, current status is PAUSED"
        }

        @Test
        fun `should not drop an achieved goal`() {
            val goal = createGoal(status = PdpGoalStatus.ACHIEVED)

            shouldThrow<IllegalArgumentException> {
                goal.drop()
            }.message shouldBe "Can only drop a goal with status ACTIVE, current status is ACHIEVED"
        }

        @Test
        fun `should not resume an active goal`() {
            val goal = createGoal(status = PdpGoalStatus.ACTIVE)

            shouldThrow<IllegalArgumentException> {
                goal.resume()
            }.message shouldBe "Can only resume a goal with status PAUSED, current status is ACTIVE"
        }

        @Test
        fun `should not resume a dropped goal`() {
            val goal = createGoal(status = PdpGoalStatus.DROPPED)

            shouldThrow<IllegalArgumentException> {
                goal.resume()
            }.message shouldBe "Can only resume a goal with status PAUSED, current status is DROPPED"
        }

        @Test
        fun `should not resume an achieved goal`() {
            val goal = createGoal(status = PdpGoalStatus.ACHIEVED)

            shouldThrow<IllegalArgumentException> {
                goal.resume()
            }.message shouldBe "Can only resume a goal with status PAUSED, current status is ACHIEVED"
        }
    }

    @Nested
    inner class UpdateDetailsTests {

        @Test
        fun `should update title`() {
            val goal = createGoal()

            val updated = goal.updateDetails(title = "New title")

            updated.title shouldBe "New title"
            updated.description shouldBe goal.description
        }

        @Test
        fun `should update description`() {
            val goal = createGoal()

            val updated = goal.updateDetails(description = "New description")

            updated.description shouldBe "New description"
            updated.title shouldBe goal.title
        }

        @Test
        fun `should update target date`() {
            val goal = createGoal()
            val newDate = LocalDate.of(2027, 6, 1)

            val updated = goal.updateDetails(targetDate = newDate)

            updated.targetDate shouldBe newDate
        }

        @Test
        fun `should reject blank title on update`() {
            val goal = createGoal()

            shouldThrow<IllegalArgumentException> {
                goal.updateDetails(title = "  ")
            }.message shouldBe "PDP goal title must not be blank"
        }

        @Test
        fun `should update updatedAt timestamp`() {
            val goal = createGoal()

            val updated = goal.updateDetails(title = "Updated")

            updated.updatedAt.isAfter(goal.createdAt) shouldBe true
        }
    }
}
