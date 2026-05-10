package com.peoplemanager.application.queries

import com.peoplemanager.domain.UserId

data class GetDashboardQuery(
    val userId: UserId,
    val dueSoonDays: Int = 3,
    val anniversaryLookaheadDays: Int = 30
)
