package com.peoplemanager.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Domain service that formats PersonExportData into a Markdown string.
 * No framework dependencies — pure domain logic.
 */
object MarkdownExportFormatter {

    private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    fun format(data: PersonExportData): String {
        val sb = StringBuilder()

        appendHeader(sb, data)
        appendProfileSummary(sb, data.person)
        appendPinnedRememberItems(sb, data.person.pinnedRememberItems)
        appendMorale(sb, data.person)
        appendOneOnOneHistory(sb, data.oneOnOneEntries)
        appendActionItems(sb, data.actionItems)
        appendPdpGoals(sb, data.pdpGoals)
        appendKudos(sb, data.kudos)
        appendFooter(sb)

        return sb.toString()
    }

    private fun appendHeader(sb: StringBuilder, data: PersonExportData) {
        sb.appendLine("# ${data.person.name}")
        sb.appendLine()
        if (data.dateFrom != null || data.dateTo != null) {
            val fromStr = data.dateFrom?.format(DATE_FORMATTER) ?: "beginning"
            val toStr = data.dateTo?.format(DATE_FORMATTER) ?: "present"
            sb.appendLine("*Export date range: $fromStr to $toStr*")
            sb.appendLine()
        }
    }

    private fun appendProfileSummary(sb: StringBuilder, person: Person) {
        sb.appendLine("## Profile")
        sb.appendLine()
        sb.appendLine("| Field | Value |")
        sb.appendLine("|-------|-------|")
        sb.appendLine("| Name | ${person.name} |")
        person.preferredName?.let { sb.appendLine("| Preferred Name | $it |") }
        person.roleTitle?.let { sb.appendLine("| Role/Title | $it |") }
        person.timezone?.let { sb.appendLine("| Timezone | $it |") }
        person.startDate?.let { sb.appendLine("| Start Date | ${it.format(DATE_FORMATTER)} |") }
        person.email?.let { sb.appendLine("| Email | $it |") }
        if (person.tags.isNotEmpty()) {
            sb.appendLine("| Tags | ${person.tags.joinToString(", ")} |")
        }
        sb.appendLine()
    }

    private fun appendPinnedRememberItems(sb: StringBuilder, items: List<PinnedRememberItem>) {
        if (items.isEmpty()) return

        sb.appendLine("## Pinned / Sticky Notes")
        sb.appendLine()
        items.sortedBy { it.displayOrder }.forEach { item ->
            val tagStr = item.tag?.let { " [$it]" } ?: ""
            if (item.sensitive) {
                sb.appendLine("- *[Sensitive note]*$tagStr")
            } else {
                sb.appendLine("- ${item.text}$tagStr")
            }
        }
        sb.appendLine()
    }

    private fun appendMorale(sb: StringBuilder, person: Person) {
        sb.appendLine("## Morale")
        sb.appendLine()
        sb.appendLine("**Status:** ${person.moraleStatus.name}")
        person.moraleNote?.let {
            sb.appendLine()
            sb.appendLine("**Note:** $it")
        }
        sb.appendLine()
    }

    private fun appendOneOnOneHistory(sb: StringBuilder, entries: List<OneOnOneEntry>) {
        sb.appendLine("## 1:1 History")
        sb.appendLine()

        if (entries.isEmpty()) {
            sb.appendLine("*No 1:1 entries recorded.*")
            sb.appendLine()
            return
        }

        // Entries should already be sorted reverse chronologically
        entries.forEach { entry ->
            val dateStr = formatInstant(entry.meetingDate)
            sb.appendLine("### $dateStr")
            sb.appendLine()

            if (entry.sensitive) {
                sb.appendLine("*[Sensitive content]*")
                sb.appendLine()
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
                if (!entry.sensitive) {
                    sb.appendLine("**Notes:**")
                    sb.appendLine()
                    sb.appendLine(notes)
                    sb.appendLine()
                }
            }

            entry.outcomesMarkdown?.let { outcomes ->
                if (!entry.sensitive) {
                    sb.appendLine("**Outcomes:**")
                    sb.appendLine()
                    sb.appendLine(outcomes)
                    sb.appendLine()
                }
            }

            sb.appendLine("---")
            sb.appendLine()
        }
    }

    private fun appendActionItems(sb: StringBuilder, items: List<ActionItem>) {
        sb.appendLine("## Action Items")
        sb.appendLine()

        if (items.isEmpty()) {
            sb.appendLine("*No action items.*")
            sb.appendLine()
            return
        }

        val openItems = items.filter { it.status == ActionItemStatus.OPEN }
        val doneItems = items.filter { it.status == ActionItemStatus.DONE }
        val canceledItems = items.filter { it.status == ActionItemStatus.CANCELED }

        if (openItems.isNotEmpty()) {
            sb.appendLine("### Open")
            sb.appendLine()
            openItems.forEach { item -> appendActionItem(sb, item) }
            sb.appendLine()
        }

        if (doneItems.isNotEmpty()) {
            sb.appendLine("### Done")
            sb.appendLine()
            doneItems.forEach { item -> appendActionItem(sb, item) }
            sb.appendLine()
        }

        if (canceledItems.isNotEmpty()) {
            sb.appendLine("### Canceled")
            sb.appendLine()
            canceledItems.forEach { item -> appendActionItem(sb, item) }
            sb.appendLine()
        }
    }

    private fun appendActionItem(sb: StringBuilder, item: ActionItem) {
        val checkbox = when (item.status) {
            ActionItemStatus.OPEN -> "[ ]"
            ActionItemStatus.DONE -> "[x]"
            ActionItemStatus.CANCELED -> "[-]"
        }
        val ownerStr = "(${item.ownerType.name.lowercase()})"
        val dueStr = item.dueDate?.let { " — due ${it.format(DATE_FORMATTER)}" } ?: ""
        sb.appendLine("- $checkbox **${item.title}** $ownerStr$dueStr")
        item.description?.let { desc ->
            sb.appendLine("  $desc")
        }
    }

    private fun appendPdpGoals(sb: StringBuilder, goals: List<PdpGoalWithUpdates>) {
        sb.appendLine("## PDP Goals")
        sb.appendLine()

        if (goals.isEmpty()) {
            sb.appendLine("*No PDP goals.*")
            sb.appendLine()
            return
        }

        goals.forEach { (goal, updates) ->
            val statusBadge = "[${goal.status.name}]"
            sb.appendLine("### ${goal.title} $statusBadge")
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

    private fun appendKudos(sb: StringBuilder, kudosList: List<Kudos>) {
        sb.appendLine("## Kudos")
        sb.appendLine()

        if (kudosList.isEmpty()) {
            sb.appendLine("*No kudos recorded.*")
            sb.appendLine()
            return
        }

        kudosList.sortedByDescending { it.date }.forEach { kudos ->
            val dateStr = kudos.date.format(DATE_FORMATTER)
            val tagsStr = if (kudos.tags.isNotEmpty()) " (${kudos.tags.joinToString(", ")})" else ""
            sb.appendLine("- **$dateStr**$tagsStr — ${kudos.text}")
        }
        sb.appendLine()
    }

    private fun appendFooter(sb: StringBuilder) {
        sb.appendLine("---")
        sb.appendLine()
        sb.appendLine("*Exported on ${LocalDate.now().format(DATE_FORMATTER)}*")
    }

    private fun formatInstant(instant: Instant): String {
        return instant.atZone(ZoneOffset.UTC).format(DATETIME_FORMATTER)
    }
}
