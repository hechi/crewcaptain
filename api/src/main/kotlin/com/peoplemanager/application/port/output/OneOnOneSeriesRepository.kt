package com.peoplemanager.application.port.output

import com.peoplemanager.domain.OneOnOneSeries
import com.peoplemanager.domain.PersonId
import com.peoplemanager.domain.UserId

interface OneOnOneSeriesRepository {
    fun findByUserIdAndPersonId(userId: UserId, personId: PersonId): OneOnOneSeries?
    fun findAllByUserId(userId: UserId): List<OneOnOneSeries>
    fun save(series: OneOnOneSeries): OneOnOneSeries
}
