package com.peoplemanager.application.queries

import com.peoplemanager.domain.PdpGoalId
import com.peoplemanager.domain.PdpGoalStatus
import com.peoplemanager.domain.PersonId
import com.peoplemanager.domain.UserId

data class GetPdpGoalQuery(
    val userId: UserId,
    val personId: PersonId,
    val goalId: PdpGoalId
)

data class ListPdpGoalsByPersonQuery(
    val userId: UserId,
    val personId: PersonId,
    val status: PdpGoalStatus? = null,
    val page: Int = 0,
    val size: Int = 20
)

data class ListPdpUpdatesByGoalQuery(
    val userId: UserId,
    val personId: PersonId,
    val goalId: PdpGoalId,
    val page: Int = 0,
    val size: Int = 20
)

data class CountActivePdpGoalsQuery(
    val userId: UserId,
    val personId: PersonId
)
