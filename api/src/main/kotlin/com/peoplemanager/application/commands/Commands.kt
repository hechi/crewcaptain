package com.peoplemanager.application.commands

import com.peoplemanager.domain.MoraleStatus
import com.peoplemanager.domain.PersonId
import com.peoplemanager.domain.RememberItemId
import com.peoplemanager.domain.UserId
import java.time.LocalDate

data class CreatePersonCommand(
    val userId: UserId,
    val name: String,
    val preferredName: String? = null,
    val roleTitle: String? = null,
    val timezone: String? = null,
    val startDate: LocalDate? = null,
    val email: String? = null,
    val tags: List<String> = emptyList()
)

data class UpdatePersonCommand(
    val userId: UserId,
    val personId: PersonId,
    val name: String,
    val preferredName: String? = null,
    val roleTitle: String? = null,
    val timezone: String? = null,
    val startDate: LocalDate? = null,
    val email: String? = null,
    val tags: List<String> = emptyList()
)

data class DeletePersonCommand(
    val userId: UserId,
    val personId: PersonId
)

data class SetMoraleCommand(
    val userId: UserId,
    val personId: PersonId,
    val status: MoraleStatus,
    val note: String? = null
)

data class AddRememberItemCommand(
    val userId: UserId,
    val personId: PersonId,
    val text: String
)

data class RemoveRememberItemCommand(
    val userId: UserId,
    val personId: PersonId,
    val itemId: RememberItemId
)

data class ReorderRememberItemsCommand(
    val userId: UserId,
    val personId: PersonId,
    val orderedIds: List<RememberItemId>
)
