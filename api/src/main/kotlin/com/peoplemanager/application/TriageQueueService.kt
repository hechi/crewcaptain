package com.peoplemanager.application

import com.peoplemanager.application.commands.SnoozeActionItemCommand
import com.peoplemanager.application.port.input.TriageCommandPort
import com.peoplemanager.application.port.input.TriageQueryPort
import com.peoplemanager.application.port.output.ActionItemRepository
import com.peoplemanager.application.port.output.OneOnOneEntryRepository
import com.peoplemanager.application.port.output.OneOnOneSeriesRepository
import com.peoplemanager.application.port.output.PersonRepository
import com.peoplemanager.application.queries.GetTriageQueueQuery
import com.peoplemanager.application.queries.OwnerScope
import com.peoplemanager.domain.*
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

@Service
@Transactional(readOnly = true)
class TriageQueueService(
    private val personRepository: PersonRepository,
    private val actionItemRepository: ActionItemRepository,
    private val oneOnOneSeriesRepository: OneOnOneSeriesRepository,
    private val oneOnOneEntryRepository: OneOnOneEntryRepository
) : TriageQueryPort, TriageCommandPort {

    override fun getTriageQueue(query: GetTriageQueueQuery): List<TriageItem> {
        val today = LocalDate.now(ZoneOffset.UTC)
        val now = Instant.now()
        val persons = personRepository.findAllByUserIdUnpaged(query.userId)
        val personMap = persons.associateBy { it.id }

        val items = mutableListOf<TriageItem>()

        // Filter by workspace / person if requested
        val filteredPersons = persons.filter { person ->
            val matchesWorkspace = query.workspaceIds.isNullOrEmpty() ||
                person.workspaceId in query.workspaceIds
            val matchesPerson = query.personId == null || person.id == query.personId
            matchesWorkspace && matchesPerson
        }
        val filteredPersonIds = filteredPersons.map { it.id }.toSet()

        // 1. Overdue Action Items
        if (query.itemType == null || query.itemType == TriageItemType.ACTION_ITEM_OVERDUE) {
            val overduePage = actionItemRepository.findOverdueByUserId(
                query.userId, today, PageRequest.of(0, 100)
            )
            overduePage.content
                .filter { it.personId in filteredPersonIds }
                .filter { !isSnoozed(it, now) }
                .filter { matchesOwnerScope(it, query.ownerScope) }
                .forEach { item ->
                    val person = personMap[item.personId]
                    items.add(
                        TriageItem(
                            id = "ai-${item.id.value}",
                            type = TriageItemType.ACTION_ITEM_OVERDUE,
                            criticality = TriageCriticality.OVERDUE,
                            title = item.title,
                            personId = item.personId,
                            personName = person?.name ?: "Unknown",
                            workspaceId = person?.workspaceId,
                            workspaceName = null, // Resolved in controller DTO if needed
                            sensitive = false,
                            dueDate = item.dueDate,
                            daysOverdue = ChronoUnit.DAYS.between(item.dueDate, today),
                            ownerType = item.ownerType,
                            sourceActionItemId = item.id,
                            snoozedUntil = item.snoozedUntil,
                            createdAt = item.createdAt
                        )
                    )
                }
        }

        // 2. Due Soon Action Items
        if (query.itemType == null || query.itemType == TriageItemType.ACTION_ITEM_DUE_SOON) {
            val dueSoonDate = today.plusDays(7)
            val dueSoonItems = actionItemRepository.findDueSoonByUserId(
                query.userId, today, dueSoonDate
            )
            dueSoonItems
                .filter { it.personId in filteredPersonIds }
                .filter { !isSnoozed(it, now) }
                .filter { matchesOwnerScope(it, query.ownerScope) }
                .forEach { item ->
                    val person = personMap[item.personId]
                    items.add(
                        TriageItem(
                            id = "ai-ds-${item.id.value}",
                            type = TriageItemType.ACTION_ITEM_DUE_SOON,
                            criticality = TriageCriticality.DUE_SOON,
                            title = item.title,
                            personId = item.personId,
                            personName = person?.name ?: "Unknown",
                            workspaceId = person?.workspaceId,
                            sensitive = false,
                            dueDate = item.dueDate,
                            daysUntilDue = ChronoUnit.DAYS.between(today, item.dueDate),
                            ownerType = item.ownerType,
                            sourceActionItemId = item.id,
                            snoozedUntil = item.snoozedUntil,
                            createdAt = item.createdAt
                        )
                    )
                }
        }

        // 3. Stale 1:1s
        if (query.itemType == null || query.itemType == TriageItemType.STALE_ONE_ON_ONE) {
            val allSeries = oneOnOneSeriesRepository.findAllByUserId(query.userId)
            allSeries
                .filter { it.personId in filteredPersonIds }
                .forEach { series ->
                    val person = personMap[series.personId] ?: return@forEach
                    val lastMeetingDate = oneOnOneEntryRepository.findLatestMeetingDate(
                        query.userId, series.personId
                    )
                    val expectedIntervalDays = cadenceToIntervalDays(series.cadenceType, series.customIntervalDays)
                    val daysSince = if (lastMeetingDate != null) {
                        ChronoUnit.DAYS.between(
                            lastMeetingDate.atZone(ZoneOffset.UTC).toLocalDate(), today
                        )
                    } else {
                        ChronoUnit.DAYS.between(
                            series.createdAt.atZone(ZoneOffset.UTC).toLocalDate(), today
                        )
                    }

                    if (daysSince > expectedIntervalDays) {
                        items.add(
                            TriageItem(
                                id = "stale-${series.personId.value}",
                                type = TriageItemType.STALE_ONE_ON_ONE,
                                criticality = TriageCriticality.STALE,
                                title = "1:1 overdue by ${daysSince - expectedIntervalDays}d",
                                personId = series.personId,
                                personName = person.name,
                                workspaceId = person.workspaceId,
                                sensitive = false,
                                daysOverdue = daysSince - expectedIntervalDays,
                                createdAt = series.createdAt
                            )
                        )
                    }
                }
        }

        // 4. Upcoming Anniversaries
        if (query.itemType == null || query.itemType == TriageItemType.UPCOMING_ANNIVERSARY) {
            filteredPersons
                .filter { it.startDate != null }
                .forEach { person ->
                    val startDate = person.startDate!!
                    val thisYearAnniversary = startDate.withYear(today.year)
                    val anniversaryDate = if (thisYearAnniversary.isBefore(today)) {
                        startDate.withYear(today.year + 1)
                    } else {
                        thisYearAnniversary
                    }
                    val daysUntil = ChronoUnit.DAYS.between(today, anniversaryDate)
                    if (daysUntil in 0..30) {
                        val yearsCompleted = anniversaryDate.year - startDate.year
                        items.add(
                            TriageItem(
                                id = "anniv-${person.id.value}",
                                type = TriageItemType.UPCOMING_ANNIVERSARY,
                                criticality = TriageCriticality.INFORMATIONAL,
                                title = "${yearsCompleted}-year anniversary in ${daysUntil}d",
                                personId = person.id,
                                personName = person.name,
                                workspaceId = person.workspaceId,
                                sensitive = false,
                                daysUntilDue = daysUntil,
                                createdAt = person.createdAt
                            )
                        )
                    }
                }
        }

        // Sort: Primary by criticality, Secondary by chronological urgency
        return items.sortedWith(
            compareBy<TriageItem> { it.criticality.sortOrder }
                .thenBy { it.dueDate ?: LocalDate.MAX }
                .thenByDescending { it.daysOverdue ?: 0 }
        )
    }

    @Transactional
    override fun snoozeActionItem(command: SnoozeActionItemCommand): ActionItem {
        val existing = actionItemRepository.findByIdAndUserIdAndPersonId(
            command.actionItemId, command.userId, command.personId
        ) ?: throw ActionItemNotFoundException(command.actionItemId)

        val snoozed = existing.snooze(command.snoozedUntil)
        return actionItemRepository.save(snoozed)
    }

    private fun isSnoozed(item: ActionItem, now: Instant): Boolean {
        return item.snoozedUntil != null && item.snoozedUntil.isAfter(now)
    }

    private fun matchesOwnerScope(item: ActionItem, scope: OwnerScope): Boolean {
        return when (scope) {
            OwnerScope.ALL -> true
            OwnerScope.MINE -> item.ownerType == ActionItemOwnerType.MANAGER
        }
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
