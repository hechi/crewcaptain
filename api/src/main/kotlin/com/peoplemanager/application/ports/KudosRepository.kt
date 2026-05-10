package com.peoplemanager.application.ports

import com.peoplemanager.domain.Kudos
import com.peoplemanager.domain.KudosId
import com.peoplemanager.domain.PersonId
import com.peoplemanager.domain.UserId
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface KudosRepository {
    fun save(kudos: Kudos): Kudos
    fun findByIdAndUserIdAndPersonId(kudosId: KudosId, userId: UserId, personId: PersonId): Kudos?
    fun findAllByUserIdAndPersonId(userId: UserId, personId: PersonId, pageable: Pageable): Page<Kudos>
    fun findAllByUserId(userId: UserId, pageable: Pageable): Page<Kudos>
    fun deleteByIdAndUserIdAndPersonId(kudosId: KudosId, userId: UserId, personId: PersonId): Boolean
}
