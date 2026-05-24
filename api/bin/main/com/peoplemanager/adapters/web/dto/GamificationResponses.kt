package com.peoplemanager.adapters.web.dto

import com.peoplemanager.domain.Achievement
import com.peoplemanager.domain.AchievementType
import com.peoplemanager.domain.ActivityDay
import com.peoplemanager.domain.GamificationStats
import com.peoplemanager.domain.PdpProgressSummary
import com.peoplemanager.domain.StreakData
import java.time.LocalDate

data class GamificationStatsResponse(
    val streaks: StreakDataResponse,
    val achievements: List<AchievementResponse>,
    val activityHeatmap: List<ActivityDayResponse>,
    val pdpProgress: PdpProgressResponse
) {
    companion object {
        fun from(stats: GamificationStats): GamificationStatsResponse = GamificationStatsResponse(
            streaks = StreakDataResponse.from(stats.streaks),
            achievements = stats.achievements.map { AchievementResponse.from(it) },
            activityHeatmap = stats.activityHeatmap.map { ActivityDayResponse.from(it) },
            pdpProgress = PdpProgressResponse.from(stats.pdpProgress)
        )
    }
}

data class StreakDataResponse(
    val currentStreak: Int,
    val longestStreak: Int,
    val totalOneOnOnesHeld: Int
) {
    companion object {
        fun from(data: StreakData): StreakDataResponse = StreakDataResponse(
            currentStreak = data.currentStreak,
            longestStreak = data.longestStreak,
            totalOneOnOnesHeld = data.totalOneOnOnesHeld
        )
    }
}

data class AchievementResponse(
    val type: AchievementType,
    val unlocked: Boolean,
    val label: String,
    val description: String,
    val current: Int,
    val target: Int
) {
    companion object {
        fun from(achievement: Achievement): AchievementResponse = AchievementResponse(
            type = achievement.type,
            unlocked = achievement.unlocked,
            label = achievement.label,
            description = achievement.description,
            current = achievement.current,
            target = achievement.target
        )
    }
}

data class ActivityDayResponse(
    val date: LocalDate,
    val count: Int
) {
    companion object {
        fun from(day: ActivityDay): ActivityDayResponse = ActivityDayResponse(
            date = day.date,
            count = day.count
        )
    }
}

data class PdpProgressResponse(
    val totalActive: Int,
    val totalAchieved: Int,
    val totalPaused: Int,
    val totalDropped: Int,
    val completionPercentage: Int
) {
    companion object {
        fun from(summary: PdpProgressSummary): PdpProgressResponse = PdpProgressResponse(
            totalActive = summary.totalActive,
            totalAchieved = summary.totalAchieved,
            totalPaused = summary.totalPaused,
            totalDropped = summary.totalDropped,
            completionPercentage = summary.completionPercentage
        )
    }
}
