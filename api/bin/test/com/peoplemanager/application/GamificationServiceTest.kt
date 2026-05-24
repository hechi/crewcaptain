package com.peoplemanager.application

import com.peoplemanager.application.ports.ActionItemRepository
import com.peoplemanager.application.ports.KudosRepository
import com.peoplemanager.application.ports.OneOnOneEntryRepository
import com.peoplemanager.application.ports.OneOnOneSeriesRepository
import com.peoplemanager.application.ports.PdpGoalRepository
import com.peoplemanager.application.queries.GetGamificationStatsQuery
import com.peoplemanager.domain.*
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

class GamificationServiceTest {

    private val oneOnOneEntryRepository = mockk<OneOnOneEntryRepository>()
    private val oneOnOneSeriesRepository = mockk<OneOnOneSeriesRepository>()
    private val actionItemRepository = mockk<ActionItemRepository>()
    private val pdpGoalRepository = mockk<PdpGoalRepository>()
    private val kudosRepository = mockk<KudosRepository>()

    private val service = GamificationService(
        oneOnOneEntryRepository, oneOnOneSeriesRepository,
        actionItemRepository, pdpGoalRepository, kudosRepository
    )

    private val userId = UserId.generate()

    @BeforeEach
    fun setup() {
        clearAllMocks()
    }

    @Nested
    inner class GetGamificationStatsTests {

        @Test
        fun `should return empty stats when no data exists`() {
            every { oneOnOneEntryRepository.findAllMeetingDatesByUserId(userId) } returns emptyList()
            every { actionItemRepository.countByUserIdAndStatus(userId, ActionItemStatus.DONE) } returns 0
            every { pdpGoalRepository.countByUserIdAndStatus(userId, PdpGoalStatus.ACHIEVED) } returns 0
            every { pdpGoalRepository.countByUserIdAndStatus(userId, PdpGoalStatus.ACTIVE) } returns 0
            every { pdpGoalRepository.countByUserIdAndStatus(userId, PdpGoalStatus.PAUSED) } returns 0
            every { pdpGoalRepository.countByUserIdAndStatus(userId, PdpGoalStatus.DROPPED) } returns 0
            every { kudosRepository.countByUserId(userId) } returns 0

            val query = GetGamificationStatsQuery(userId = userId)
            val result = service.getGamificationStats(query)

            result.streaks.currentStreak shouldBe 0
            result.streaks.longestStreak shouldBe 0
            result.streaks.totalOneOnOnesHeld shouldBe 0
            result.achievements shouldHaveSize 13
            result.achievements.all { !it.unlocked } shouldBe true
            result.pdpProgress.totalActive shouldBe 0
            result.pdpProgress.totalAchieved shouldBe 0
            result.pdpProgress.completionPercentage shouldBe 0
        }

        @Test
        fun `should scope all queries by userId`() {
            every { oneOnOneEntryRepository.findAllMeetingDatesByUserId(userId) } returns emptyList()
            every { actionItemRepository.countByUserIdAndStatus(userId, ActionItemStatus.DONE) } returns 0
            every { pdpGoalRepository.countByUserIdAndStatus(userId, any()) } returns 0
            every { kudosRepository.countByUserId(userId) } returns 0

            val query = GetGamificationStatsQuery(userId = userId)
            service.getGamificationStats(query)

            verify { oneOnOneEntryRepository.findAllMeetingDatesByUserId(userId) }
            verify { actionItemRepository.countByUserIdAndStatus(userId, ActionItemStatus.DONE) }
            verify { pdpGoalRepository.countByUserIdAndStatus(userId, PdpGoalStatus.ACHIEVED) }
            verify { kudosRepository.countByUserId(userId) }
        }
    }

