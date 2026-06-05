package com.peoplemanager.domain.service

import com.peoplemanager.domain.*

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Domain service that formats ReviewPacketData into a structured Markdown
 * review packet document. No framework dependencies — pure domain logic.
 */
object ReviewPacketFormatter {

    private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    fun format(data: ReviewPacketData): String {
        val sb = StringBuilder()

        appendTitle(sb, data)
        appendExecutiveSummary(sb, data.summary)
        appendMorale(sb, data.person)
        appendOneOnOneSection(sb, data.oneOnOneEntries)
        appendActionItemsSection(sb, data.actionItems, data.summary)
        appendPdpSection(sb, data.pdpGoals, data.summary)
        appendKudosSection(sb, data.kudos, data.summary)
        appendFooter(sb, data)

        return sb.toString()
    }

    private fun appendTitle(sb: StringBuilder, data: ReviewPacketData) {
        sb.appendLine("# Review Packet: ${data.person.name}")
        sb.appendLine()
        sb.appendLine("**Period:** ${data.dateFrom.format(DATE_FORMATTER)} to ${data.dateTo.format(DATE_FORMATTER)}")
        data.person.roleTitle?.let { sb.appendLine("**Role:** $it") }
        sb.appendLine()
    }

    private fun appendExecutiveSummary(sb: StringBuilder, summary: ReviewPacketSummary) {
        sb.appendLine("## Executive Summary")
        sb.appendLine()
        sb.appendLine("| Metric | Value |")
        sb.appendLine("|--------|-------|")
        sb.appendLine("| 1:1 Meetings | ${summary.totalOneOnOnes} |")
        sb.appendLine("| Action Items Created | ${summary.totalActionItems} |")
        sb.appendLine("| Action Items Completed | ${summary.actionItemsCompleted} |")
        sb.appendLine("| Completion Rate | ${formatPercentage(summary.actionItemCompletionRate)} |")
        sb.appendLine("| PDP Goals (Total) | ${summary.totalPdpGoals} |")
        sb.appendLine("| PDP Goals Achieved | ${summary.pdpGoalsAchieved} |")
        sb.appendLine("| Kudos Given | ${summary.totalKudos} |")
        sb.appendLine()
    }

    private fun appendMorale(sb: StringBuilder, person: Person) {
        sb.appendLine("## Current Morale")
        sb.appendLine()
        sb.appendLine("**Status:** ${person.moraleStatus.name}")
        person.moraleNote?.let {
            sb.appendLine("**Note:** $it")
        }
        sb.appendLine()
    }

    private fun appendOneOnOneSection(sb: StringBuilder, entries: List<OneOnOneEntry>) {
        sb.appendLine("## 1:1 Meetings")
        sb.appendLine()

        if (entries.isEmpty()) {
            sb.appendLine("*No 1:1 meetings during this period.*")
            sb.appendLine()
            return
        }

        sb.appendLine("${entries.size} meeting(s) held during this period.")
        sb.appendLine()

        entries.forEach { entry ->
            val dateStr = formatInstant(entry.meetingDate)
            sb.appendLine("### $dateStr")
            sb.appendLine()

            if (entry.sensitive) {
                sb.appendLine("*[Sensitive content — not included in review packet]*")
                sb.appendLine()
                return@forEach
            }

            if (entry.agendaItems.isNotEmpty()) {
                sb.appendLine("**Agenda:**")
                entry.agendaItems.sortedBy { it.displayOrder }.forEach { item ->
                    val checkbox = if (item.checked) "[x]" else "[ ]"
                    sb.appendLine("- $checkbox ${item.text}")
                }
                sb.appendLine()
            }

            entry.notesMarkdown?.let { notes ->
                sb.appendLine("**Notes:**")
                sb.appendLine()
                sb.appendLine(notes)
                sb.appendLine()
            }

            entry.outcomesMarkdown?.let { outcomes ->
                sb.appendLine("**Outcomes/Decisions:**")
                sb.appendLine()
                sb.appendLine(outcomes)
                sb.appendLine()
            }

            sb.appendLine("---")
            sb.appendLine()
        }
    }

    private fun appendActionItemsSection(
        sb: StringBuilder,
        items: List<ActionItem>,
        summary: ReviewPacketSummary
    ) {
        sb.appendLine("## Action Items")
        sb.appendLine()

        if (items.isEmpty()) {
            sb.appendLine("*No action items during this period.*")
            sb.appendLine()
            return
        }

        sb.appendLine("**Completion Rate:** ${formatPercentage(summary.actionItemCompletionRate)} " +
            "(${summary.actionItemsCompleted} completed, ${summary.actionItemsOpen} open, " +
            "${summary.actionItemsCanceled} canceled)")
        sb.appendLine()

        val completedItems = items.filter { it.status == ActionItemStatus.DONE }
        val openItems = items.filter { it.status == ActionItemStatus.OPEN }
        val canceledItems = items.filter { it.status == ActionItemStatus.CANCELED }

        if (completedItems.isNotEmpty()) {
            sb.appendLine("### Completed")
            sb.appendLine()
            completedItems.forEach { appendActionItem(sb, it) }
            sb.appendLine()
        }

        if (openItems.isNotEmpty()) {
            sb.appendLine("### Still Open")
            sb.appendLine()
            openItems.forEach { appendActionItem(sb, it) }
            sb.appendLine()
        }

        if (canceledItems.isNotEmpty()) {
            sb.appendLine("### Canceled")
            sb.appendLine()
            canceledItems.forEach { appendActionItem(sb, it) }
            sb.appendLine()
        }
    }

