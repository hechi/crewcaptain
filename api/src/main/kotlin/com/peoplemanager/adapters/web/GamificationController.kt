package com.peoplemanager.adapters.web

import com.peoplemanager.adapters.auth.AuthenticatedUser
import com.peoplemanager.adapters.web.dto.GamificationStatsResponse
import com.peoplemanager.application.ports.GamificationQueryPort
import com.peoplemanager.application.queries.GetGamificationStatsQuery
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1")
class GamificationController(
    private val gamificationQueryPort: GamificationQueryPort
) {

    @GetMapping("/gamification/stats")
    fun getGamificationStats(
        @RequestParam(defaultValue = "90") heatmapDays: Int
    ): ResponseEntity<GamificationStatsResponse> {
        val userId = AuthenticatedUser.getUserId()
        val query = GetGamificationStatsQuery(
            userId = userId,
            heatmapDays = heatmapDays
        )
        val stats = gamificationQueryPort.getGamificationStats(query)
        return ResponseEntity.ok(GamificationStatsResponse.from(stats))
    }
}