    @Nested
    inner class StreakComputationTests {

        @Test
        fun `should return zero streak when no meetings exist`() {
            every { oneOnOneEntryRepository.findAllMeetingDatesByUserId(userId) } returns emptyList()

            val result = service.computeStreaks(userId)

            result.currentStreak shouldBe 0
            result.longestStreak shouldBe 0
            result.totalOneOnOnesHeld shouldBe 0
        }

        @Test
        fun `should return streak of 1 when only one meeting this week`() {
            val today = LocalDate.now()
            val meetingDate = today.atStartOfDay().toInstant(ZoneOffset.UTC)

            every { oneOnOneEntryRepository.findAllMeetingDatesByUserId(userId) } returns listOf(meetingDate)

            val result = service.computeStreaks(userId)

            result.currentStreak shouldBe 1
            result.longestStreak shouldBe 1
            result.totalOneOnOnesHeld shouldBe 1
        }

        @Test
        fun `should count consecutive weeks as streak`() {
            val today = LocalDate.now()
            val meetings = (0..3).map { weeksAgo ->
                today.minusWeeks(weeksAgo.toLong()).atStartOfDay().toInstant(ZoneOffset.UTC)
            }

            every { oneOnOneEntryRepository.findAllMeetingDatesByUserId(userId) } returns meetings

            val result = service.computeStreaks(userId)

            result.currentStreak shouldBe 4
            result.longestStreak shouldBe 4
            result.totalOneOnOnesHeld shouldBe 4
        }

        @Test
        fun `should break streak when a week is missed`() {
            val today = LocalDate.now()
            // Meetings this week, last week, and 3 weeks ago (gap at 2 weeks ago)
            val meetings = listOf(
                today.atStartOfDay().toInstant(ZoneOffset.UTC),
                today.minusWeeks(1).atStartOfDay().toInstant(ZoneOffset.UTC),
                today.minusWeeks(3).atStartOfDay().toInstant(ZoneOffset.UTC),
                today.minusWeeks(4).atStartOfDay().toInstant(ZoneOffset.UTC),
            )

            every { oneOnOneEntryRepository.findAllMeetingDatesByUserId(userId) } returns meetings

            val result = service.computeStreaks(userId)

            result.currentStreak shouldBe 2
            result.longestStreak shouldBe 2
            result.totalOneOnOnesHeld shouldBe 4
        }

        @Test
        fun `should count multiple meetings in same week as one streak unit`() {
            val today = LocalDate.now()
            // Find the Monday of the current ISO week to ensure all dates are in the same week
            val monday = today.with(java.time.DayOfWeek.MONDAY)
            // Multiple meetings in the same ISO week (Mon, Tue, Wed)
            val meetings = listOf(
                monday.atStartOfDay().toInstant(ZoneOffset.UTC),
                monday.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC),
                monday.plusDays(2).atStartOfDay().toInstant(ZoneOffset.UTC),
            )

            every { oneOnOneEntryRepository.findAllMeetingDatesByUserId(userId) } returns meetings

            val result = service.computeStreaks(userId)

            result.currentStreak shouldBe 1
            result.longestStreak shouldBe 1
            result.totalOneOnOnesHeld shouldBe 3
        }

