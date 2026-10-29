package com.peoplemanager.application.port.output

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
    fun findAllByUserIdAndTag(userId: UserId, tag: String, pageable: Pageable): Page<QuickNote>
    fun findAllByUserIdAndSelfAssignedAndTag(userId: UserId, selfAssigned: Boolean, tag: String, pageable: Pageable): Page<QuickNote>
    fun deleteByIdAndUserId(quickNoteId: QuickNoteId, userId: UserId): Boolean

    /**
     * Returns the user's self-assigned notes of the given status ordered by their manual
     * display order (ascending; ties broken by newest-first). Used to resolve the adjacent
     * note when moving a note up or down. Always scoped by userId.
     */
    fun findSelfAssignedOrdered(userId: UserId, status: QuickNoteStatus): List<QuickNote>

    /**
     * Returns the lowest (top-most) display order among the user's self-assigned notes, or
     * null if the user has none. Used to place newly-created personal notes on top.
     */
    fun findMinSelfAssignedDisplayOrder(userId: UserId): Int?
}
