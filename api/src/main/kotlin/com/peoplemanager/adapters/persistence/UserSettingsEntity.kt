package com.peoplemanager.adapters.persistence

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "user_settings")
class UserSettingsEntity(
    @Id
    @Column(name = "user_id")
    val userId: UUID = UUID.randomUUID(),

    @Column(name = "due_soon_days", nullable = false)
    val dueSoonDays: Int = 3,

    @Column(name = "stale_one_on_one_days", nullable = false)
    val staleOneOnOneDays: Int = 14,

    @Column(name = "anniversary_lookahead_days", nullable = false)
    val anniversaryLookaheadDays: Int = 30,

    @Column(name = "theme", nullable = false)
    val theme: String = "DARK",

    @Column(name = "show_achievements", nullable = false)
    val showAchievements: Boolean = true,

    @Column(name = "notify_action_item_overdue", nullable = false)
    val notifyActionItemOverdue: Boolean = true,

    @Column(name = "notify_action_item_due_soon", nullable = false)
    val notifyActionItemDueSoon: Boolean = true,

    @Column(name = "notify_stale_one_on_one", nullable = false)
    val notifyStaleOneOnOne: Boolean = true,

    @Column(name = "notify_upcoming_anniversary", nullable = false)
    val notifyUpcomingAnniversary: Boolean = true,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant = Instant.now()
)
