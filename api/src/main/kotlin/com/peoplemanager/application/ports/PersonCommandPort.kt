package com.peoplemanager.application.ports

import com.peoplemanager.application.commands.AddRememberItemCommand
import com.peoplemanager.application.commands.CreatePersonCommand
import com.peoplemanager.application.commands.DeletePersonCommand
import com.peoplemanager.application.commands.RemoveRememberItemCommand
import com.peoplemanager.application.commands.ReorderRememberItemsCommand
import com.peoplemanager.application.commands.RestorePersonCommand
import com.peoplemanager.application.commands.SetMoraleCommand
import com.peoplemanager.application.commands.UpdatePersonCommand
import com.peoplemanager.domain.Person
import com.peoplemanager.domain.PinnedRememberItem

interface PersonCommandPort {
    fun createPerson(command: CreatePersonCommand): Person
    fun updatePerson(command: UpdatePersonCommand): Person
    fun deletePerson(command: DeletePersonCommand)
    fun restorePerson(command: RestorePersonCommand): Person
    fun setMorale(command: SetMoraleCommand): Person
    fun addRememberItem(command: AddRememberItemCommand): List<PinnedRememberItem>
    fun removeRememberItem(command: RemoveRememberItemCommand): List<PinnedRememberItem>
    fun reorderRememberItems(command: ReorderRememberItemsCommand): List<PinnedRememberItem>
}
