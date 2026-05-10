package com.peoplemanager.adapters.web

import com.peoplemanager.adapters.auth.AuthenticatedUser
import com.peoplemanager.adapters.web.dto.DashboardResponse
import com.peoplemanager.application.ports.DashboardQueryPort
import com.peoplemanager.application.queries.GetDashboardQuery
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1")
class DashboardController(
    private val dashboardQueryPort: DashboardQueryPort
) {

    @GetMapping("/dashboard")
    fun getDashboard(
        @RequestParam(defaultValue = "3") dueSoonDays: Int,
        @RequestParam(defaultValue = "30") anniversaryLookaheadDays: Int
    ): ResponseEntity<DashboardResponse> {
        val userId = AuthenticatedUser.getUserId()
        val query = GetDashboardQuery(
            userId = userId,
            dueSoonDays = dueSoonDays,
            anniversaryLookaheadDays = anniversaryLookaheadDays
        )
        val dashboardData = dashboardQueryPort.getDashboard(query)
        return ResponseEntity.ok(DashboardResponse.from(dashboardData))
    }
}
