package com.peoplemanager.application.ports

import com.peoplemanager.application.commands.CreateOneOnOneEntryCommand
import com.peoplemanager.application.commands.DeleteOneOnOneEntryCommand
import com.peoplemanager.application.commands.UpdateOneOnOneEntryCommand
import com.peoplemanager.application.commands.UpsertOneOnOneSeriesCommand
import com.peoplemanager.domain.OneOnOneEntry
import com.peoplemanager.domain.OneOnOneSeries

interface OneOnOneCommandPort {
    fun upsertSeries(command: UpsertOneOnOneSeriesCommand): OneOnOneSeries
    fun createEntry(command: CreateOneOnOneEntryCommand): OneOnOneEntry
    fun updateEntry(command: UpdateOneOnOneEntryCommand): OneOnOneEntry
    fun deleteEntry(command: DeleteOneOnOneEntryCommand)
}
