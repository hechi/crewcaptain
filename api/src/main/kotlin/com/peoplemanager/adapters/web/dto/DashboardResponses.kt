package com.peoplemanager.adapters.web.dto

import com.peoplemanager.domain.ActionItemOwnerType
import com.peoplemanager.domain.CadenceType
import com.peoplemanager.domain.DashboardActionItem
import com.peoplemanager.domain.DashboardData
import com.peoplemanager.domain.StaleOneOnOneReminder
import com.peoplemanager.domain.UpcomingAnniversary
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class DashboardResponse(
    val overdueActionItems: List<DashboardActionItemResponse>,
    val dueSoonActionItems: List<DashboardActionItemResponse>,
    val staleOneOnOnes: List<StaleOneOnOneReminderResponse>,
    val upcomingAnniversaries: List<UpcomingAnniversaryResponse>
) {
    companion object {
        fun from(data: DashboardData): DashboardResponse = DashboardResponse(
            overdueActionItems = data.overdueActionItems.map { DashboardActionItemResponse.from(it) },
            dueSoonActionItems = data.dueSoonActionItems.map { DashboardActionItemResponse.from(it) },
            staleOneOnOnes = data.staleOneOnOnes.map { StaleOneOnOneReminderResponse.from(it) },
            upcomingAnniversaries = data.upcomingAnniversaries.map { UpcomingAnniversaryResponse.from(it) }
        )
    }
}

data class DashboardActionItemResponse(
    val id: UUID,
    val personId: UUID,
    val personName: String,
    val title: String,
    val dueDate: LocalDate,
    val ownerType: ActionItemOwnerType
) {
    companion object {
        fun from(item: DashboardActionItem): DashboardActionItemResponse = DashboardActionItemResponse(
            id = item.id.value,
            personId = item.personId.value,
            personName = item.personName,
            title = item.title,
            dueDate = item.dueDate,
            ownerType = item.ownerType
        )
    }
}

data class StaleOneOnOneReminderResponse(
    val personId: UUID,
    val personName: String,
    val cadenceType: CadenceType,
    val customIntervalDays: Int?,
    val lastMeetingDate: Instant?,
    val daysSinceLastMeeting: Long,
    val expectedIntervalDays: Int
) {
    companion object {
        fun from(reminder: StaleOneOnOneReminder): StaleOneOnOneReminderResponse = StaleOneOnOneReminderResponse(
            personId = reminder.personId.value,
            personName = reminder.personName,
            cadenceType = reminder.cadenceType,
            customIntervalDays = reminder.customIntervalDays,
            lastMeetingDate = reminder.lastMeetingDate,
            daysSinceLastMeeting = reminder.daysSinceLastMeeting,
            expectedIntervalDays = reminder.expectedIntervalDays
        )
    }
}

data class UpcomingAnniversaryResponse(
    val personId: UUID,
    val personName: String,
    val startDate: LocalDate,
    val anniversaryDate: LocalDate,
    val yearsCompleted: Int,
    val daysUntil: Long
) {
    companion object {
        fun from(anniversary: UpcomingAnniversary): UpcomingAnniversaryResponse = UpcomingAnniversaryResponse(
            personId = anniversary.personId.value,
            personName = anniversary.personName,
            startDate = anniversary.startDate,
            anniversaryDate = anniversary.anniversaryDate,
            yearsCompleted = anniversary.yearsCompleted,
            daysUntil = anniversary.daysUntil
        )
    }
}
