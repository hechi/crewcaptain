package com.peoplemanager.adapters.persistence

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface SpringDataQuickNoteRepository : JpaRepository<QuickNoteEntity, UUID> {

    fun findByIdAndUserId(id: UUID, userId: UUID): QuickNoteEntity?

    fun findAllByUserId(userId: UUID, pageable: Pageable): Page<QuickNoteEntity>

    fun findAllByUserIdAndStatus(userId: UUID, status: String, pageable: Pageable): Page<QuickNoteEntity>

    fun findAllByUserIdAndPersonId(userId: UUID, personId: UUID, pageable: Pageable): Page<QuickNoteEntity>

    fun findAllByUserIdAndStatusAndPersonId(userId: UUID, status: String, personId: UUID, pageable: Pageable): Page<QuickNoteEntity>

    fun findAllByUserIdAndSelfAssigned(userId: UUID, selfAssigned: Boolean, pageable: Pageable): Page<QuickNoteEntity>

    fun findAllByUserIdAndSelfAssignedAndStatus(userId: UUID, selfAssigned: Boolean, status: String, pageable: Pageable): Page<QuickNoteEntity>

    fun deleteByIdAndUserId(id: UUID, userId: UUID): Long
}
