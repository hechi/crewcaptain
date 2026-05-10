package com.peoplemanager.adapters.web.dto

import com.peoplemanager.domain.PdpGoal
import com.peoplemanager.domain.PdpGoalStatus
import com.peoplemanager.domain.PdpUpdate
import org.springframework.data.domain.Page
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class PdpGoalResponse(
    val id: UUID,
    val personId: UUID,
    val title: String,
    val description: String?,
    val targetDate: LocalDate?,
    val status: PdpGoalStatus,
    val createdAt: Instant,
    val updatedAt: Instant
) {
    companion object {
        fun from(goal: PdpGoal): PdpGoalResponse = PdpGoalResponse(
            id = goal.id.value,
            personId = goal.personId.value,
            title = goal.title,
            description = goal.description,
            targetDate = goal.targetDate,
            status = goal.status,
            createdAt = goal.createdAt,
            updatedAt = goal.updatedAt
        )
    }
}

data class PaginatedPdpGoalResponse(
    val content: List<PdpGoalResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int
) {
    companion object {
        fun from(pageResult: Page<PdpGoal>): PaginatedPdpGoalResponse =
            PaginatedPdpGoalResponse(
                content = pageResult.content.map { PdpGoalResponse.from(it) },
                page = pageResult.number,
                size = pageResult.size,
                totalElements = pageResult.totalElements,
                totalPages = pageResult.totalPages
            )
    }
}

data class PdpUpdateResponse(
    val id: UUID,
    val goalId: UUID,
    val textMarkdown: String,
    val sensitive: Boolean,
    val createdAt: Instant
) {
    companion object {
        fun from(update: PdpUpdate): PdpUpdateResponse = PdpUpdateResponse(
            id = update.id.value,
            goalId = update.goalId.value,
            textMarkdown = update.textMarkdown,
            sensitive = update.sensitive,
            createdAt = update.createdAt
        )
    }
}

data class PaginatedPdpUpdateResponse(
    val content: List<PdpUpdateResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int
) {
    companion object {
        fun from(pageResult: Page<PdpUpdate>): PaginatedPdpUpdateResponse =
            PaginatedPdpUpdateResponse(
                content = pageResult.content.map { PdpUpdateResponse.from(it) },
                page = pageResult.number,
                size = pageResult.size,
                totalElements = pageResult.totalElements,
                totalPages = pageResult.totalPages
            )
    }
}
