package com.peoplemanager.domain

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class GamificationStatsTest {

    @Nested
    inner class PdpProgressSummaryTests {

        @Test
        fun `should return 0 completion when no goals exist`() {
            val summary = PdpProgressSummary(totalActive = 0, totalAchieved = 0, totalPaused = 0, totalDropped = 0)
            summary.completionPercentage shouldBe 0
        }

        @Test
        fun `should return 100 when all goals achieved`() {
            val summary = PdpProgressSummary(totalActive = 0, totalAchieved = 10, totalPaused = 0, totalDropped = 0)
            summary.completionPercentage shouldBe 100
        }

        @Test
        fun `should return 50 when half goals achieved`() {
            val summary = PdpProgressSummary(totalActive = 5, totalAchieved = 5, totalPaused = 0, totalDropped = 0)
            summary.completionPercentage shouldBe 50
        }

        @Test
        fun `should calculate percentage correctly with all statuses`() {
            val summary = PdpProgressSummary(totalActive = 3, totalAchieved = 2, totalPaused = 1, totalDropped = 4)
            // 2 / 10 = 20%
            summary.completionPercentage shouldBe 20
        }

        @Test
        fun `should truncate decimal in percentage`() {
            val summary = PdpProgressSummary(totalActive = 2, totalAchieved = 1, totalPaused = 0, totalDropped = 0)
            // 1 / 3 = 33.33% → 33
            summary.completionPercentage shouldBe 33
        }
    }

    @Nested
    inner class AchievementTypeTests {

        @Test
        fun `should have all expected achievement types`() {
            val types = AchievementType.entries
            types.size shouldBe 13
        }
    }

    @Nested
    inner class StreakDataTests {

        @Test
        fun `should hold streak values`() {
            val data = StreakData(currentStreak = 5, longestStreak = 10, totalOneOnOnesHeld = 25)
            data.currentStreak shouldBe 5
            data.longestStreak shouldBe 10
            data.totalOneOnOnesHeld shouldBe 25
        }
    }
}
