package com.peoplemanager.application.ports

import com.peoplemanager.domain.QuickNote
import com.peoplemanager.domain.QuickNoteId
import com.peoplemanager.domain.QuickNoteStatus
import com.peoplemanager.domain.PersonId
import com.peoplemanager.domain.UserId
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface QuickNoteRepository {
    fun save(quickNote: QuickNote): QuickNote
    fun findByIdAndUserId(quickNoteId: QuickNoteId, userId: UserId): QuickNote?
    fun findAllByUserId(userId: UserId, pageable: Pageable): Page<QuickNote>
    fun findAllByUserIdAndStatus(userId: UserId, status: QuickNoteStatus, pageable: Pageable): Page<QuickNote>
    fun findAllByUserIdAndPersonId(userId: UserId, personId: PersonId, pageable: Pageable): Page<QuickNote>
    fun findAllByUserIdAndStatusAndPersonId(userId: UserId, status: QuickNoteStatus, personId: PersonId, pageable: Pageable): Page<QuickNote>
    fun findAllByUserIdAndSelfAssigned(userId: UserId, selfAssigned: Boolean, pageable: Pageable): Page<QuickNote>
    fun findAllByUserIdAndSelfAssignedAndStatus(userId: UserId, selfAssigned: Boolean, status: QuickNoteStatus, pageable: Pageable): Page<QuickNote>
    fun deleteByIdAndUserId(quickNoteId: QuickNoteId, userId: UserId): Boolean
}