        @Test
        fun `should reset current streak when last meeting was more than a week ago`() {
            val today = LocalDate.now()
            // Meeting 3 weeks ago only
            val meetings = listOf(
                today.minusWeeks(3).atStartOfDay().toInstant(ZoneOffset.UTC),
            )

            every { oneOnOneEntryRepository.findAllMeetingDatesByUserId(userId) } returns meetings

            val result = service.computeStreaks(userId)

            result.currentStreak shouldBe 0
            result.longestStreak shouldBe 1
            result.totalOneOnOnesHeld shouldBe 1
        }
    }

    @Nested
    inner class AchievementComputationTests {

        @Test
        fun `should unlock first 1-1 achievement`() {
            every { actionItemRepository.countByUserIdAndStatus(userId, ActionItemStatus.DONE) } returns 0
            every { pdpGoalRepository.countByUserIdAndStatus(userId, PdpGoalStatus.ACHIEVED) } returns 0
            every { kudosRepository.countByUserId(userId) } returns 0

            val streaks = StreakData(currentStreak = 1, longestStreak = 1, totalOneOnOnesHeld = 1)
            val result = service.computeAchievements(userId, streaks)

            val achievement = result.first { it.type == AchievementType.FIRST_ONE_ON_ONE }
            achievement.unlocked shouldBe true
            achievement.current shouldBe 1
            achievement.target shouldBe 1
        }

        @Test
        fun `should unlock multiple 1-1 milestones`() {
            every { actionItemRepository.countByUserIdAndStatus(userId, ActionItemStatus.DONE) } returns 0
            every { pdpGoalRepository.countByUserIdAndStatus(userId, PdpGoalStatus.ACHIEVED) } returns 0
            every { kudosRepository.countByUserId(userId) } returns 0

            val streaks = StreakData(currentStreak = 5, longestStreak = 10, totalOneOnOnesHeld = 50)
            val result = service.computeAchievements(userId, streaks)

            result.first { it.type == AchievementType.FIRST_ONE_ON_ONE }.unlocked shouldBe true
            result.first { it.type == AchievementType.TEN_ONE_ON_ONES }.unlocked shouldBe true
            result.first { it.type == AchievementType.FIFTY_ONE_ON_ONES }.unlocked shouldBe true
            result.first { it.type == AchievementType.STREAK_SEVEN }.unlocked shouldBe true
        }

        @Test
        fun `should unlock action item milestones`() {
            every { actionItemRepository.countByUserIdAndStatus(userId, ActionItemStatus.DONE) } returns 100
            every { pdpGoalRepository.countByUserIdAndStatus(userId, PdpGoalStatus.ACHIEVED) } returns 0
            every { kudosRepository.countByUserId(userId) } returns 0

            val streaks = StreakData(currentStreak = 0, longestStreak = 0, totalOneOnOnesHeld = 0)
            val result = service.computeAchievements(userId, streaks)

            result.first { it.type == AchievementType.FIRST_ACTION_ITEM_CLOSED }.unlocked shouldBe true
            result.first { it.type == AchievementType.TEN_ACTION_ITEMS_CLOSED }.unlocked shouldBe true
            result.first { it.type == AchievementType.FIFTY_ACTION_ITEMS_CLOSED }.unlocked shouldBe true
            result.first { it.type == AchievementType.HUNDRED_ACTION_ITEMS_CLOSED }.unlocked shouldBe true
        }

        @Test
        fun `should unlock PDP goal milestones`() {
            every { actionItemRepository.countByUserIdAndStatus(userId, ActionItemStatus.DONE) } returns 0
            every { pdpGoalRepository.countByUserIdAndStatus(userId, PdpGoalStatus.ACHIEVED) } returns 5
            every { kudosRepository.countByUserId(userId) } returns 0

            val streaks = StreakData(currentStreak = 0, longestStreak = 0, totalOneOnOnesHeld = 0)
            val result = service.computeAchievements(userId, streaks)

            result.first { it.type == AchievementType.FIRST_PDP_GOAL_ACHIEVED }.unlocked shouldBe true
            result.first { it.type == AchievementType.FIVE_PDP_GOALS_ACHIEVED }.unlocked shouldBe true
        }

        @Test
        fun `should unlock kudos milestones`() {
            every { actionItemRepository.countByUserIdAndStatus(userId, ActionItemStatus.DONE) } returns 0
            every { pdpGoalRepository.countByUserIdAndStatus(userId, PdpGoalStatus.ACHIEVED) } returns 0
            every { kudosRepository.countByUserId(userId) } returns 10

            val streaks = StreakData(currentStreak = 0, longestStreak = 0, totalOneOnOnesHeld = 0)
            val result = service.computeAchievements(userId, streaks)

            result.first { it.type == AchievementType.FIRST_KUDOS_GIVEN }.unlocked shouldBe true
            result.first { it.type == AchievementType.TEN_KUDOS_GIVEN }.unlocked shouldBe true
        }

        @Test
        fun `should unlock streak milestones`() {
            every { actionItemRepository.countByUserIdAndStatus(userId, ActionItemStatus.DONE) } returns 0
            every { pdpGoalRepository.countByUserIdAndStatus(userId, PdpGoalStatus.ACHIEVED) } returns 0
            every { kudosRepository.countByUserId(userId) } returns 0

            val streaks = StreakData(currentStreak = 30, longestStreak = 30, totalOneOnOnesHeld = 30)
            val result = service.computeAchievements(userId, streaks)

            result.first { it.type == AchievementType.STREAK_SEVEN }.unlocked shouldBe true
            result.first { it.type == AchievementType.STREAK_THIRTY }.unlocked shouldBe true
        }

        @Test
        fun `should not unlock achievements below threshold`() {
            every { actionItemRepository.countByUserIdAndStatus(userId, ActionItemStatus.DONE) } returns 0
            every { pdpGoalRepository.countByUserIdAndStatus(userId, PdpGoalStatus.ACHIEVED) } returns 0
            every { kudosRepository.countByUserId(userId) } returns 0

            val streaks = StreakData(currentStreak = 0, longestStreak = 0, totalOneOnOnesHeld = 0)
            val result = service.computeAchievements(userId, streaks)

            result shouldHaveSize 13
            result.all { !it.unlocked } shouldBe true
            result.all { it.current == 0 } shouldBe true
        }
    }

    @Nested
    inner class ActivityHeatmapTests {

        @Test
        fun `should return empty heatmap when no meetings exist`() {
            every { oneOnOneEntryRepository.findAllMeetingDatesByUserId(userId) } returns emptyList()

            val result = service.computeActivityHeatmap(userId, 90)

            result shouldHaveSize 91 // 90 days + today
            result.all { it.count == 0 } shouldBe true
        }

        @Test
        fun `should count meetings per day`() {
            val today = LocalDate.now()
            val meetingDate = today.atStartOfDay().toInstant(ZoneOffset.UTC)
            val meetings = listOf(meetingDate, meetingDate.plusSeconds(3600))

            every { oneOnOneEntryRepository.findAllMeetingDatesByUserId(userId) } returns meetings

            val result = service.computeActivityHeatmap(userId, 90)

            val todayEntry = result.find { it.date == today }
            todayEntry?.count shouldBe 2
        }

        @Test
        fun `should exclude meetings outside the heatmap window`() {
            val today = LocalDate.now()
            val oldMeeting = today.minusDays(100).atStartOfDay().toInstant(ZoneOffset.UTC)

            every { oneOnOneEntryRepository.findAllMeetingDatesByUserId(userId) } returns listOf(oldMeeting)

            val result = service.computeActivityHeatmap(userId, 90)

            result.all { it.count == 0 } shouldBe true
        }

        @Test
        fun `should respect custom heatmap days parameter`() {
            every { oneOnOneEntryRepository.findAllMeetingDatesByUserId(userId) } returns emptyList()

            val result = service.computeActivityHeatmap(userId, 30)

            result shouldHaveSize 31 // 30 days + today
        }
    }

    @Nested
    inner class PdpProgressTests {

        @Test
        fun `should compute PDP progress summary`() {
            every { pdpGoalRepository.countByUserIdAndStatus(userId, PdpGoalStatus.ACTIVE) } returns 5
            every { pdpGoalRepository.countByUserIdAndStatus(userId, PdpGoalStatus.ACHIEVED) } returns 3
            every { pdpGoalRepository.countByUserIdAndStatus(userId, PdpGoalStatus.PAUSED) } returns 1
            every { pdpGoalRepository.countByUserIdAndStatus(userId, PdpGoalStatus.DROPPED) } returns 1

            val result = service.computePdpProgress(userId)

            result.totalActive shouldBe 5
            result.totalAchieved shouldBe 3
            result.totalPaused shouldBe 1
            result.totalDropped shouldBe 1
            result.completionPercentage shouldBe 30 // 3/10 = 30%
        }

        @Test
        fun `should return zero completion when no goals exist`() {
            every { pdpGoalRepository.countByUserIdAndStatus(userId, PdpGoalStatus.ACTIVE) } returns 0
            every { pdpGoalRepository.countByUserIdAndStatus(userId, PdpGoalStatus.ACHIEVED) } returns 0
            every { pdpGoalRepository.countByUserIdAndStatus(userId, PdpGoalStatus.PAUSED) } returns 0
            every { pdpGoalRepository.countByUserIdAndStatus(userId, PdpGoalStatus.DROPPED) } returns 0

            val result = service.computePdpProgress(userId)

            result.completionPercentage shouldBe 0
        }

        @Test
        fun `should return 100 percent when all goals achieved`() {
            every { pdpGoalRepository.countByUserIdAndStatus(userId, PdpGoalStatus.ACTIVE) } returns 0
            every { pdpGoalRepository.countByUserIdAndStatus(userId, PdpGoalStatus.ACHIEVED) } returns 5
            every { pdpGoalRepository.countByUserIdAndStatus(userId, PdpGoalStatus.PAUSED) } returns 0
            every { pdpGoalRepository.countByUserIdAndStatus(userId, PdpGoalStatus.DROPPED) } returns 0

            val result = service.computePdpProgress(userId)

            result.completionPercentage shouldBe 100
        }
    }
}
