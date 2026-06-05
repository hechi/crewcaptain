package com.peoplemanager.adapters.persistence

import com.peoplemanager.application.port.output.NotificationRepository
import com.peoplemanager.domain.*
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
class JpaNotificationRepositoryAdapter(
    private val springDataNotificationRepository: SpringDataNotificationRepository
) : NotificationRepository {

    override fun save(notification: Notification): Notification {
        val entity = notification.toEntity()
        return springDataNotificationRepository.save(entity).toDomain()
    }

    override fun saveAll(notifications: List<Notification>): List<Notification> {
        val entities = notifications.map { it.toEntity() }
        return springDataNotificationRepository.saveAll(entities).map { it.toDomain() }
    }

    override fun findByIdAndUserId(notificationId: NotificationId, userId: UserId): Notification? {
        return springDataNotificationRepository.findByIdAndUserId(
            notificationId.value, userId.value
        )?.toDomain()
    }

    override fun findAllByUserId(userId: UserId, unreadOnly: Boolean, pageable: Pageable): Page<Notification> {
        val page = if (unreadOnly) {
            springDataNotificationRepository.findAllByUserIdAndReadAtIsNullOrderByCreatedAtDesc(userId.value, pageable)
        } else {
            springDataNotificationRepository.findAllByUserIdOrderByCreatedAtDesc(userId.value, pageable)
        }
        return page.map { it.toDomain() }
    }

    override fun countUnreadByUserId(userId: UserId): Long {
        return springDataNotificationRepository.countByUserIdAndReadAtIsNull(userId.value)
    }

    override fun markAllAsReadByUserId(userId: UserId, readAt: Instant): Int {
        return springDataNotificationRepository.markAllAsReadByUserId(userId.value, readAt)
    }

    override fun existsByUserIdAndTypeAndReferenceIdAndCreatedAfter(
        userId: UserId,
        type: NotificationType,
        referenceId: String,
        createdAfter: Instant
    ): Boolean {
        return springDataNotificationRepository.existsByUserIdAndTypeAndReferenceIdAndCreatedAfter(
            userId.value, type.name, referenceId, createdAfter
        )
    }

    private fun Notification.toEntity(): NotificationEntity = NotificationEntity(
        id = this.id.value,
        userId = this.userId.value,
        type = this.type.name,
        title = this.title,
        message = this.message,
        referenceId = this.referenceId,
        personId = this.personId?.value,
        readAt = this.readAt,
        createdAt = this.createdAt
    )

    private fun NotificationEntity.toDomain(): Notification = Notification(
        id = NotificationId(this.id),
        userId = UserId(this.userId),
        type = NotificationType.valueOf(this.type),
        title = this.title,
        message = this.message,
        referenceId = this.referenceId,
        personId = this.personId?.let { PersonId(it) },
        readAt = this.readAt,
        createdAt = this.createdAt
    )
}
