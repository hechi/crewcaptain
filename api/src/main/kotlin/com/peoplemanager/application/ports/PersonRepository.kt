package com.peoplemanager.application.ports

import com.peoplemanager.domain.MoraleStatus
import com.peoplemanager.domain.Person
import com.peoplemanager.domain.PersonId
import com.peoplemanager.domain.UserId
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface PersonRepository {
    fun save(person: Person): Person
    fun findByIdAndUserId(personId: PersonId, userId: UserId): Person?
    fun findAllByUserId(userId: UserId, pageable: Pageable, tagFilter: String?, moraleFilter: MoraleStatus?): Page<Person>
    fun findAllByUserIdUnpaged(userId: UserId): List<Person>
    fun deleteByIdAndUserId(personId: PersonId, userId: UserId): Boolean
}
