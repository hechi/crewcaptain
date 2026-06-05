package com.peoplemanager.application.port.input

import com.peoplemanager.application.queries.GetDashboardQuery
import com.peoplemanager.domain.DashboardData

interface DashboardQueryPort {
    fun getDashboard(query: GetDashboardQuery): DashboardData
}
