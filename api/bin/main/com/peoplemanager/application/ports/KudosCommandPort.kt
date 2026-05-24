package com.peoplemanager.application.ports

import com.peoplemanager.application.commands.CreateKudosCommand
import com.peoplemanager.application.commands.DeleteKudosCommand
import com.peoplemanager.domain.Kudos

interface KudosCommandPort {
    fun createKudos(command: CreateKudosCommand): Kudos
    fun deleteKudos(command: DeleteKudosCommand)
}
