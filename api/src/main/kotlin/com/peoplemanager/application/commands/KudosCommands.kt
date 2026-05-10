package com.peoplemanager.application.commands

import com.peoplemanager.domain.KudosId
import com.peoplemanager.domain.PersonId
import com.peoplemanager.domain.UserId
import java.time.LocalDate

data class CreateKudosCommand(
    val userId: UserId,
    val personId: PersonId,
    val date: LocalDate,
    val text: String,
    val tags: List<String> = emptyList()
)

data class DeleteKudosCommand(
    val userId: UserId,
    val personId: PersonId,
    val kudosId: KudosId
)