    private fun appendActionItem(sb: StringBuilder, item: ActionItem) {
        val ownerStr = "(${item.ownerType.name.lowercase()})"
        val dueStr = item.dueDate?.let { " — due ${it.format(DATE_FORMATTER)}" } ?: ""
        val statusIcon = when (item.status) {
            ActionItemStatus.DONE -> "✅"
            ActionItemStatus.OPEN -> "⬜"
            ActionItemStatus.CANCELED -> "❌"
        }
        sb.appendLine("- $statusIcon **${item.title}** $ownerStr$dueStr")
        item.description?.let { desc ->
            sb.appendLine("  $desc")
        }
    }

    private fun appendPdpSection(
        sb: StringBuilder,
        goals: List<PdpGoalWithUpdates>,
        summary: ReviewPacketSummary
    ) {
        sb.appendLine("## Personal Development Plan")
        sb.appendLine()

        if (goals.isEmpty()) {
            sb.appendLine("*No PDP goals during this period.*")
            sb.appendLine()
            return
        }

        sb.appendLine("**Summary:** ${summary.totalPdpGoals} goal(s) — " +
            "${summary.pdpGoalsAchieved} achieved, ${summary.pdpGoalsActive} active, " +
            "${summary.pdpGoalsPaused} paused, ${summary.pdpGoalsDropped} dropped")
        sb.appendLine()

        goals.forEach { (goal, updates) ->
            val statusBadge = when (goal.status) {
                PdpGoalStatus.ACHIEVED -> "✅ ACHIEVED"
                PdpGoalStatus.ACTIVE -> "🔄 ACTIVE"
                PdpGoalStatus.PAUSED -> "⏸️ PAUSED"
                PdpGoalStatus.DROPPED -> "❌ DROPPED"
            }
            sb.appendLine("### ${goal.title} [$statusBadge]")
            sb.appendLine()

            goal.description?.let { desc ->
                sb.appendLine(desc)
                sb.appendLine()
            }

            goal.targetDate?.let { date ->
                sb.appendLine("**Target date:** ${date.format(DATE_FORMATTER)}")
                sb.appendLine()
            }

            if (updates.isNotEmpty()) {
                sb.appendLine("**Progress Updates:**")
                sb.appendLine()
                updates.sortedByDescending { it.createdAt }.forEach { update ->
                    val dateStr = formatInstant(update.createdAt)
                    if (update.sensitive) {
                        sb.appendLine("- *$dateStr* — *[Sensitive content]*")
                    } else {
                        sb.appendLine("- *$dateStr* — ${update.textMarkdown}")
                    }
                }
                sb.appendLine()
            }
        }
    }

    private fun appendKudosSection(
        sb: StringBuilder,
        kudosList: List<Kudos>,
        summary: ReviewPacketSummary
    ) {
        sb.appendLine("## Kudos & Recognition")
        sb.appendLine()

        if (kudosList.isEmpty()) {
            sb.appendLine("*No kudos recorded during this period.*")
            sb.appendLine()
            return
        }

        sb.appendLine("${summary.totalKudos} kudos recorded.")
        if (summary.kudosTagSummary.isNotEmpty()) {
            sb.appendLine()
            sb.appendLine("**Tags:** ${summary.kudosTagSummary.entries.joinToString(", ") { "${it.key} (${it.value})" }}")
        }
        sb.appendLine()

        kudosList.sortedByDescending { it.date }.forEach { kudos ->
            val dateStr = kudos.date.format(DATE_FORMATTER)
            val tagsStr = if (kudos.tags.isNotEmpty()) " [${kudos.tags.joinToString(", ")}]" else ""
            sb.appendLine("- **$dateStr**$tagsStr — ${kudos.text}")
        }
        sb.appendLine()
    }

    private fun appendFooter(sb: StringBuilder, data: ReviewPacketData) {
        sb.appendLine("---")
        sb.appendLine()
        sb.appendLine("*Review packet generated on ${LocalDate.now().format(DATE_FORMATTER)} " +
            "for period ${data.dateFrom.format(DATE_FORMATTER)} to ${data.dateTo.format(DATE_FORMATTER)}*")
    }

    private fun formatPercentage(rate: Double): String {
        return "${(rate * 100).toInt()}%"
    }

    private fun formatInstant(instant: Instant): String {
        return instant.atZone(ZoneOffset.UTC).format(DATETIME_FORMATTER)
    }
}
