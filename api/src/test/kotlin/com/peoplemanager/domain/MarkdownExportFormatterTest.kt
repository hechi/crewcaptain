package com.peoplemanager.domain

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate

class MarkdownExportFormatterTest {

    private val userId = UserId.generate()
    private val personId = PersonId.generate()

    private fun createPerson(
        name: String = "Jane Smith",
        preferredName: String? = "Jane",
        roleTitle: String? = "Senior Engineer",
        timezone: String? = "Europe/Berlin",
        startDate: LocalDate? = LocalDate.of(2023, 3, 15),
        email: String? = "jane@example.com",
        tags: List<String> = listOf("engineering", "senior"),
        moraleStatus: MoraleStatus = MoraleStatus.GREEN,
        moraleNote: String? = "Doing great",
        pinnedRememberItems: List<PinnedRememberItem> = emptyList()
    ) = Person(
        id = personId,
        userId = userId,
        name = name,
        preferredName = preferredName,
        roleTitle = roleTitle,
        timezone = timezone,
        startDate = startDate,
        email = email,
        tags = tags,
        moraleStatus = moraleStatus,
        moraleNote = moraleNote,
        pinnedRememberItems = pinnedRememberItems
    )

    private fun createExportData(
        person: Person = createPerson(),
        oneOnOneEntries: List<OneOnOneEntry> = emptyList(),
        actionItems: List<ActionItem> = emptyList(),
        pdpGoals: List<PdpGoalWithUpdates> = emptyList(),
        kudos: List<Kudos> = emptyList(),
        dateFrom: LocalDate? = null,
        dateTo: LocalDate? = null
    ) = PersonExportData(
        person = person,
        oneOnOneEntries = oneOnOneEntries,
        actionItems = actionItems,
        pdpGoals = pdpGoals,
        kudos = kudos,
        dateFrom = dateFrom,
        dateTo = dateTo
    )

    @Nested
    inner class HeaderTests {
        @Test
        fun `should include person name as h1 header`() {
            val result = MarkdownExportFormatter.format(createExportData())
            result shouldContain "# Jane Smith"
        }

        @Test
        fun `should include date range when dateFrom and dateTo are specified`() {
            val data = createExportData(
                dateFrom = LocalDate.of(2024, 1, 1),
                dateTo = LocalDate.of(2024, 6, 30)
            )
            val result = MarkdownExportFormatter.format(data)
            result shouldContain "Export date range: 2024-01-01 to 2024-06-30"
        }

        @Test
        fun `should show beginning when only dateTo is specified`() {
            val data = createExportData(dateTo = LocalDate.of(2024, 6, 30))
            val result = MarkdownExportFormatter.format(data)
            result shouldContain "Export date range: beginning to 2024-06-30"
        }

        @Test
        fun `should show present when only dateFrom is specified`() {
            val data = createExportData(dateFrom = LocalDate.of(2024, 1, 1))
            val result = MarkdownExportFormatter.format(data)
            result shouldContain "Export date range: 2024-01-01 to present"
        }

        @Test
        fun `should not include date range when neither dateFrom nor dateTo specified`() {
            val result = MarkdownExportFormatter.format(createExportData())
            result shouldNotContain "Export date range"
        }
    }

    @Nested
    inner class ProfileTests {
        @Test
        fun `should include profile section with all fields`() {
            val result = MarkdownExportFormatter.format(createExportData())
            result shouldContain "## Profile"
            result shouldContain "| Name | Jane Smith |"
            result shouldContain "| Preferred Name | Jane |"
            result shouldContain "| Role/Title | Senior Engineer |"
            result shouldContain "| Timezone | Europe/Berlin |"
            result shouldContain "| Start Date | 2023-03-15 |"
            result shouldContain "| Email | jane@example.com |"
            result shouldContain "| Tags | engineering, senior |"
        }

        @Test
        fun `should omit optional fields when null`() {
            val person = createPerson(
                preferredName = null,
                roleTitle = null,
                timezone = null,
                startDate = null,
                email = null,
                tags = emptyList()
            )
            val result = MarkdownExportFormatter.format(createExportData(person = person))
            result shouldNotContain "Preferred Name"
            result shouldNotContain "Role/Title"
            result shouldNotContain "Timezone"
            result shouldNotContain "Start Date"
            result shouldNotContain "Email"
            result shouldNotContain "Tags"
        }
    }

