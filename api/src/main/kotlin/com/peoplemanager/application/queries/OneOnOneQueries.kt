package com.peoplemanager.application.queries

import com.peoplemanager.domain.OneOnOneEntryId
import com.peoplemanager.domain.PersonId
import com.peoplemanager.domain.UserId

data class GetOneOnOneSeriesQuery(
    val userId: UserId,
    val personId: PersonId
)

data class GetOneOnOneEntryQuery(
    val userId: UserId,
    val personId: PersonId,
    val entryId: OneOnOneEntryId
)

data class ListOneOnOneEntriesQuery(
    val userId: UserId,
    val personId: PersonId,
    val page: Int = 0,
    val size: Int = 20
)

data class GetLastOneOnOneDateQuery(
    val userId: UserId,
    val personId: PersonId
)
