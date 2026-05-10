package com.peoplemanager.domain

import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class ReviewPacketFormatterTest {

    private val userId = UserId.generate()
    private val personId = PersonId.generate()
    private val dateFrom = LocalDate.of(2024, 1, 1)
    private val dateTo = LocalDate.of(2024, 6, 30)

    private fun createPerson(
        name: String = "Jane Smith",
        roleTitle: String? = "Senior Engineer",
        moraleStatus: MoraleStatus = MoraleStatus.GREEN,
        moraleNote: String? = "Doing great"
    ) = Person(
        id = personId,
        userId = userId,
        name = name,
        preferredName = "Jane",
        roleTitle = roleTitle,
        timezone = "Europe/Berlin",
        startDate = LocalDate.of(2023, 3, 15),
        email = "jane@example.com",
        tags = listOf("engineering"),
        moraleStatus = moraleStatus,
        moraleNote = moraleNote,
        pinnedRememberItems = emptyList()
    )

    private fun createReviewPacketData(
        person: Person = createPerson(),
        oneOnOneEntries: List<OneOnOneEntry> = emptyList(),
        actionItems: List<ActionItem> = emptyList(),
        pdpGoals: List<PdpGoalWithUpdates> = emptyList(),
        kudos: List<Kudos> = emptyList()
    ): ReviewPacketData {
        val summary = ReviewPacketSummary.compute(oneOnOneEntries, actionItems, pdpGoals, kudos)
        return ReviewPacketData(
            person = person,
            dateFrom = dateFrom,
            dateTo = dateTo,
            oneOnOneEntries = oneOnOneEntries,
            actionItems = actionItems,
            pdpGoals = pdpGoals,
            kudos = kudos,
            summary = summary
        )
    }

    @Nested
    inner class TitleTests {
        @Test
        fun `should include person name in title`() {
            val result = ReviewPacketFormatter.format(createReviewPacketData())
            result shouldContain "# Review Packet: Jane Smith"
        }

        @Test
        fun `should include date range`() {
            val result = ReviewPacketFormatter.format(createReviewPacketData())
            result shouldContain "**Period:** 2024-01-01 to 2024-06-30"
        }

        @Test
        fun `should include role when present`() {
            val result = ReviewPacketFormatter.format(createReviewPacketData())
            result shouldContain "**Role:** Senior Engineer"
        }

        @Test
        fun `should not include role line when role is null`() {
            val person = createPerson(roleTitle = null)
            val result = ReviewPacketFormatter.format(createReviewPacketData(person = person))
            result shouldNotContain "**Role:**"
        }
    }

    @Nested
    inner class ExecutiveSummaryTests {
        @Test
        fun `should include executive summary section`() {
            val result = ReviewPacketFormatter.format(createReviewPacketData())
            result shouldContain "## Executive Summary"
        }

        @Test
        fun `should show zero values for empty data`() {
            val result = ReviewPacketFormatter.format(createReviewPacketData())
            result shouldContain "| 1:1 Meetings | 0 |"
            result shouldContain "| Action Items Created | 0 |"
            result shouldContain "| Action Items Completed | 0 |"
            result shouldContain "| Completion Rate | 0% |"
            result shouldContain "| PDP Goals (Total) | 0 |"
            result shouldContain "| PDP Goals Achieved | 0 |"
            result shouldContain "| Kudos Given | 0 |"
        }

        @Test
        fun `should show correct counts with data`() {
            val entries = listOf(
                OneOnOneEntry(
                    id = OneOnOneEntryId.generate(), userId = userId, personId = personId,
                    meetingDate = Instant.parse("2024-03-01T10:00:00Z"),
                    agendaItems = emptyList(), sensitive = false
                )
            )
            val items = listOf(
                ActionItem(
                    id = ActionItemId.generate(), userId = userId, personId = personId,
                    title = "Done item", status = ActionItemStatus.DONE, createdAt = Instant.now()
                ),
                ActionItem(
                    id = ActionItemId.generate(), userId = userId, personId = personId,
                    title = "Open item", status = ActionItemStatus.OPEN, createdAt = Instant.now()
                )
            )
            val result = ReviewPacketFormatter.format(createReviewPacketData(
                oneOnOneEntries = entries,
                actionItems = items
            ))
            result shouldContain "| 1:1 Meetings | 1 |"
            result shouldContain "| Action Items Created | 2 |"
            result shouldContain "| Action Items Completed | 1 |"
            result shouldContain "| Completion Rate | 50% |"
        }
    }

    @Nested
    inner class MoraleTests {
        @Test
        fun `should include morale section`() {
            val result = ReviewPacketFormatter.format(createReviewPacketData())
            result shouldContain "## Current Morale"
            result shouldContain "**Status:** GREEN"
        }

        @Test
        fun `should include morale note when present`() {
            val result = ReviewPacketFormatter.format(createReviewPacketData())
            result shouldContain "**Note:** Doing great"
        }

        @Test
        fun `should not include note line when morale note is null`() {
            val person = createPerson(moraleNote = null)
            val result = ReviewPacketFormatter.format(createReviewPacketData(person = person))
            result shouldNotContain "**Note:**"
        }
    }

    @Nested
    inner class OneOnOneSectionTests {
        @Test
        fun `should show empty message when no entries`() {
            val result = ReviewPacketFormatter.format(createReviewPacketData())
            result shouldContain "*No 1:1 meetings during this period.*"
        }

        @Test
        fun `should show meeting count`() {
            val entries = listOf(
                OneOnOneEntry(
                    id = OneOnOneEntryId.generate(), userId = userId, personId = personId,
                    meetingDate = Instant.parse("2024-03-01T10:00:00Z"),
                    agendaItems = emptyList(), sensitive = false
                ),
                OneOnOneEntry(
                    id = OneOnOneEntryId.generate(), userId = userId, personId = personId,
                    meetingDate = Instant.parse("2024-04-01T10:00:00Z"),
                    agendaItems = emptyList(), sensitive = false
                )
            )
            val result = ReviewPacketFormatter.format(createReviewPacketData(oneOnOneEntries = entries))
            result shouldContain "2 meeting(s) held during this period."
        }

        @Test
        fun `should include meeting notes`() {
            val entries = listOf(
                OneOnOneEntry(
                    id = OneOnOneEntryId.generate(), userId = userId, personId = personId,
                    meetingDate = Instant.parse("2024-03-01T10:00:00Z"),
                    agendaItems = emptyList(), sensitive = false,
                    notesMarkdown = "Discussed project progress"
                )
            )
            val result = ReviewPacketFormatter.format(createReviewPacketData(oneOnOneEntries = entries))
            result shouldContain "Discussed project progress"
        }

        @Test
        fun `should hide sensitive entry content`() {
            val entries = listOf(
                OneOnOneEntry(
                    id = OneOnOneEntryId.generate(), userId = userId, personId = personId,
                    meetingDate = Instant.parse("2024-03-01T10:00:00Z"),
                    agendaItems = emptyList(), sensitive = true,
                    notesMarkdown = "Secret content"
                )
            )
            val result = ReviewPacketFormatter.format(createReviewPacketData(oneOnOneEntries = entries))
            result shouldContain "*[Sensitive content — not included in review packet]*"
            result shouldNotContain "Secret content"
        }

        @Test
        fun `should include agenda items`() {
            val entries = listOf(
                OneOnOneEntry(
                    id = OneOnOneEntryId.generate(), userId = userId, personId = personId,
                    meetingDate = Instant.parse("2024-03-01T10:00:00Z"),
                    agendaItems = listOf(
                        AgendaItem(id = AgendaItemId.generate(), text = "Review goals", checked = true, displayOrder = 0),
                        AgendaItem(id = AgendaItemId.generate(), text = "Discuss blockers", checked = false, displayOrder = 1)
                    ),
                    sensitive = false
                )
            )
            val result = ReviewPacketFormatter.format(createReviewPacketData(oneOnOneEntries = entries))
            result shouldContain "- [x] Review goals"
            result shouldContain "- [ ] Discuss blockers"
        }

        @Test
        fun `should include outcomes`() {
            val entries = listOf(
                OneOnOneEntry(
                    id = OneOnOneEntryId.generate(), userId = userId, personId = personId,
                    meetingDate = Instant.parse("2024-03-01T10:00:00Z"),
                    agendaItems = emptyList(), sensitive = false,
                    outcomesMarkdown = "Decided to proceed with plan B"
                )
            )
            val result = ReviewPacketFormatter.format(createReviewPacketData(oneOnOneEntries = entries))
            result shouldContain "Decided to proceed with plan B"
        }
    }

    @Nested
    inner class ActionItemsSectionTests {
        @Test
        fun `should show empty message when no action items`() {
            val result = ReviewPacketFormatter.format(createReviewPacketData())
            result shouldContain "*No action items during this period.*"
        }

        @Test
        fun `should show completion rate`() {
            val items = listOf(
                ActionItem(
                    id = ActionItemId.generate(), userId = userId, personId = personId,
                    title = "Done", status = ActionItemStatus.DONE, createdAt = Instant.now()
                ),
                ActionItem(
                    id = ActionItemId.generate(), userId = userId, personId = personId,
                    title = "Open", status = ActionItemStatus.OPEN, createdAt = Instant.now()
                )
            )
            val result = ReviewPacketFormatter.format(createReviewPacketData(actionItems = items))
            result shouldContain "**Completion Rate:** 50%"
        }

        @Test
        fun `should group action items by status`() {
            val items = listOf(
                ActionItem(
                    id = ActionItemId.generate(), userId = userId, personId = personId,
                    title = "Completed task", status = ActionItemStatus.DONE, createdAt = Instant.now()
                ),
                ActionItem(
                    id = ActionItemId.generate(), userId = userId, personId = personId,
                    title = "Pending task", status = ActionItemStatus.OPEN, createdAt = Instant.now()
                ),
                ActionItem(
                    id = ActionItemId.generate(), userId = userId, personId = personId,
                    title = "Dropped task", status = ActionItemStatus.CANCELED, createdAt = Instant.now()
                )
            )
            val result = ReviewPacketFormatter.format(createReviewPacketData(actionItems = items))
            result shouldContain "### Completed"
            result shouldContain "### Still Open"
            result shouldContain "### Canceled"
            result shouldContain "✅ **Completed task**"
            result shouldContain "⬜ **Pending task**"
            result shouldContain "❌ **Dropped task**"
        }

        @Test
        fun `should include due date when present`() {
            val items = listOf(
                ActionItem(
                    id = ActionItemId.generate(), userId = userId, personId = personId,
                    title = "Task with due date", status = ActionItemStatus.OPEN,
                    dueDate = LocalDate.of(2024, 4, 15), createdAt = Instant.now()
                )
            )
            val result = ReviewPacketFormatter.format(createReviewPacketData(actionItems = items))
            result shouldContain "due 2024-04-15"
        }

        @Test
        fun `should include owner type`() {
            val items = listOf(
                ActionItem(
                    id = ActionItemId.generate(), userId = userId, personId = personId,
                    title = "Manager task", status = ActionItemStatus.OPEN,
                    ownerType = ActionItemOwnerType.MANAGER, createdAt = Instant.now()
                )
            )
            val result = ReviewPacketFormatter.format(createReviewPacketData(actionItems = items))
            result shouldContain "(manager)"
        }
    }

    @Nested
    inner class PdpSectionTests {
        @Test
        fun `should show empty message when no PDP goals`() {
            val result = ReviewPacketFormatter.format(createReviewPacketData())
            result shouldContain "*No PDP goals during this period.*"
        }

        @Test
        fun `should show PDP summary`() {
            val goals = listOf(
                PdpGoalWithUpdates(
                    goal = PdpGoal(
                        id = PdpGoalId.generate(), userId = userId, personId = personId,
                        title = "Learn Kotlin", status = PdpGoalStatus.ACHIEVED, createdAt = Instant.now()
                    ),
                    updates = emptyList()
                ),
                PdpGoalWithUpdates(
                    goal = PdpGoal(
                        id = PdpGoalId.generate(), userId = userId, personId = personId,
                        title = "Lead project", status = PdpGoalStatus.ACTIVE, createdAt = Instant.now()
                    ),
                    updates = emptyList()
                )
            )
            val result = ReviewPacketFormatter.format(createReviewPacketData(pdpGoals = goals))
            result shouldContain "2 goal(s)"
            result shouldContain "1 achieved"
            result shouldContain "1 active"
        }

        @Test
        fun `should show goal with status badge`() {
            val goals = listOf(
                PdpGoalWithUpdates(
                    goal = PdpGoal(
                        id = PdpGoalId.generate(), userId = userId, personId = personId,
                        title = "Learn Kotlin", status = PdpGoalStatus.ACHIEVED, createdAt = Instant.now()
                    ),
                    updates = emptyList()
                )
            )
            val result = ReviewPacketFormatter.format(createReviewPacketData(pdpGoals = goals))
            result shouldContain "### Learn Kotlin [✅ ACHIEVED]"
        }

        @Test
        fun `should include goal description`() {
            val goals = listOf(
                PdpGoalWithUpdates(
                    goal = PdpGoal(
                        id = PdpGoalId.generate(), userId = userId, personId = personId,
                        title = "Learn Kotlin", description = "Complete Kotlin course",
                        status = PdpGoalStatus.ACTIVE, createdAt = Instant.now()
                    ),
                    updates = emptyList()
                )
            )
            val result = ReviewPacketFormatter.format(createReviewPacketData(pdpGoals = goals))
            result shouldContain "Complete Kotlin course"
        }

        @Test
        fun `should include progress updates`() {
            val goals = listOf(
                PdpGoalWithUpdates(
                    goal = PdpGoal(
                        id = PdpGoalId.generate(), userId = userId, personId = personId,
                        title = "Learn Kotlin", status = PdpGoalStatus.ACTIVE, createdAt = Instant.now()
                    ),
                    updates = listOf(
                        PdpUpdate(
                            id = PdpUpdateId.generate(),
                            goalId = PdpGoalId.generate(),
                            userId = userId,
                            textMarkdown = "Completed chapter 5",
                            sensitive = false,
                            createdAt = Instant.parse("2024-03-15T10:00:00Z")
                        )
                    )
                )
            )
            val result = ReviewPacketFormatter.format(createReviewPacketData(pdpGoals = goals))
            result shouldContain "Completed chapter 5"
        }

        @Test
        fun `should hide sensitive progress updates`() {
            val goals = listOf(
                PdpGoalWithUpdates(
                    goal = PdpGoal(
                        id = PdpGoalId.generate(), userId = userId, personId = personId,
                        title = "Learn Kotlin", status = PdpGoalStatus.ACTIVE, createdAt = Instant.now()
                    ),
                    updates = listOf(
                        PdpUpdate(
                            id = PdpUpdateId.generate(),
                            goalId = PdpGoalId.generate(),
                            userId = userId,
                            textMarkdown = "Private update content",
                            sensitive = true,
                            createdAt = Instant.parse("2024-03-15T10:00:00Z")
                        )
                    )
                )
            )
            val result = ReviewPacketFormatter.format(createReviewPacketData(pdpGoals = goals))
            result shouldContain "*[Sensitive content]*"
            result shouldNotContain "Private update content"
        }
    }

    @Nested
    inner class KudosSectionTests {
        @Test
        fun `should show empty message when no kudos`() {
            val result = ReviewPacketFormatter.format(createReviewPacketData())
            result shouldContain "*No kudos recorded during this period.*"
        }

        @Test
        fun `should show kudos count`() {
            val kudos = listOf(
                Kudos(
                    id = KudosId.generate(), userId = userId, personId = personId,
                    date = LocalDate.of(2024, 3, 1), text = "Great presentation", tags = emptyList()
                ),
                Kudos(
                    id = KudosId.generate(), userId = userId, personId = personId,
                    date = LocalDate.of(2024, 4, 1), text = "Helped onboard new hire", tags = emptyList()
                )
            )
            val result = ReviewPacketFormatter.format(createReviewPacketData(kudos = kudos))
            result shouldContain "2 kudos recorded."
        }

        @Test
        fun `should show tag summary`() {
            val kudos = listOf(
                Kudos(
                    id = KudosId.generate(), userId = userId, personId = personId,
                    date = LocalDate.of(2024, 3, 1), text = "Great work",
                    tags = listOf("impact", "collaboration")
                ),
                Kudos(
                    id = KudosId.generate(), userId = userId, personId = personId,
                    date = LocalDate.of(2024, 4, 1), text = "Nice job",
                    tags = listOf("impact")
                )
            )
            val result = ReviewPacketFormatter.format(createReviewPacketData(kudos = kudos))
            result shouldContain "**Tags:**"
            result shouldContain "impact (2)"
            result shouldContain "collaboration (1)"
        }

        @Test
        fun `should include kudos entries with dates and tags`() {
            val kudos = listOf(
                Kudos(
                    id = KudosId.generate(), userId = userId, personId = personId,
                    date = LocalDate.of(2024, 3, 1), text = "Great presentation",
                    tags = listOf("impact")
                )
            )
            val result = ReviewPacketFormatter.format(createReviewPacketData(kudos = kudos))
            result shouldContain "**2024-03-01** [impact]"
            result shouldContain "Great presentation"
        }
    }

    @Nested
    inner class FooterTests {
        @Test
        fun `should include generation date and period in footer`() {
            val result = ReviewPacketFormatter.format(createReviewPacketData())
            result shouldContain "Review packet generated on"
            result shouldContain "for period 2024-01-01 to 2024-06-30"
        }
    }
}
