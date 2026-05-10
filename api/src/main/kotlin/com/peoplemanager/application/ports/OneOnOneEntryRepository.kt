package com.peoplemanager.application.ports

import com.peoplemanager.domain.OneOnOneEntry
import com.peoplemanager.domain.OneOnOneEntryId
import com.peoplemanager.domain.PersonId
import com.peoplemanager.domain.UserId
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.time.Instant

interface OneOnOneEntryRepository {
    fun save(entry: OneOnOneEntry): OneOnOneEntry
    fun findByIdAndUserId(entryId: OneOnOneEntryId, userId: UserId): OneOnOneEntry?
    fun findByIdAndUserIdAndPersonId(entryId: OneOnOneEntryId, userId: UserId, personId: PersonId): OneOnOneEntry?
    fun findAllByUserIdAndPersonId(userId: UserId, personId: PersonId, pageable: Pageable): Page<OneOnOneEntry>
    fun deleteByIdAndUserIdAndPersonId(entryId: OneOnOneEntryId, userId: UserId, personId: PersonId): Boolean
    fun findLatestMeetingDate(userId: UserId, personId: PersonId): Instant?
}
