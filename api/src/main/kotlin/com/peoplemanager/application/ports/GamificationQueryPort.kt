package com.peoplemanager.application.ports

import com.peoplemanager.application.queries.GetGamificationStatsQuery
import com.peoplemanager.domain.GamificationStats

interface GamificationQueryPort {
    fun getGamificationStats(query: GetGamificationStatsQuery): GamificationStats
}
