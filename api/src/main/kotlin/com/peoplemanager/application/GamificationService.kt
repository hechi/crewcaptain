package com.peoplemanager.application

import com.peoplemanager.application.port.output.ActionItemRepository
import com.peoplemanager.application.port.input.GamificationQueryPort
import com.peoplemanager.application.port.output.KudosRepository
import com.peoplemanager.application.port.output.OneOnOneEntryRepository
import com.peoplemanager.application.port.output.OneOnOneSeriesRepository
import com.peoplemanager.application.port.output.PdpGoalRepository
import com.peoplemanager.application.queries.GetGamificationStatsQuery
import com.peoplemanager.domain.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.time.temporal.IsoFields

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
        val totalOneOnOnes = streaks.totalOneOnOnesHeld
        val totalActionItemsClosed = actionItemRepository.countByUserIdAndStatus(userId, ActionItemStatus.DONE).toInt()
        val totalPdpGoalsAchieved = pdpGoalRepository.countByUserIdAndStatus(userId, PdpGoalStatus.ACHIEVED).toInt()
        val totalKudosGiven = kudosRepository.countByUserId(userId).toInt()
        val longestStreak = streaks.longestStreak

        return listOf(
            // 1:1 milestones
            Achievement(AchievementType.FIRST_ONE_ON_ONE, totalOneOnOnes >= 1, "First 1:1", "Hold your first 1:1 meeting", totalOneOnOnes, 1),
            Achievement(AchievementType.TEN_ONE_ON_ONES, totalOneOnOnes >= 10, "10 1:1s", "Hold 10 one-on-one meetings", totalOneOnOnes, 10),
            Achievement(AchievementType.FIFTY_ONE_ON_ONES, totalOneOnOnes >= 50, "50 1:1s", "Hold 50 one-on-one meetings", totalOneOnOnes, 50),

            // Action item milestones
            Achievement(AchievementType.FIRST_ACTION_ITEM_CLOSED, totalActionItemsClosed >= 1, "First Close", "Close your first action item", totalActionItemsClosed, 1),
            Achievement(AchievementType.TEN_ACTION_ITEMS_CLOSED, totalActionItemsClosed >= 10, "10 Closed", "Close 10 action items", totalActionItemsClosed, 10),
            Achievement(AchievementType.FIFTY_ACTION_ITEMS_CLOSED, totalActionItemsClosed >= 50, "50 Closed", "Close 50 action items", totalActionItemsClosed, 50),
            Achievement(AchievementType.HUNDRED_ACTION_ITEMS_CLOSED, totalActionItemsClosed >= 100, "100 Closed", "Close 100 action items", totalActionItemsClosed, 100),

            // PDP milestones
            Achievement(AchievementType.FIRST_PDP_GOAL_ACHIEVED, totalPdpGoalsAchieved >= 1, "First Goal", "Achieve your first PDP goal", totalPdpGoalsAchieved, 1),
            Achievement(AchievementType.FIVE_PDP_GOALS_ACHIEVED, totalPdpGoalsAchieved >= 5, "5 Goals", "Achieve 5 PDP goals", totalPdpGoalsAchieved, 5),

            // Kudos milestones
            Achievement(AchievementType.FIRST_KUDOS_GIVEN, totalKudosGiven >= 1, "First Kudos", "Give your first kudos", totalKudosGiven, 1),
            Achievement(AchievementType.TEN_KUDOS_GIVEN, totalKudosGiven >= 10, "10 Kudos", "Give 10 kudos to your team", totalKudosGiven, 10),

            // Streak milestones
            Achievement(AchievementType.STREAK_SEVEN, longestStreak >= 7, "7-Week Streak", "Maintain a 7-week 1:1 streak", longestStreak, 7),
            Achievement(AchievementType.STREAK_THIRTY, longestStreak >= 30, "30-Week Streak", "Maintain a 30-week 1:1 streak", longestStreak, 30),
        )
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
     * Returns a year-week key for grouping dates into ISO weeks (Monday-Sunday).
     * Encodes year and week into a single Long for easy comparison.
     */
    private fun weekKey(date: LocalDate): Long {
        val weekYear = date.get(IsoFields.WEEK_BASED_YEAR).toLong()
        val weekOfYear = date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR).toLong()
        return weekYear * 100 + weekOfYear
    }

    /**
     * Checks if two week keys are consecutive ISO weeks.
     */
    private fun isConsecutiveWeek(week1: Long, week2: Long): Boolean {
        val year1 = week1 / 100
        val w1 = week1 % 100
        val year2 = week2 / 100
        val w2 = week2 % 100

        // Same year, consecutive weeks
        if (year1 == year2 && w2 - w1 == 1L) return true

        // Year boundary: last week of year1 → first week of year2
        if (year2 == year1 + 1 && w2 == 1L) {
            // ISO years can have 52 or 53 weeks
            val maxWeeksInYear1 = LocalDate.of(year1.toInt(), 12, 28)
                .get(IsoFields.WEEK_OF_WEEK_BASED_YEAR).toLong()
            if (w1 == maxWeeksInYear1) return true
        }

        return false
    }
}