    @Nested
    inner class PinnedRememberItemsTests {
        @Test
        fun `should include pinned remember items`() {
            val items = listOf(
                PinnedRememberItem(RememberItemId.generate(), "Prefers async communication", 0, Instant.now()),
                PinnedRememberItem(RememberItemId.generate(), "Has a dog named Max", 1, Instant.now())
            )
            val person = createPerson(pinnedRememberItems = items)
            val result = MarkdownExportFormatter.format(createExportData(person = person))
            result shouldContain "## Pinned Remember Items"
            result shouldContain "- Prefers async communication"
            result shouldContain "- Has a dog named Max"
        }

        @Test
        fun `should not include section when no remember items`() {
            val result = MarkdownExportFormatter.format(createExportData())
            result shouldNotContain "## Pinned Remember Items"
        }
    }

    @Nested
    inner class MoraleTests {
        @Test
        fun `should include morale status and note`() {
            val result = MarkdownExportFormatter.format(createExportData())
            result shouldContain "## Morale"
            result shouldContain "**Status:** GREEN"
            result shouldContain "**Note:** Doing great"
        }

        @Test
        fun `should omit note when null`() {
            val person = createPerson(moraleNote = null)
            val result = MarkdownExportFormatter.format(createExportData(person = person))
            result shouldContain "**Status:** GREEN"
            result shouldNotContain "**Note:**"
        }
    }

    @Nested
    inner class OneOnOneHistoryTests {
        @Test
        fun `should include 1-1 entries with agenda and notes`() {
            val entry = OneOnOneEntry(
                id = OneOnOneEntryId.generate(),
                userId = userId,
                personId = personId,
                meetingDate = Instant.parse("2024-03-15T10:00:00Z"),
                agendaItems = listOf(
                    AgendaItem(AgendaItemId.generate(), "Discuss project status", true, 0),
                    AgendaItem(AgendaItemId.generate(), "Review goals", false, 1)
                ),
                notesMarkdown = "Good progress on the project.",
                outcomesMarkdown = "Agreed to extend deadline by 1 week.",
                sensitive = false
            )
            val result = MarkdownExportFormatter.format(createExportData(oneOnOneEntries = listOf(entry)))
            result shouldContain "## 1:1 History"
            result shouldContain "### 2024-03-15 10:00"
            result shouldContain "- [x] Discuss project status"
            result shouldContain "- [ ] Review goals"
            result shouldContain "Good progress on the project."
            result shouldContain "Agreed to extend deadline by 1 week."
        }

        @Test
        fun `should hide notes and outcomes for sensitive entries`() {
            val entry = OneOnOneEntry(
                id = OneOnOneEntryId.generate(),
                userId = userId,
                personId = personId,
                meetingDate = Instant.parse("2024-03-15T10:00:00Z"),
                agendaItems = emptyList(),
                notesMarkdown = "Secret notes",
                outcomesMarkdown = "Secret outcomes",
                sensitive = true
            )
            val result = MarkdownExportFormatter.format(createExportData(oneOnOneEntries = listOf(entry)))
            result shouldContain "[Sensitive content]"
            result shouldNotContain "Secret notes"
            result shouldNotContain "Secret outcomes"
        }

        @Test
        fun `should show empty state when no entries`() {
            val result = MarkdownExportFormatter.format(createExportData())
            result shouldContain "## 1:1 History"
            result shouldContain "*No 1:1 entries recorded.*"
        }
    }

