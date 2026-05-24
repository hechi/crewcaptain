package com.peoplemanager.application

import com.peoplemanager.application.ports.ActionItemRepository
import com.peoplemanager.application.ports.KudosRepository
import com.peoplemanager.application.ports.OneOnOneEntryRepository
import com.peoplemanager.application.ports.PdpGoalRepository
import com.peoplemanager.application.ports.PdpUpdateRepository
import com.peoplemanager.application.ports.PersonRepository
import com.peoplemanager.application.ports.ReviewPacketPort
import com.peoplemanager.application.queries.GenerateReviewPacketQuery
import com.peoplemanager.domain.ActionItem
import com.peoplemanager.domain.Kudos
import com.peoplemanager.domain.OneOnOneEntry
import com.peoplemanager.domain.PdpGoalWithUpdates
import com.peoplemanager.domain.ReviewPacketData
import com.peoplemanager.domain.ReviewPacketFormatter
import com.peoplemanager.domain.ReviewPacketSummary
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.ZoneOffset

@Service
@Transactional(readOnly = true)
class ReviewPacketService(
    private val personRepository: PersonRepository,
    private val oneOnOneEntryRepository: OneOnOneEntryRepository,
    private val actionItemRepository: ActionItemRepository,
    private val pdpGoalRepository: PdpGoalRepository,
    private val pdpUpdateRepository: PdpUpdateRepository,
    private val kudosRepository: KudosRepository
) : ReviewPacketPort {

    companion object {
        private const val MAX_PAGE_SIZE = 1000
    }

    override fun generateReviewPacket(query: GenerateReviewPacketQuery): String {
        val person = personRepository.findByIdAndUserId(query.personId, query.userId)
            ?: throw PersonNotFoundException(query.personId)

        val entries = fetchOneOnOneEntries(query)
        val actionItems = fetchActionItems(query)
        val pdpGoals = fetchPdpGoals(query)
        val kudos = fetchKudos(query)

        val summary = ReviewPacketSummary.compute(entries, actionItems, pdpGoals, kudos)

        val packetData = ReviewPacketData(
            person = person,
            dateFrom = query.dateFrom,
            dateTo = query.dateTo,
            oneOnOneEntries = entries,
            actionItems = actionItems,
            pdpGoals = pdpGoals,
            kudos = kudos,
            summary = summary
        )

        return ReviewPacketFormatter.format(packetData)
    }

    private fun fetchOneOnOneEntries(query: GenerateReviewPacketQuery): List<OneOnOneEntry> {
        val pageable = PageRequest.of(0, MAX_PAGE_SIZE, Sort.by(Sort.Direction.DESC, "meetingDate"))
        val entries = oneOnOneEntryRepository.findAllByUserIdAndPersonId(
            query.userId, query.personId, pageable
        ).content

        return filterByDateRange(entries, query.dateFrom, query.dateTo) { entry ->
            entry.meetingDate.atZone(ZoneOffset.UTC).toLocalDate()
        }
    }

    private fun fetchActionItems(query: GenerateReviewPacketQuery): List<ActionItem> {
        val pageable = PageRequest.of(0, MAX_PAGE_SIZE, Sort.by(Sort.Direction.DESC, "createdAt"))
        val items = actionItemRepository.findAllByUserIdAndPersonId(
            query.userId, query.personId, null, pageable
        ).content

        return filterByDateRange(items, query.dateFrom, query.dateTo) { item ->
            item.createdAt.atZone(ZoneOffset.UTC).toLocalDate()
        }
    }

    private fun fetchPdpGoals(query: GenerateReviewPacketQuery): List<PdpGoalWithUpdates> {
        val pageable = PageRequest.of(0, MAX_PAGE_SIZE, Sort.by(Sort.Direction.DESC, "createdAt"))
        val goals = pdpGoalRepository.findAllByUserIdAndPersonId(
            query.userId, query.personId, null, pageable
        ).content

        val filteredGoals = filterByDateRange(goals, query.dateFrom, query.dateTo) { goal ->
            goal.createdAt.atZone(ZoneOffset.UTC).toLocalDate()
        }

        return filteredGoals.map { goal ->
            val updatePageable = PageRequest.of(0, MAX_PAGE_SIZE, Sort.by(Sort.Direction.DESC, "createdAt"))
            val updates = pdpUpdateRepository.findAllByGoalIdAndUserId(
                goal.id, query.userId, updatePageable
            ).content
            // Filter updates to the date range as well
            val filteredUpdates = filterByDateRange(updates, query.dateFrom, query.dateTo) { update ->
                update.createdAt.atZone(ZoneOffset.UTC).toLocalDate()
            }
            PdpGoalWithUpdates(goal = goal, updates = filteredUpdates)
        }
    }

    private fun fetchKudos(query: GenerateReviewPacketQuery): List<Kudos> {
        val pageable = PageRequest.of(0, MAX_PAGE_SIZE, Sort.by(Sort.Direction.DESC, "date"))
        val kudos = kudosRepository.findAllByUserIdAndPersonId(
            query.userId, query.personId, pageable
        ).content

        return filterByDateRange(kudos, query.dateFrom, query.dateTo) { k ->
            k.date
        }
    }

    private fun <T> filterByDateRange(
        items: List<T>,
        dateFrom: LocalDate,
        dateTo: LocalDate,
        dateExtractor: (T) -> LocalDate
    ): List<T> {
        return items.filter { item ->
            val itemDate = dateExtractor(item)
            !itemDate.isBefore(dateFrom) && !itemDate.isAfter(dateTo)
        }
    }
}
