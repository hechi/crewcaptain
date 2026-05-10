package com.peoplemanager.domain

import java.time.LocalDate

/**
 * Aggregated data for a person export. This is a read-only value object
 * that holds all the data needed to generate a Markdown export.
 */
data class PersonExportData(
    val person: Person,
    val oneOnOneEntries: List<OneOnOneEntry>,
    val actionItems: List<ActionItem>,
    val pdpGoals: List<PdpGoalWithUpdates>,
    val kudos: List<Kudos>,
    val dateFrom: LocalDate? = null,
    val dateTo: LocalDate? = null
)

data class PdpGoalWithUpdates(
    val goal: PdpGoal,
    val updates: List<PdpUpdate>
)