    @Nested
    inner class ActionItemTests {
        @Test
        fun `should group action items by status`() {
            val items = listOf(
                ActionItem(
                    id = ActionItemId.generate(), userId = userId, personId = personId,
                    title = "Open task", ownerType = ActionItemOwnerType.MANAGER,
                    dueDate = LocalDate.of(2024, 4, 1), status = ActionItemStatus.OPEN
                ),
                ActionItem(
                    id = ActionItemId.generate(), userId = userId, personId = personId,
                    title = "Done task", ownerType = ActionItemOwnerType.PERSON,
                    status = ActionItemStatus.DONE
                ),
                ActionItem(
                    id = ActionItemId.generate(), userId = userId, personId = personId,
                    title = "Canceled task", ownerType = ActionItemOwnerType.MANAGER,
                    status = ActionItemStatus.CANCELED
                )
            )
            val result = MarkdownExportFormatter.format(createExportData(actionItems = items))
            result shouldContain "### Open"
            result shouldContain "- [ ] **Open task** (manager) — due 2024-04-01"
            result shouldContain "### Done"
            result shouldContain "- [x] **Done task** (person)"
            result shouldContain "### Canceled"
            result shouldContain "- [-] **Canceled task** (manager)"
        }

        @Test
        fun `should include description when present`() {
            val item = ActionItem(
                id = ActionItemId.generate(), userId = userId, personId = personId,
                title = "Task with desc", description = "Some details here",
                ownerType = ActionItemOwnerType.MANAGER, status = ActionItemStatus.OPEN
            )
            val result = MarkdownExportFormatter.format(createExportData(actionItems = listOf(item)))
            result shouldContain "Some details here"
        }

        @Test
        fun `should show empty state when no action items`() {
            val result = MarkdownExportFormatter.format(createExportData())
            result shouldContain "## Action Items"
            result shouldContain "*No action items.*"
        }
    }

    @Nested
    inner class PdpGoalTests {
        @Test
        fun `should include PDP goals with status and updates`() {
            val goalId = PdpGoalId.generate()
            val goal = PdpGoal(
                id = goalId, userId = userId, personId = personId,
                title = "Learn Kotlin", description = "Master Kotlin for backend development",
                targetDate = LocalDate.of(2024, 12, 31), status = PdpGoalStatus.ACTIVE
            )
            val updates = listOf(
                PdpUpdate(
                    id = PdpUpdateId.generate(), goalId = goalId, userId = userId,
                    textMarkdown = "Completed Kotlin basics course",
                    createdAt = Instant.parse("2024-03-01T10:00:00Z")
                ),
                PdpUpdate(
                    id = PdpUpdateId.generate(), goalId = goalId, userId = userId,
                    textMarkdown = "Started Spring Boot project",
                    createdAt = Instant.parse("2024-04-01T10:00:00Z")
                )
            )
            val data = createExportData(pdpGoals = listOf(PdpGoalWithUpdates(goal, updates)))
            val result = MarkdownExportFormatter.format(data)
            result shouldContain "### Learn Kotlin [ACTIVE]"
            result shouldContain "Master Kotlin for backend development"
            result shouldContain "**Target date:** 2024-12-31"
            result shouldContain "Completed Kotlin basics course"
            result shouldContain "Started Spring Boot project"
        }

        @Test
        fun `should hide sensitive PDP updates`() {
            val goalId = PdpGoalId.generate()
            val goal = PdpGoal(
                id = goalId, userId = userId, personId = personId,
                title = "Goal", status = PdpGoalStatus.ACTIVE
            )
            val updates = listOf(
                PdpUpdate(
                    id = PdpUpdateId.generate(), goalId = goalId, userId = userId,
                    textMarkdown = "Secret update", sensitive = true,
                    createdAt = Instant.parse("2024-03-01T10:00:00Z")
                )
            )
            val data = createExportData(pdpGoals = listOf(PdpGoalWithUpdates(goal, updates)))
            val result = MarkdownExportFormatter.format(data)
            result shouldContain "[Sensitive content]"
            result shouldNotContain "Secret update"
        }

        @Test
        fun `should show empty state when no PDP goals`() {
            val result = MarkdownExportFormatter.format(createExportData())
            result shouldContain "## PDP Goals"
            result shouldContain "*No PDP goals.*"
        }
    }

