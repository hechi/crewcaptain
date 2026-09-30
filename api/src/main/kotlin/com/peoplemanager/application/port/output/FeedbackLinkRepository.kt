package com.peoplemanager.application.port.output

import com.peoplemanager.domain.FeedbackLink
import com.peoplemanager.domain.FeedbackLinkId
import com.peoplemanager.domain.PersonId
import com.peoplemanager.domain.UserId

interface FeedbackLinkRepository {
    fun save(link: FeedbackLink): FeedbackLink
    fun findByIdAndUserId(id: FeedbackLinkId, userId: UserId): FeedbackLink?
    fun findAllByUserIdAndPersonId(userId: UserId, personId: PersonId): List<FeedbackLink>

    /** Public resolution by token. Returns the link regardless of owner (caller must not leak owner data). */
    fun findByToken(token: String): FeedbackLink?

    fun existsByToken(token: String): Boolean
}
