package com.peoplemanager.application.ports

import com.peoplemanager.domain.ActionItem
import com.peoplemanager.domain.ActionItemId
import com.peoplemanager.domain.ActionItemStatus
import com.peoplemanager.domain.PersonId
import com.peoplemanager.domain.UserId
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.time.LocalDate

interface ActionItemRepository {
    fun save(actionItem: ActionItem): ActionItem
    fun findByIdAndUserIdAndPersonId(actionItemId: ActionItemId, userId: UserId, personId: PersonId): ActionItem?
    fun findAllByUserIdAndPersonId(userId: UserId, personId: PersonId, status: ActionItemStatus?, pageable: Pageable): Page<ActionItem>
    fun findAllByUserId(userId: UserId, status: ActionItemStatus?, pageable: Pageable): Page<ActionItem>
    fun findOverdueByUserId(userId: UserId, referenceDate: LocalDate, pageable: Pageable): Page<ActionItem>
    fun findDueSoonByUserId(userId: UserId, fromDate: LocalDate, toDate: LocalDate): List<ActionItem>
    fun deleteByIdAndUserIdAndPersonId(actionItemId: ActionItemId, userId: UserId, personId: PersonId): Boolean
    fun countOpenByUserIdAndPersonId(userId: UserId, personId: PersonId): Long
    fun countByUserIdAndStatus(userId: UserId, status: ActionItemStatus): Long
}
