package com.peoplemanager.application

import com.peoplemanager.application.ports.ActionItemRepository
import com.peoplemanager.application.ports.GamificationQueryPort
import com.peoplemanager.application.ports.KudosRepository
import com.peoplemanager.application.ports.OneOnOneEntryRepository
import com.peoplemanager.application.ports.OneOnOneSeriesRepository
import com.peoplemanager.application.ports.PdpGoalRepository
import com.peoplemanager.application.queries.GetGamificationStatsQuery
import com.peoplemanager.domain.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

@Service
@Transactional(readOnly = true)
class GamificationService(
    private val oneOnOneEntryRepository: OneOnOneEntryRepository,
    private val oneOnOneSeriesRepository: OneOnOneSeriesRepository,
    private val actionItemRepository: ActionItemRepository,
    private val pdpGoalRepository: PdpGoalRepository,
    private val kudosRepository: KudosRepository
) : GamificationQueryPort {

    override fun getGamificationStats(query: GetGamificationStatsQuery): GamificationStats {
        val userId = query.userId

        val streaks = computeStreaks(userId)
        val achievements = computeAchievements(userId, streaks)
        val activityHeatmap = computeActivityHeatmap(userId, query.heatmapDays)
        val pdpProgress = computePdpProgress(userId)

        return GamificationStats(
            streaks = streaks,
            achievements = achievements,
            activityHeatmap = activityHeatmap,
            pdpProgress = pdpProgress
        )
    }

    /**
     * Computes streak data based on weekly intervals.
     * A "streak week" is counted when at least one 1:1 was held in that calendar week.
     * Consecutive weeks with 1:1s form the streak.
     */
    internal fun computeStreaks(userId: UserId): StreakData {
        val allMeetingDates = oneOnOneEntryRepository.findAllMeetingDatesByUserId(userId)
        val totalOneOnOnesHeld = allMeetingDates.size

        if (allMeetingDates.isEmpty()) {
            return StreakData(currentStreak = 0, longestStreak = 0, totalOneOnOnesHeld = 0)
        }

        // Convert to weeks (ISO week numbers) and deduplicate
        val meetingWeeks = allMeetingDates
            .map { it.atZone(ZoneOffset.UTC).toLocalDate() }
            .map { weekKey(it) }
            .distinct()
            .sorted()

        // Calculate streaks based on consecutive weeks
        var longestStreak = 1
        var tempStreak = 1

        for (i in 1 until meetingWeeks.size) {
            if (isConsecutiveWeek(meetingWeeks[i - 1], meetingWeeks[i])) {
                tempStreak++
            } else {
                longestStreak = maxOf(longestStreak, tempStreak)
                tempStreak = 1
            }
        }
        longestStreak = maxOf(longestStreak, tempStreak)

        // Current streak: count backwards from the current week
        val today = LocalDate.now()
        val currentWeek = weekKey(today)
        val lastMeetingWeek = meetingWeeks.last()

        val currentStreak = if (lastMeetingWeek == currentWeek || isConsecutiveWeek(lastMeetingWeek, currentWeek)) {
            // The streak is still active (meeting this week or last week)
            var streak = 1
            for (i in meetingWeeks.size - 2 downTo 0) {
                if (isConsecutiveWeek(meetingWeeks[i], meetingWeeks[i + 1])) {
                    streak++
                } else {
                    break
                }
            }
            streak
        } else {
            0
        }

        return StreakData(
            currentStreak = currentStreak,
            longestStreak = longestStreak,
            totalOneOnOnesHeld = totalOneOnOnesHeld
        )
    }

    internal fun computeAchievements(userId: UserId, streaks: StreakData): List<Achievement> {
        val achievements = mutableListOf<Achievement>()
        val today = LocalDate.now()

        val totalOneOnOnes = streaks.totalOneOnOnesHeld
        val totalActionItemsClosed = actionItemRepository.countByUserIdAndStatus(userId, ActionItemStatus.DONE)
        val totalPdpGoalsAchieved = pdpGoalRepository.countByUserIdAndStatus(userId, PdpGoalStatus.ACHIEVED)
        val totalKudosGiven = kudosRepository.countByUserId(userId)

        // 1:1 milestones
        if (totalOneOnOnes >= 1) {
            achievements.add(Achievement(AchievementType.FIRST_ONE_ON_ONE, today, "First 1:1", "Held your first 1:1 meeting"))
        }
        if (totalOneOnOnes >= 10) {
            achievements.add(Achievement(AchievementType.TEN_ONE_ON_ONES, today, "10 1:1s", "Held 10 one-on-one meetings"))
        }
        if (totalOneOnOnes >= 50) {
            achievements.add(Achievement(AchievementType.FIFTY_ONE_ON_ONES, today, "50 1:1s", "Held 50 one-on-one meetings"))
        }

        // Action item milestones
        if (totalActionItemsClosed >= 1) {
            achievements.add(Achievement(AchievementType.FIRST_ACTION_ITEM_CLOSED, today, "First Close", "Closed your first action item"))
        }
        if (totalActionItemsClosed >= 10) {
            achievements.add(Achievement(AchievementType.TEN_ACTION_ITEMS_CLOSED, today, "10 Closed", "Closed 10 action items"))
        }
        if (totalActionItemsClosed >= 50) {
            achievements.add(Achievement(AchievementType.FIFTY_ACTION_ITEMS_CLOSED, today, "50 Closed", "Closed 50 action items"))
        }
        if (totalActionItemsClosed >= 100) {
            achievements.add(Achievement(AchievementType.HUNDRED_ACTION_ITEMS_CLOSED, today, "100 Closed", "Closed 100 action items"))
        }

        // PDP milestones
        if (totalPdpGoalsAchieved >= 1) {
            achievements.add(Achievement(AchievementType.FIRST_PDP_GOAL_ACHIEVED, today, "First Goal", "Achieved your first PDP goal"))
        }
        if (totalPdpGoalsAchieved >= 5) {
            achievements.add(Achievement(AchievementType.FIVE_PDP_GOALS_ACHIEVED, today, "5 Goals", "Achieved 5 PDP goals"))
        }

        // Kudos milestones
        if (totalKudosGiven >= 1) {
            achievements.add(Achievement(AchievementType.FIRST_KUDOS_GIVEN, today, "First Kudos", "Gave your first kudos"))
        }
        if (totalKudosGiven >= 10) {
            achievements.add(Achievement(AchievementType.TEN_KUDOS_GIVEN, today, "10 Kudos", "Gave 10 kudos to your team"))
        }

        // Streak milestones
        if (streaks.longestStreak >= 7) {
            achievements.add(Achievement(AchievementType.STREAK_SEVEN, today, "7-Week Streak", "Maintained a 7-week 1:1 streak"))
        }
        if (streaks.longestStreak >= 30) {
            achievements.add(Achievement(AchievementType.STREAK_THIRTY, today, "30-Week Streak", "Maintained a 30-week 1:1 streak"))
        }

        return achievements
    }

    internal fun computeActivityHeatmap(userId: UserId, days: Int): List<ActivityDay> {
        val today = LocalDate.now()
        val startDate = today.minusDays(days.toLong())

        val allMeetingDates = oneOnOneEntryRepository.findAllMeetingDatesByUserId(userId)

        // Count activities per day within the window
        val activityMap = mutableMapOf<LocalDate, Int>()
        allMeetingDates
            .map { it.atZone(ZoneOffset.UTC).toLocalDate() }
            .filter { !it.isBefore(startDate) && !it.isAfter(today) }
            .forEach { date ->
                activityMap[date] = (activityMap[date] ?: 0) + 1
            }

        // Generate all days in the range with their counts
        return (0..days.toLong()).map { offset ->
            val date = startDate.plusDays(offset)
            ActivityDay(date = date, count = activityMap[date] ?: 0)
        }
    }

    internal fun computePdpProgress(userId: UserId): PdpProgressSummary {
        val active = pdpGoalRepository.countByUserIdAndStatus(userId, PdpGoalStatus.ACTIVE)
        val achieved = pdpGoalRepository.countByUserIdAndStatus(userId, PdpGoalStatus.ACHIEVED)
        val paused = pdpGoalRepository.countByUserIdAndStatus(userId, PdpGoalStatus.PAUSED)
        val dropped = pdpGoalRepository.countByUserIdAndStatus(userId, PdpGoalStatus.DROPPED)

        return PdpProgressSummary(
            totalActive = active.toInt(),
            totalAchieved = achieved.toInt(),
            totalPaused = paused.toInt(),
            totalDropped = dropped.toInt()
        )
    }

    /**
     * Returns a year-week key for grouping dates into weeks.
     */
    private fun weekKey(date: LocalDate): Long {
        // Use epoch day / 7 for consistent week boundaries
        return date.toEpochDay() / 7
    }

    /**
     * Checks if two week keys are consecutive.
     */
    private fun isConsecutiveWeek(week1: Long, week2: Long): Boolean {
        return week2 - week1 == 1L
    }
}
