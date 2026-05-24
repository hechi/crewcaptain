package com.peoplemanager.application

import com.peoplemanager.application.ports.ActionItemRepository
import com.peoplemanager.application.ports.KudosRepository
import com.peoplemanager.application.ports.OneOnOneEntryRepository
import com.peoplemanager.application.ports.PdpGoalRepository
import com.peoplemanager.application.ports.PdpUpdateRepository
import com.peoplemanager.application.ports.PersonExportPort
import com.peoplemanager.application.ports.PersonRepository
import com.peoplemanager.application.queries.ExportPersonDataQuery
import com.peoplemanager.domain.ActionItem
import com.peoplemanager.domain.Kudos
import com.peoplemanager.domain.MarkdownExportFormatter
import com.peoplemanager.domain.OneOnOneEntry
import com.peoplemanager.domain.PdpGoalWithUpdates
import com.peoplemanager.domain.PersonExportData
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.ZoneOffset

@Service
@Transactional(readOnly = true)
class PersonExportService(
    private val personRepository: PersonRepository,
    private val oneOnOneEntryRepository: OneOnOneEntryRepository,
    private val actionItemRepository: ActionItemRepository,
    private val pdpGoalRepository: PdpGoalRepository,
    private val pdpUpdateRepository: PdpUpdateRepository,
    private val kudosRepository: KudosRepository
) : PersonExportPort {

    companion object {
        private const val MAX_EXPORT_PAGE_SIZE = 1000
    }

    override fun exportPersonMarkdown(query: ExportPersonDataQuery): String {
        val person = personRepository.findByIdAndUserId(query.personId, query.userId)
            ?: throw PersonNotFoundException(query.personId)

        val entries = fetchOneOnOneEntries(query)
        val actionItems = fetchActionItems(query)
        val pdpGoals = fetchPdpGoals(query)
        val kudos = fetchKudos(query)

        val exportData = PersonExportData(
            person = person,
            oneOnOneEntries = entries,
            actionItems = actionItems,
            pdpGoals = pdpGoals,
            kudos = kudos,
            dateFrom = query.dateFrom,
            dateTo = query.dateTo
        )

        return MarkdownExportFormatter.format(exportData)
    }

    private fun fetchOneOnOneEntries(query: ExportPersonDataQuery): List<OneOnOneEntry> {
        val pageable = PageRequest.of(0, MAX_EXPORT_PAGE_SIZE, Sort.by(Sort.Direction.DESC, "meetingDate"))
        val entries = oneOnOneEntryRepository.findAllByUserIdAndPersonId(
            query.userId, query.personId, pageable
        ).content

        return filterByDateRange(entries, query.dateFrom, query.dateTo) { entry ->
            entry.meetingDate.atZone(ZoneOffset.UTC).toLocalDate()
        }
    }

    private fun fetchActionItems(query: ExportPersonDataQuery): List<ActionItem> {
        val pageable = PageRequest.of(0, MAX_EXPORT_PAGE_SIZE, Sort.by(Sort.Direction.DESC, "createdAt"))
        val items = actionItemRepository.findAllByUserIdAndPersonId(
            query.userId, query.personId, null, pageable
        ).content

        return if (query.dateFrom != null || query.dateTo != null) {
            filterByDateRange(items, query.dateFrom, query.dateTo) { item ->
                item.createdAt.atZone(ZoneOffset.UTC).toLocalDate()
            }
        } else {
            items
        }
    }

    private fun fetchPdpGoals(query: ExportPersonDataQuery): List<PdpGoalWithUpdates> {
        val pageable = PageRequest.of(0, MAX_EXPORT_PAGE_SIZE, Sort.by(Sort.Direction.DESC, "createdAt"))
        val goals = pdpGoalRepository.findAllByUserIdAndPersonId(
            query.userId, query.personId, null, pageable
        ).content

        val filteredGoals = if (query.dateFrom != null || query.dateTo != null) {
            filterByDateRange(goals, query.dateFrom, query.dateTo) { goal ->
                goal.createdAt.atZone(ZoneOffset.UTC).toLocalDate()
            }
        } else {
            goals
        }

        return filteredGoals.map { goal ->
            val updatePageable = PageRequest.of(0, MAX_EXPORT_PAGE_SIZE, Sort.by(Sort.Direction.DESC, "createdAt"))
            val updates = pdpUpdateRepository.findAllByGoalIdAndUserId(
                goal.id, query.userId, updatePageable
            ).content
            PdpGoalWithUpdates(goal = goal, updates = updates)
        }
    }

    private fun fetchKudos(query: ExportPersonDataQuery): List<Kudos> {
        val pageable = PageRequest.of(0, MAX_EXPORT_PAGE_SIZE, Sort.by(Sort.Direction.DESC, "date"))
        val kudos = kudosRepository.findAllByUserIdAndPersonId(
            query.userId, query.personId, pageable
        ).content

        return if (query.dateFrom != null || query.dateTo != null) {
            filterByDateRange(kudos, query.dateFrom, query.dateTo) { k ->
                k.date
            }
        } else {
            kudos
        }
    }

    private fun <T> filterByDateRange(
        items: List<T>,
        dateFrom: LocalDate?,
        dateTo: LocalDate?,
        dateExtractor: (T) -> LocalDate
    ): List<T> {
        return items.filter { item ->
            val itemDate = dateExtractor(item)
            val afterFrom = dateFrom == null || !itemDate.isBefore(dateFrom)
            val beforeTo = dateTo == null || !itemDate.isAfter(dateTo)
            afterFrom && beforeTo
        }
    }
}
