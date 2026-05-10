package com.peoplemanager.application.ports

import com.peoplemanager.application.queries.GetDashboardQuery
import com.peoplemanager.domain.DashboardData

interface DashboardQueryPort {
    fun getDashboard(query: GetDashboardQuery): DashboardData
}
