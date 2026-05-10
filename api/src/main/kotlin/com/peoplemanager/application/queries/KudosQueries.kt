package com.peoplemanager.application.queries

import com.peoplemanager.domain.KudosId
import com.peoplemanager.domain.PersonId
import com.peoplemanager.domain.UserId

data class GetKudosQuery(
    val userId: UserId,
    val personId: PersonId,
    val kudosId: KudosId
)

data class ListKudosByPersonQuery(
    val userId: UserId,
    val personId: PersonId,
    val page: Int = 0,
    val size: Int = 20
)

data class ListAllKudosQuery(
    val userId: UserId,
    val page: Int = 0,
    val size: Int = 20
)
