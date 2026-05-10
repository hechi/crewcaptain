package com.peoplemanager.application.queries

import com.peoplemanager.domain.MoraleStatus
import com.peoplemanager.domain.PersonId
import com.peoplemanager.domain.UserId

data class GetPersonQuery(
    val userId: UserId,
    val personId: PersonId
)

data class ListPersonsQuery(
    val userId: UserId,
    val page: Int = 0,
    val size: Int = 20,
    val tagFilter: String? = null,
    val moraleFilter: MoraleStatus? = null
)

data class ListDeletedPersonsQuery(
    val userId: UserId,
    val page: Int = 0,
    val size: Int = 20
)
