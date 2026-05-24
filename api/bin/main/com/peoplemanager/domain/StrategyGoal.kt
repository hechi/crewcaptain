package com.peoplemanager.domain

import java.time.Instant
import java.time.LocalDate

data class StrategyGoal(
    val id: StrategyGoalId,
    val userId: UserId,
    val title: String,
    val description: String? = null,
    val targetDate: LocalDate? = null,
    val status: StrategyGoalStatus = StrategyGoalStatus.ACTIVE,
    val sensitive: Boolean = false,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
) {
    init {
        require(title.isNotBlank()) { "Strategy goal title must not be blank" }
        require(title.length <= 500) { "Strategy goal title must not exceed 500 characters" }
        require(description?.length?.let { it <= 5000 } ?: true) { "Strategy goal description must not exceed 5000 characters" }
    }

    fun achieve(): StrategyGoal {
        require(status == StrategyGoalStatus.ACTIVE) {
            "Can only achieve a goal with status ACTIVE, current status is $status"
        }
        return copy(status = StrategyGoalStatus.ACHIEVED, updatedAt = Instant.now())
    }

    fun drop(): StrategyGoal {
        require(status == StrategyGoalStatus.ACTIVE) {
            "Can only drop a goal with status ACTIVE, current status is $status"
        }
        return copy(status = StrategyGoalStatus.DROPPED, updatedAt = Instant.now())
    }

    fun updateDetails(
        title: String? = null,
        description: String? = null,
        targetDate: LocalDate? = null
    ): StrategyGoal {
        val newTitle = title ?: this.title
        require(newTitle.isNotBlank()) { "Strategy goal title must not be blank" }
        require(newTitle.length <= 500) { "Strategy goal title must not exceed 500 characters" }
        val newDescription = description ?: this.description
        require(newDescription?.length?.let { it <= 5000 } ?: true) { "Strategy goal description must not exceed 5000 characters" }
        return copy(
            title = newTitle,
            description = newDescription,
            targetDate = targetDate ?: this.targetDate,
            updatedAt = Instant.now()
        )
    }

    fun toggleSensitive(): StrategyGoal {
        return copy(sensitive = !sensitive, updatedAt = Instant.now())
    }
}
