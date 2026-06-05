package com.peoplemanager.application.port.input

import com.peoplemanager.application.queries.GetGamificationStatsQuery
import com.peoplemanager.domain.GamificationStats

interface GamificationQueryPort {
    fun getGamificationStats(query: GetGamificationStatsQuery): GamificationStats
}