    @Nested
    inner class KudosTests {
        @Test
        fun `should include kudos with date and tags`() {
            val kudosList = listOf(
                Kudos(
                    id = KudosId.generate(), userId = userId, personId = personId,
                    date = LocalDate.of(2024, 3, 15), text = "Great presentation!",
                    tags = listOf("impact", "communication")
                ),
                Kudos(
                    id = KudosId.generate(), userId = userId, personId = personId,
                    date = LocalDate.of(2024, 2, 10), text = "Helped onboard new team member"
                )
            )
            val result = MarkdownExportFormatter.format(createExportData(kudos = kudosList))
            result shouldContain "## Kudos"
            result shouldContain "**2024-03-15** (impact, communication) — Great presentation!"
            result shouldContain "**2024-02-10** — Helped onboard new team member"
        }

        @Test
        fun `should show empty state when no kudos`() {
            val result = MarkdownExportFormatter.format(createExportData())
            result shouldContain "## Kudos"
            result shouldContain "*No kudos recorded.*"
        }
    }

    @Nested
    inner class FooterTests {
        @Test
        fun `should include export date in footer`() {
            val result = MarkdownExportFormatter.format(createExportData())
            result shouldContain "*Exported on ${LocalDate.now()}*"
        }
    }

    @Nested
    inner class FullExportTests {
        @Test
        fun `should produce a complete well-structured export`() {
            val items = listOf(
                PinnedRememberItem(RememberItemId.generate(), "Likes coffee", 0, Instant.now())
            )
            val person = createPerson(pinnedRememberItems = items)
            val entry = OneOnOneEntry(
                id = OneOnOneEntryId.generate(), userId = userId, personId = personId,
                meetingDate = Instant.parse("2024-03-15T10:00:00Z"),
                agendaItems = listOf(AgendaItem(AgendaItemId.generate(), "Check in", false, 0)),
                notesMarkdown = "All good"
            )
            val actionItem = ActionItem(
                id = ActionItemId.generate(), userId = userId, personId = personId,
                title = "Follow up", ownerType = ActionItemOwnerType.MANAGER,
                status = ActionItemStatus.OPEN
            )
            val goalId = PdpGoalId.generate()
            val goal = PdpGoalWithUpdates(
                goal = PdpGoal(id = goalId, userId = userId, personId = personId, title = "Grow", status = PdpGoalStatus.ACTIVE),
                updates = listOf(PdpUpdate(id = PdpUpdateId.generate(), goalId = goalId, userId = userId, textMarkdown = "Progress", createdAt = Instant.now()))
            )
            val kudos = Kudos(
                id = KudosId.generate(), userId = userId, personId = personId,
                date = LocalDate.of(2024, 3, 1), text = "Well done!"
            )

            val data = createExportData(
                person = person,
                oneOnOneEntries = listOf(entry),
                actionItems = listOf(actionItem),
                pdpGoals = listOf(goal),
                kudos = listOf(kudos)
            )
            val result = MarkdownExportFormatter.format(data)

            // Verify section ordering
            val profileIdx = result.indexOf("## Profile")
            val rememberIdx = result.indexOf("## Pinned Remember Items")
            val moraleIdx = result.indexOf("## Morale")
            val oneOnOneIdx = result.indexOf("## 1:1 History")
            val actionIdx = result.indexOf("## Action Items")
            val pdpIdx = result.indexOf("## PDP Goals")
            val kudosIdx = result.indexOf("## Kudos")

            assert(profileIdx < rememberIdx) { "Profile should come before Remember Items" }
            assert(rememberIdx < moraleIdx) { "Remember Items should come before Morale" }
            assert(moraleIdx < oneOnOneIdx) { "Morale should come before 1:1 History" }
            assert(oneOnOneIdx < actionIdx) { "1:1 History should come before Action Items" }
            assert(actionIdx < pdpIdx) { "Action Items should come before PDP Goals" }
            assert(pdpIdx < kudosIdx) { "PDP Goals should come before Kudos" }
        }
    }
}
