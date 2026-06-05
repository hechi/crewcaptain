package com.peoplemanager.adapters.scheduler

import com.peoplemanager.application.NotificationGenerationService
import com.peoplemanager.application.port.output.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * Scheduled task that generates notifications for all users.
 * Runs every hour by default (configurable via app.notifications.cron).
 *
 * Each user's notifications are generated independently so that a failure
 * for one user does not block others.
 */
@Component
class NotificationScheduler(
    private val userRepository: UserRepository,
    private val notificationGenerationService: NotificationGenerationService
) {

    private val logger = LoggerFactory.getLogger(NotificationScheduler::class.java)

    @Scheduled(cron = "\${app.notifications.cron:0 0 * * * *}")
    fun generateNotifications() {
        logger.info("Starting notification generation cycle")
        val userIds = userRepository.findAllUserIds()
        var totalGenerated = 0

        for (userId in userIds) {
            try {
                val count = notificationGenerationService.generateForUser(userId)
                totalGenerated += count
                if (count > 0) {
                    logger.debug("Generated {} notifications for user {}", count, userId.value)
                }
            } catch (e: Exception) {
                logger.error("Failed to generate notifications for user {}: {}", userId.value, e.message, e)
            }
        }

        logger.info("Notification generation cycle complete. Generated {} notifications for {} users", totalGenerated, userIds.size)
    }
}
