package com.peoplemanager.application.queries

import com.peoplemanager.domain.PersonId
import com.peoplemanager.domain.UserId
import com.peoplemanager.domain.service.AnalyticsBucket
import java.time.LocalDate

data class GetFeedbackAnalyticsQuery(
    val userId: UserId,
    val personId: PersonId,
    val from: LocalDate? = null,
    val to: LocalDate? = null,
    val bucket: AnalyticsBucket = AnalyticsBucket.QUARTER
)

data class ListFeedbackSummariesQuery(
    val userId: UserId,
    val personId: PersonId
)
