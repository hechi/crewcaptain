package com.peoplemanager.domain

import java.time.LocalDate

/**
 * Aggregated data for a review packet. Contains both raw data and computed
 * summary statistics for a person over a date range.
 */
data class ReviewPacketData(
    val person: Person,
    val dateFrom: LocalDate,
    val dateTo: LocalDate,
    val oneOnOneEntries: List<OneOnOneEntry>,
    val actionItems: List<ActionItem>,
    val pdpGoals: List<PdpGoalWithUpdates>,
    val kudos: List<Kudos>,
    val summary: ReviewPacketSummary
)

/**
 * Computed summary statistics for the review packet.
 */
data class ReviewPacketSummary(
    val totalOneOnOnes: Int,
    val totalActionItems: Int,
    val actionItemsCompleted: Int,
    val actionItemsCanceled: Int,
    val actionItemsOpen: Int,
    val actionItemCompletionRate: Double,
    val totalPdpGoals: Int,
    val pdpGoalsAchieved: Int,
    val pdpGoalsActive: Int,
    val pdpGoalsPaused: Int,
    val pdpGoalsDropped: Int,
    val totalKudos: Int,
    val kudosTagSummary: Map<String, Int>
) {
    companion object {
        fun compute(
            oneOnOneEntries: List<OneOnOneEntry>,
            actionItems: List<ActionItem>,
            pdpGoals: List<PdpGoalWithUpdates>,
            kudos: List<Kudos>
        ): ReviewPacketSummary {
            val totalActionItems = actionItems.size
            val completed = actionItems.count { it.status == ActionItemStatus.DONE }
            val canceled = actionItems.count { it.status == ActionItemStatus.CANCELED }
            val open = actionItems.count { it.status == ActionItemStatus.OPEN }
            val completionRate = if (totalActionItems > 0) {
                completed.toDouble() / totalActionItems
            } else {
                0.0
            }

            val tagSummary = kudos
                .flatMap { it.tags }
                .groupingBy { it }
                .eachCount()
                .toSortedMap()

            return ReviewPacketSummary(
                totalOneOnOnes = oneOnOneEntries.size,
                totalActionItems = totalActionItems,
                actionItemsCompleted = completed,
                actionItemsCanceled = canceled,
                actionItemsOpen = open,
                actionItemCompletionRate = completionRate,
                totalPdpGoals = pdpGoals.size,
                pdpGoalsAchieved = pdpGoals.count { it.goal.status == PdpGoalStatus.ACHIEVED },
                pdpGoalsActive = pdpGoals.count { it.goal.status == PdpGoalStatus.ACTIVE },
                pdpGoalsPaused = pdpGoals.count { it.goal.status == PdpGoalStatus.PAUSED },
                pdpGoalsDropped = pdpGoals.count { it.goal.status == PdpGoalStatus.DROPPED },
                totalKudos = kudos.size,
                kudosTagSummary = tagSummary
            )
        }
    }
}
