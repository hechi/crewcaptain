package com.peoplemanager.domain

import java.time.Instant

data class QuickNote(
    val id: QuickNoteId,
    val userId: UserId,
    val personId: PersonId? = null,
    val text: String,
    val sensitive: Boolean = false,
    val selfAssigned: Boolean = false,
    val status: QuickNoteStatus = QuickNoteStatus.INBOX,
    val attachedEntryId: OneOnOneEntryId? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
) {
    init {
        require(text.isNotBlank()) { "Quick note text must not be blank" }
        require(!(selfAssigned && personId != null)) {
            "A quick note cannot be both self-assigned and assigned to a person"
        }
    }

    fun assignToPerson(personId: PersonId): QuickNote {
        return copy(personId = personId, selfAssigned = false, updatedAt = Instant.now())
    }

    fun markSelfAssigned(): QuickNote {
        return copy(selfAssigned = true, personId = null, updatedAt = Instant.now())
    }

    fun markAttached(entryId: OneOnOneEntryId): QuickNote {
        require(status == QuickNoteStatus.INBOX) {
            "Can only attach a quick note with status INBOX, current status is $status"
        }
        return copy(status = QuickNoteStatus.ATTACHED, attachedEntryId = entryId, updatedAt = Instant.now())
    }

    fun markConverted(): QuickNote {
        require(status == QuickNoteStatus.INBOX) {
            "Can only convert a quick note with status INBOX, current status is $status"
        }
        return copy(status = QuickNoteStatus.CONVERTED, updatedAt = Instant.now())
    }

    fun archive(): QuickNote {
        require(status == QuickNoteStatus.INBOX) {
            "Can only archive a quick note with status INBOX, current status is $status"
        }
        return copy(status = QuickNoteStatus.ARCHIVED, updatedAt = Instant.now())
    }

    fun updateText(newText: String): QuickNote {
        require(newText.isNotBlank()) { "Quick note text must not be blank" }
        return copy(text = newText, updatedAt = Instant.now())
    }

    fun toggleSensitive(): QuickNote {
        return copy(sensitive = !sensitive, updatedAt = Instant.now())
    }
}
