package com.peoplemanager.domain

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

class StrategyGoalTest {

    private val userId = UserId.generate()

    private fun createGoal(
        title: String = "Modernize tech stack",
        description: String? = "Migrate to cloud-native architecture",
        targetDate: LocalDate? = LocalDate.of(2026, 12, 31),
        status: StrategyGoalStatus = StrategyGoalStatus.ACTIVE,
        sensitive: Boolean = false
    ) = StrategyGoal(
        id = StrategyGoalId.generate(),
        userId = userId,
        title = title,
        description = description,
        targetDate = targetDate,
        status = status,
        sensitive = sensitive
    )

    @Nested
    inner class CreationTests {

        @Test
        fun `should create strategy goal with valid fields`() {
            val goal = createGoal()

            goal.title shouldBe "Modernize tech stack"
            goal.description shouldBe "Migrate to cloud-native architecture"
            goal.targetDate shouldBe LocalDate.of(2026, 12, 31)
            goal.status shouldBe StrategyGoalStatus.ACTIVE
            goal.sensitive shouldBe false
            goal.userId shouldBe userId
        }

        @Test
        fun `should create strategy goal with minimal fields`() {
            val goal = StrategyGoal(
                id = StrategyGoalId.generate(),
                userId = userId,
                title = "Build team cohesion"
            )

            goal.title shouldBe "Build team cohesion"
            goal.description shouldBe null
            goal.targetDate shouldBe null
            goal.status shouldBe StrategyGoalStatus.ACTIVE
            goal.sensitive shouldBe false
        }

        @Test
        fun `should create sensitive strategy goal`() {
            val goal = createGoal(sensitive = true)

            goal.sensitive shouldBe true
        }

        @Test
        fun `should reject blank title`() {
            shouldThrow<IllegalArgumentException> {
                createGoal(title = "")
            }.message shouldBe "Strategy goal title must not be blank"
        }

        @Test
        fun `should reject whitespace-only title`() {
            shouldThrow<IllegalArgumentException> {
                createGoal(title = "   ")
            }.message shouldBe "Strategy goal title must not be blank"
        }

        @Test
        fun `should reject title exceeding 500 characters`() {
            val longTitle = "A".repeat(501)
            shouldThrow<IllegalArgumentException> {
                createGoal(title = longTitle)
            }.message shouldBe "Strategy goal title must not exceed 500 characters"
        }

        @Test
        fun `should accept title at exactly 500 characters`() {
            val maxTitle = "A".repeat(500)
            val goal = createGoal(title = maxTitle)
            goal.title shouldBe maxTitle
        }

        @Test
        fun `should reject description exceeding 5000 characters`() {
            val longDescription = "A".repeat(5001)
            shouldThrow<IllegalArgumentException> {
                createGoal(description = longDescription)
            }.message shouldBe "Strategy goal description must not exceed 5000 characters"
        }

        @Test
        fun `should accept description at exactly 5000 characters`() {
            val maxDescription = "A".repeat(5000)
            val goal = createGoal(description = maxDescription)
            goal.description shouldBe maxDescription
        }
    }

    @Nested
    inner class StatusTransitionTests {

        @Test
        fun `should achieve an active goal`() {
            val goal = createGoal(status = StrategyGoalStatus.ACTIVE)

            val achieved = goal.achieve()

            achieved.status shouldBe StrategyGoalStatus.ACHIEVED
        }

        @Test
        fun `should drop an active goal`() {
            val goal = createGoal(status = StrategyGoalStatus.ACTIVE)

            val dropped = goal.drop()

            dropped.status shouldBe StrategyGoalStatus.DROPPED
        }

        @Test
        fun `should not achieve an already achieved goal`() {
            val goal = createGoal(status = StrategyGoalStatus.ACHIEVED)

            shouldThrow<IllegalArgumentException> {
                goal.achieve()
            }.message shouldBe "Can only achieve a goal with status ACTIVE, current status is ACHIEVED"
        }

        @Test
        fun `should not achieve a dropped goal`() {
            val goal = createGoal(status = StrategyGoalStatus.DROPPED)

            shouldThrow<IllegalArgumentException> {
                goal.achieve()
            }.message shouldBe "Can only achieve a goal with status ACTIVE, current status is DROPPED"
        }

        @Test
        fun `should not drop an already achieved goal`() {
            val goal = createGoal(status = StrategyGoalStatus.ACHIEVED)

            shouldThrow<IllegalArgumentException> {
                goal.drop()
            }.message shouldBe "Can only drop a goal with status ACTIVE, current status is ACHIEVED"
        }

        @Test
        fun `should not drop an already dropped goal`() {
            val goal = createGoal(status = StrategyGoalStatus.DROPPED)

            shouldThrow<IllegalArgumentException> {
                goal.drop()
            }.message shouldBe "Can only drop a goal with status ACTIVE, current status is DROPPED"
        }
    }

    @Nested
    inner class UpdateDetailsTests {

        @Test
        fun `should update title`() {
            val goal = createGoal()

            val updated = goal.updateDetails(title = "New strategy title")

            updated.title shouldBe "New strategy title"
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
            }.message shouldBe "Strategy goal title must not be blank"
        }

        @Test
        fun `should reject title exceeding 500 characters on update`() {
            val goal = createGoal()
            val longTitle = "A".repeat(501)

            shouldThrow<IllegalArgumentException> {
                goal.updateDetails(title = longTitle)
            }.message shouldBe "Strategy goal title must not exceed 500 characters"
        }

        @Test
        fun `should reject description exceeding 5000 characters on update`() {
            val goal = createGoal()
            val longDescription = "A".repeat(5001)

            shouldThrow<IllegalArgumentException> {
                goal.updateDetails(description = longDescription)
            }.message shouldBe "Strategy goal description must not exceed 5000 characters"
        }

        @Test
        fun `should update updatedAt timestamp`() {
            val goal = createGoal()

            val updated = goal.updateDetails(title = "Updated")

            updated.updatedAt.isAfter(goal.createdAt) shouldBe true
        }
    }

    @Nested
    inner class SensitiveToggleTests {

        @Test
        fun `should toggle sensitive flag from false to true`() {
            val goal = createGoal(sensitive = false)

            val toggled = goal.toggleSensitive()

            toggled.sensitive shouldBe true
        }

        @Test
        fun `should toggle sensitive flag from true to false`() {
            val goal = createGoal(sensitive = true)

            val toggled = goal.toggleSensitive()

            toggled.sensitive shouldBe false
        }

        @Test
        fun `should update updatedAt when toggling sensitive`() {
            val goal = createGoal()

            val toggled = goal.toggleSensitive()

            toggled.updatedAt.isAfter(goal.updatedAt) shouldBe true
        }
    }
}
