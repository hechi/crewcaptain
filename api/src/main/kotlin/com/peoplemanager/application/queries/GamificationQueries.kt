package com.peoplemanager.application.queries

import com.peoplemanager.domain.UserId

data class GetGamificationStatsQuery(
    val userId: UserId,
    val heatmapDays: Int = 90
)
