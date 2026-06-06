package com.peoplemanager.application.port.input

import com.peoplemanager.application.queries.GetTriageQueueQuery
import com.peoplemanager.domain.TriageItem

interface TriageQueryPort {
    fun getTriageQueue(query: GetTriageQueueQuery): List<TriageItem>
}
