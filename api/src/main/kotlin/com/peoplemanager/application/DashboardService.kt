package com.peoplemanager.application

import com.peoplemanager.application.port.output.ActionItemRepository
import com.peoplemanager.application.port.input.DashboardQueryPort
import com.peoplemanager.application.port.output.OneOnOneEntryRepository
import com.peoplemanager.application.port.output.OneOnOneSeriesRepository
import com.peoplemanager.application.port.output.PersonRepository
import com.peoplemanager.application.queries.GetDashboardQuery
import com.peoplemanager.domain.CadenceType
import com.peoplemanager.domain.DashboardActionItem
import com.peoplemanager.domain.DashboardData
import com.peoplemanager.domain.StaleOneOnOneReminder
import com.peoplemanager.domain.UpcomingAnniversary
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

@Service
@Transactional(readOnly = true)
class DashboardService(
    private val personRepository: PersonRepository,
    private val actionItemRepository: ActionItemRepository,
    private val oneOnOneSeriesRepository: OneOnOneSeriesRepository,
    private val oneOnOneEntryRepository: OneOnOneEntryRepository
) : DashboardQueryPort {

    override fun getDashboard(query: GetDashboardQuery): DashboardData {
        val today = LocalDate.now(ZoneOffset.UTC)
        val persons = personRepository.findAllByUserIdUnpaged(query.userId)
        val personMap = persons.associateBy { it.id }

        // Overdue action items (status=OPEN, dueDate < today)
        val overduePage = actionItemRepository.findOverdueByUserId(
            query.userId, today, PageRequest.of(0, 10)
        )
        val overdueItems = overduePage.content.map { item ->
            DashboardActionItem(
                id = item.id,
                personId = item.personId,
                personName = personMap[item.personId]?.name ?: "Unknown",
                title = item.title,
                dueDate = item.dueDate!!,
                ownerType = item.ownerType
            )
        }

        // Due soon action items (status=OPEN, dueDate between today and today+dueSoonDays)
        val dueSoonDate = today.plusDays(query.dueSoonDays.toLong())
        val dueSoonItems = actionItemRepository.findDueSoonByUserId(
            query.userId, today, dueSoonDate
        ).map { item ->
            DashboardActionItem(
                id = item.id,
                personId = item.personId,
                personName = personMap[item.personId]?.name ?: "Unknown",
                title = item.title,
                dueDate = item.dueDate!!,
                ownerType = item.ownerType
            )
        }

        // Stale 1:1 reminders
        val allSeries = oneOnOneSeriesRepository.findAllByUserId(query.userId)
        val staleReminders = allSeries.mapNotNull { series ->
            val person = personMap[series.personId] ?: return@mapNotNull null
            val lastMeetingDate = oneOnOneEntryRepository.findLatestMeetingDate(query.userId, series.personId)
            val expectedIntervalDays = cadenceToIntervalDays(series.cadenceType, series.customIntervalDays)

            val daysSinceLastMeeting = if (lastMeetingDate != null) {
                ChronoUnit.DAYS.between(
                    lastMeetingDate.atZone(ZoneOffset.UTC).toLocalDate(),
                    today
                )
            } else {
                // If no meeting ever, consider it stale from person creation
                ChronoUnit.DAYS.between(
                    series.createdAt.atZone(ZoneOffset.UTC).toLocalDate(),
                    today
                )
            }

            if (daysSinceLastMeeting > expectedIntervalDays) {
                StaleOneOnOneReminder(
                    personId = series.personId,
                    personName = person.name,
                    cadenceType = series.cadenceType,
                    customIntervalDays = series.customIntervalDays,
                    lastMeetingDate = lastMeetingDate,
                    daysSinceLastMeeting = daysSinceLastMeeting,
                    expectedIntervalDays = expectedIntervalDays
                )
            } else {
                null
            }
        }.sortedByDescending { it.daysSinceLastMeeting }

        // Upcoming anniversaries
        val upcomingAnniversaries = persons
            .filter { it.startDate != null }
            .mapNotNull { person ->
                val startDate = person.startDate!!
                val thisYearAnniversary = startDate.withYear(today.year)
                val anniversaryDate = if (thisYearAnniversary.isBefore(today)) {
                    startDate.withYear(today.year + 1)
                } else {
                    thisYearAnniversary
                }
                val daysUntil = ChronoUnit.DAYS.between(today, anniversaryDate)
                if (daysUntil <= query.anniversaryLookaheadDays) {
                    val yearsCompleted = anniversaryDate.year - startDate.year
                    UpcomingAnniversary(
                        personId = person.id,
                        personName = person.name,
                        startDate = startDate,
                        anniversaryDate = anniversaryDate,
                        yearsCompleted = yearsCompleted,
                        daysUntil = daysUntil
                    )
                } else {
                    null
                }
            }
            .sortedBy { it.daysUntil }

        return DashboardData(
            overdueActionItems = overdueItems,
            dueSoonActionItems = dueSoonItems,
            staleOneOnOnes = staleReminders,
            upcomingAnniversaries = upcomingAnniversaries
        )
    }

    private fun cadenceToIntervalDays(cadenceType: CadenceType, customIntervalDays: Int?): Int {
        return when (cadenceType) {
            CadenceType.WEEKLY -> 7
            CadenceType.BIWEEKLY -> 14
            CadenceType.MONTHLY -> 30
            CadenceType.CUSTOM -> customIntervalDays ?: 14
        }
    }
}
