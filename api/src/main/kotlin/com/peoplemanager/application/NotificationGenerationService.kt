package com.peoplemanager.application

import com.peoplemanager.application.ports.ActionItemRepository
import com.peoplemanager.application.ports.NotificationRepository
import com.peoplemanager.application.ports.OneOnOneEntryRepository
import com.peoplemanager.application.ports.OneOnOneSeriesRepository
import com.peoplemanager.application.ports.PersonRepository
import com.peoplemanager.application.ports.UserRepository
import com.peoplemanager.application.ports.UserSettingsRepository
import com.peoplemanager.domain.*
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

/**
 * Generates notifications for all users based on current data state.
 * Called by the scheduler adapter on a periodic basis.
 *
 * Deduplication: notifications are not generated if an identical notification
 * (same type + referenceId) was already created within the deduplication window.
 */
@Service
@Transactional
class NotificationGenerationService(
    private val userRepository: UserRepository,
    private val personRepository: PersonRepository,
    private val actionItemRepository: ActionItemRepository,
    private val oneOnOneSeriesRepository: OneOnOneSeriesRepository,
    private val oneOnOneEntryRepository: OneOnOneEntryRepository,
    private val notificationRepository: NotificationRepository,
    private val userSettingsRepository: UserSettingsRepository
) {

    companion object {
        /** Default deduplication window — don't re-notify for the same item within 24 hours */
        val DEDUPLICATION_WINDOW: java.time.Duration = java.time.Duration.ofHours(24)
        const val DEFAULT_DUE_SOON_DAYS = 3
        const val DEFAULT_ANNIVERSARY_LOOKAHEAD_DAYS = 7
    }

    /**
     * Generate notifications for a single user. Returns the count of new notifications created.
     * Respects user notification preferences — disabled notification types are skipped.
     */
    fun generateForUser(userId: UserId, dueSoonDays: Int = DEFAULT_DUE_SOON_DAYS, anniversaryLookaheadDays: Int = DEFAULT_ANNIVERSARY_LOOKAHEAD_DAYS): Int {
        val settings = userSettingsRepository.findByUserId(userId)
        val effectiveDueSoonDays = settings?.dueSoonDays ?: dueSoonDays
        val effectiveAnniversaryDays = settings?.anniversaryLookaheadDays ?: anniversaryLookaheadDays

        val today = LocalDate.now(ZoneOffset.UTC)
        val deduplicationCutoff = Instant.now().minus(DEDUPLICATION_WINDOW)
        val persons = personRepository.findAllByUserIdUnpaged(userId)
        val personMap = persons.associateBy { it.id }

        val notifications = mutableListOf<Notification>()

        // 1. Overdue action items (if enabled)
        if (settings?.notifyActionItemOverdue != false) {
            notifications.addAll(generateOverdueActionItemNotifications(userId, today, personMap, deduplicationCutoff))
        }

        // 2. Due soon action items (if enabled)
        if (settings?.notifyActionItemDueSoon != false) {
            notifications.addAll(generateDueSoonActionItemNotifications(userId, today, effectiveDueSoonDays, personMap, deduplicationCutoff))
        }

        // 3. Stale 1:1 reminders (if enabled)
        if (settings?.notifyStaleOneOnOne != false) {
            notifications.addAll(generateStaleOneOnOneNotifications(userId, today, personMap, deduplicationCutoff))
        }

        // 4. Upcoming anniversaries (if enabled)
        if (settings?.notifyUpcomingAnniversary != false) {
            notifications.addAll(generateAnniversaryNotifications(userId, today, effectiveAnniversaryDays, persons, deduplicationCutoff))
        }

        if (notifications.isNotEmpty()) {
            notificationRepository.saveAll(notifications)
        }

        return notifications.size
    }

    private fun generateOverdueActionItemNotifications(
        userId: UserId,
        today: LocalDate,
        personMap: Map<PersonId, Person>,
        deduplicationCutoff: Instant
    ): List<Notification> {
        val overdueItems = actionItemRepository.findOverdueByUserId(userId, today, PageRequest.of(0, 100))
        return overdueItems.content.mapNotNull { item ->
            val referenceId = item.id.value.toString()
            if (notificationRepository.existsByUserIdAndTypeAndReferenceIdAndCreatedAfter(
                    userId, NotificationType.ACTION_ITEM_OVERDUE, referenceId, deduplicationCutoff
                )) {
                return@mapNotNull null
            }
            val personName = personMap[item.personId]?.name ?: "Unknown"
            Notification.actionItemOverdue(
                userId = userId,
                personId = item.personId,
                personName = personName,
                actionItemId = item.id,
                actionItemTitle = item.title,
                dueDate = item.dueDate!!
            )
        }
    }

    private fun generateDueSoonActionItemNotifications(
        userId: UserId,
        today: LocalDate,
        dueSoonDays: Int,
        personMap: Map<PersonId, Person>,
        deduplicationCutoff: Instant
    ): List<Notification> {
        val dueSoonDate = today.plusDays(dueSoonDays.toLong())
        val dueSoonItems = actionItemRepository.findDueSoonByUserId(userId, today, dueSoonDate)
        return dueSoonItems.mapNotNull { item ->
            val referenceId = item.id.value.toString()
            if (notificationRepository.existsByUserIdAndTypeAndReferenceIdAndCreatedAfter(
                    userId, NotificationType.ACTION_ITEM_DUE_SOON, referenceId, deduplicationCutoff
                )) {
                return@mapNotNull null
            }
            val personName = personMap[item.personId]?.name ?: "Unknown"
            Notification.actionItemDueSoon(
                userId = userId,
                personId = item.personId,
                personName = personName,
                actionItemId = item.id,
                actionItemTitle = item.title,
                dueDate = item.dueDate!!
            )
        }
    }

    private fun generateStaleOneOnOneNotifications(
        userId: UserId,
        today: LocalDate,
        personMap: Map<PersonId, Person>,
        deduplicationCutoff: Instant
    ): List<Notification> {
        val allSeries = oneOnOneSeriesRepository.findAllByUserId(userId)
        return allSeries.mapNotNull { series ->
            val person = personMap[series.personId] ?: return@mapNotNull null
            val referenceId = series.personId.value.toString()

            if (notificationRepository.existsByUserIdAndTypeAndReferenceIdAndCreatedAfter(
                    userId, NotificationType.STALE_ONE_ON_ONE, referenceId, deduplicationCutoff
                )) {
                return@mapNotNull null
            }

            val lastMeetingDate = oneOnOneEntryRepository.findLatestMeetingDate(userId, series.personId)
            val expectedIntervalDays = cadenceToIntervalDays(series.cadenceType, series.customIntervalDays)

            val daysSinceLastMeeting = if (lastMeetingDate != null) {
                ChronoUnit.DAYS.between(
                    lastMeetingDate.atZone(ZoneOffset.UTC).toLocalDate(),
                    today
                )
            } else {
                ChronoUnit.DAYS.between(
                    series.createdAt.atZone(ZoneOffset.UTC).toLocalDate(),
                    today
                )
            }

            if (daysSinceLastMeeting > expectedIntervalDays) {
                Notification.staleOneOnOne(
                    userId = userId,
                    personId = series.personId,
                    personName = person.name,
                    daysSinceLastMeeting = daysSinceLastMeeting
                )
            } else {
                null
            }
        }
    }

    private fun generateAnniversaryNotifications(
        userId: UserId,
        today: LocalDate,
        anniversaryLookaheadDays: Int,
        persons: List<Person>,
        deduplicationCutoff: Instant
    ): List<Notification> {
        return persons
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

                if (daysUntil > anniversaryLookaheadDays) {
                    return@mapNotNull null
                }

                val referenceId = person.id.value.toString()
                if (notificationRepository.existsByUserIdAndTypeAndReferenceIdAndCreatedAfter(
                        userId, NotificationType.UPCOMING_ANNIVERSARY, referenceId, deduplicationCutoff
                    )) {
                    return@mapNotNull null
                }

                val yearsCompleted = anniversaryDate.year - startDate.year
                Notification.upcomingAnniversary(
                    userId = userId,
                    personId = person.id,
                    personName = person.name,
                    yearsCompleted = yearsCompleted,
                    daysUntil = daysUntil
                )
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
