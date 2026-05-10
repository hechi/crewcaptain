package com.peoplemanager.domain

import java.time.LocalDate

/**
 * Read-only projection for gamification statistics.
 * Aggregates engagement data across all domain entities for a manager.
 */
data class GamificationStats(
    val streaks: StreakData,
    val achievements: List<Achievement>,
    val activityHeatmap: List<ActivityDay>,
    val pdpProgress: PdpProgressSummary
)

/**
 * Streak data for consecutive 1:1 cadence adherence.
 * A streak is maintained when the manager holds 1:1s within their configured cadence.
 */
data class StreakData(
    val currentStreak: Int,
    val longestStreak: Int,
    val totalOneOnOnesHeld: Int
)

/**
 * An achievement milestone the manager has reached.
 */
data class Achievement(
    val type: AchievementType,
    val unlockedAt: LocalDate,
    val label: String,
    val description: String
)

enum class AchievementType {
    FIRST_ONE_ON_ONE,
    TEN_ONE_ON_ONES,
    FIFTY_ONE_ON_ONES,
    FIRST_ACTION_ITEM_CLOSED,
    TEN_ACTION_ITEMS_CLOSED,
    FIFTY_ACTION_ITEMS_CLOSED,
    HUNDRED_ACTION_ITEMS_CLOSED,
    FIRST_PDP_GOAL_ACHIEVED,
    FIVE_PDP_GOALS_ACHIEVED,
    FIRST_KUDOS_GIVEN,
    TEN_KUDOS_GIVEN,
    STREAK_SEVEN,
    STREAK_THIRTY
}

/**
 * Activity count for a single day (for the heatmap).
 */
data class ActivityDay(
    val date: LocalDate,
    val count: Int
)

/**
 * Summary of PDP goal progress across all persons.
 */
data class PdpProgressSummary(
    val totalActive: Int,
    val totalAchieved: Int,
    val totalPaused: Int,
    val totalDropped: Int
) {
    /**
     * Completion percentage: achieved / (achieved + active + paused + dropped) * 100
     */
    val completionPercentage: Int
        get() {
            val total = totalActive + totalAchieved + totalPaused + totalDropped
            return if (total == 0) 0 else ((totalAchieved.toDouble() / total) * 100).toInt()
        }
}
