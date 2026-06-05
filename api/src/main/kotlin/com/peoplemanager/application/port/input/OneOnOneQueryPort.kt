package com.peoplemanager.application.port.input

import com.peoplemanager.application.queries.GetLastOneOnOneDateQuery
import com.peoplemanager.application.queries.GetOneOnOneEntryQuery
import com.peoplemanager.application.queries.GetOneOnOneSeriesQuery
import com.peoplemanager.application.queries.ListOneOnOneEntriesQuery
import com.peoplemanager.domain.OneOnOneEntry
import com.peoplemanager.domain.OneOnOneSeries
import org.springframework.data.domain.Page
import java.time.Instant

interface OneOnOneQueryPort {
    fun getSeries(query: GetOneOnOneSeriesQuery): OneOnOneSeries?
    fun getEntry(query: GetOneOnOneEntryQuery): OneOnOneEntry
    fun listEntries(query: ListOneOnOneEntriesQuery): Page<OneOnOneEntry>
    fun getLastOneOnOneDate(query: GetLastOneOnOneDateQuery): Instant?
}
