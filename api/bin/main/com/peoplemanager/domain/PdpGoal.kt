package com.peoplemanager.domain

import java.time.Instant
import java.time.LocalDate

data class PdpGoal(
    val id: PdpGoalId,
    val userId: UserId,
    val personId: PersonId,
    val title: String,
    val description: String? = null,
    val targetDate: LocalDate? = null,
    val status: PdpGoalStatus = PdpGoalStatus.ACTIVE,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
) {
    init {
        require(title.isNotBlank()) { "PDP goal title must not be blank" }
    }

    fun achieve(): PdpGoal {
        require(status == PdpGoalStatus.ACTIVE) {
            "Can only achieve a goal with status ACTIVE, current status is $status"
        }
        return copy(status = PdpGoalStatus.ACHIEVED, updatedAt = Instant.now())
    }

    fun pause(): PdpGoal {
        require(status == PdpGoalStatus.ACTIVE) {
            "Can only pause a goal with status ACTIVE, current status is $status"
        }
        return copy(status = PdpGoalStatus.PAUSED, updatedAt = Instant.now())
    }

    fun drop(): PdpGoal {
        require(status == PdpGoalStatus.ACTIVE) {
            "Can only drop a goal with status ACTIVE, current status is $status"
        }
        return copy(status = PdpGoalStatus.DROPPED, updatedAt = Instant.now())
    }

    fun resume(): PdpGoal {
        require(status == PdpGoalStatus.PAUSED) {
            "Can only resume a goal with status PAUSED, current status is $status"
        }
        return copy(status = PdpGoalStatus.ACTIVE, updatedAt = Instant.now())
    }

    fun updateDetails(
        title: String? = null,
        description: String? = null,
        targetDate: LocalDate? = null
    ): PdpGoal {
        val newTitle = title ?: this.title
        require(newTitle.isNotBlank()) { "PDP goal title must not be blank" }
        return copy(
            title = newTitle,
            description = description ?: this.description,
            targetDate = targetDate ?: this.targetDate,
            updatedAt = Instant.now()
        )
    }
}
