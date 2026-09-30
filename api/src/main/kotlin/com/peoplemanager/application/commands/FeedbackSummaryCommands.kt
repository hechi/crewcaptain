package com.peoplemanager.application.commands

import com.peoplemanager.domain.PersonId
import com.peoplemanager.domain.UserId
import java.time.LocalDate

/** Generate (but do not persist) an AI narrative summary of a person's feedback. */
data class GenerateFeedbackSummaryCommand(
    val userId: UserId,
    val personId: PersonId,
    val from: LocalDate? = null,
    val to: LocalDate? = null
)

/** Persist a manager-edited feedback summary. */
data class SaveFeedbackSummaryCommand(
    val userId: UserId,
    val personId: PersonId,
    val periodFrom: LocalDate,
    val periodTo: LocalDate,
    val content: String,
    val responseCount: Int
)
