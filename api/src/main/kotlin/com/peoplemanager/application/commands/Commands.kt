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

data class RestorePersonCommand(
    val userId: UserId,
    val personId: PersonId
)

data class PermanentDeletePersonCommand(
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
    val text: String,
    val color: String? = null,
    val tag: String? = null,
    val sensitive: Boolean = false
)

data class UpdateRememberItemCommand(
    val userId: UserId,
    val personId: PersonId,
    val itemId: RememberItemId,
    val text: String,
    val color: String? = null,
    val tag: String? = null,
    val sensitive: Boolean = false
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
