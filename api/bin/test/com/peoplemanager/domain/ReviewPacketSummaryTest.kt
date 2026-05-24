package com.peoplemanager.domain

import io.kotest.matchers.doubles.shouldBeExactly
import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.maps.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate

class ReviewPacketSummaryTest {

    private val userId = UserId.generate()
    private val personId = PersonId.generate()

    private fun createActionItem(
        status: ActionItemStatus = ActionItemStatus.OPEN,
        title: String = "Test item"
    ) = ActionItem(
        id = ActionItemId.generate(),
        userId = userId,
        personId = personId,
        title = title,
        status = status,
        createdAt = Instant.now()
    )

    private fun createOneOnOneEntry() = OneOnOneEntry(
        id = OneOnOneEntryId.generate(),
        userId = userId,
        personId = personId,
        meetingDate = Instant.now(),
        agendaItems = emptyList(),
        notesMarkdown = null,
        outcomesMarkdown = null,
        sensitive = false
    )

    private fun createPdpGoalWithUpdates(
        status: PdpGoalStatus = PdpGoalStatus.ACTIVE
    ) = PdpGoalWithUpdates(
        goal = PdpGoal(
            id = PdpGoalId.generate(),
            userId = userId,
            personId = personId,
            title = "Test goal",
            status = status,
            createdAt = Instant.now()
        ),
        updates = emptyList()
    )

    private fun createKudos(
        tags: List<String> = emptyList()
    ) = Kudos(
        id = KudosId.generate(),
        userId = userId,
        personId = personId,
        date = LocalDate.now(),
        text = "Great work!",
        tags = tags
    )

    @Nested
    inner class ComputeTests {

        @Test
        fun `should compute zero values for empty data`() {
            val summary = ReviewPacketSummary.compute(
                oneOnOneEntries = emptyList(),
                actionItems = emptyList(),
                pdpGoals = emptyList(),
                kudos = emptyList()
            )

            summary.totalOneOnOnes shouldBe 0
            summary.totalActionItems shouldBe 0
            summary.actionItemsCompleted shouldBe 0
            summary.actionItemsCanceled shouldBe 0
            summary.actionItemsOpen shouldBe 0
            summary.actionItemCompletionRate shouldBeExactly 0.0
            summary.totalPdpGoals shouldBe 0
            summary.pdpGoalsAchieved shouldBe 0
            summary.pdpGoalsActive shouldBe 0
            summary.pdpGoalsPaused shouldBe 0
            summary.pdpGoalsDropped shouldBe 0
            summary.totalKudos shouldBe 0
            summary.kudosTagSummary.shouldBeEmpty()
        }

        @Test
        fun `should count one-on-one entries`() {
            val entries = listOf(createOneOnOneEntry(), createOneOnOneEntry(), createOneOnOneEntry())
            val summary = ReviewPacketSummary.compute(entries, emptyList(), emptyList(), emptyList())

            summary.totalOneOnOnes shouldBe 3
        }

        @Test
        fun `should count action items by status`() {
            val items = listOf(
                createActionItem(status = ActionItemStatus.DONE),
                createActionItem(status = ActionItemStatus.DONE),
                createActionItem(status = ActionItemStatus.OPEN),
                createActionItem(status = ActionItemStatus.CANCELED)
            )
            val summary = ReviewPacketSummary.compute(emptyList(), items, emptyList(), emptyList())

            summary.totalActionItems shouldBe 4
            summary.actionItemsCompleted shouldBe 2
            summary.actionItemsOpen shouldBe 1
            summary.actionItemsCanceled shouldBe 1
        }

        @Test
        fun `should compute action item completion rate`() {
            val items = listOf(
                createActionItem(status = ActionItemStatus.DONE),
                createActionItem(status = ActionItemStatus.DONE),
                createActionItem(status = ActionItemStatus.OPEN),
                createActionItem(status = ActionItemStatus.CANCELED)
            )
            val summary = ReviewPacketSummary.compute(emptyList(), items, emptyList(), emptyList())

            summary.actionItemCompletionRate shouldBeExactly 0.5
        }

        @Test
        fun `should compute zero completion rate when no action items`() {
            val summary = ReviewPacketSummary.compute(emptyList(), emptyList(), emptyList(), emptyList())

            summary.actionItemCompletionRate shouldBeExactly 0.0
        }

        @Test
        fun `should count PDP goals by status`() {
            val goals = listOf(
                createPdpGoalWithUpdates(status = PdpGoalStatus.ACHIEVED),
                createPdpGoalWithUpdates(status = PdpGoalStatus.ACTIVE),
                createPdpGoalWithUpdates(status = PdpGoalStatus.ACTIVE),
                createPdpGoalWithUpdates(status = PdpGoalStatus.PAUSED),
                createPdpGoalWithUpdates(status = PdpGoalStatus.DROPPED)
            )
            val summary = ReviewPacketSummary.compute(emptyList(), emptyList(), goals, emptyList())

            summary.totalPdpGoals shouldBe 5
            summary.pdpGoalsAchieved shouldBe 1
            summary.pdpGoalsActive shouldBe 2
            summary.pdpGoalsPaused shouldBe 1
            summary.pdpGoalsDropped shouldBe 1
        }

        @Test
        fun `should count kudos`() {
            val kudos = listOf(createKudos(), createKudos(), createKudos())
            val summary = ReviewPacketSummary.compute(emptyList(), emptyList(), emptyList(), kudos)

            summary.totalKudos shouldBe 3
        }

        @Test
        fun `should compute kudos tag summary`() {
            val kudos = listOf(
                createKudos(tags = listOf("impact", "collaboration")),
                createKudos(tags = listOf("impact", "leadership")),
                createKudos(tags = listOf("collaboration"))
            )
            val summary = ReviewPacketSummary.compute(emptyList(), emptyList(), emptyList(), kudos)

            summary.kudosTagSummary shouldContainExactly mapOf(
                "collaboration" to 2,
                "impact" to 2,
                "leadership" to 1
            )
        }

        @Test
        fun `should handle kudos with no tags`() {
            val kudos = listOf(createKudos(tags = emptyList()))
            val summary = ReviewPacketSummary.compute(emptyList(), emptyList(), emptyList(), kudos)

            summary.kudosTagSummary.shouldBeEmpty()
        }

        @Test
        fun `should compute 100 percent completion rate when all items done`() {
            val items = listOf(
                createActionItem(status = ActionItemStatus.DONE),
                createActionItem(status = ActionItemStatus.DONE)
            )
            val summary = ReviewPacketSummary.compute(emptyList(), items, emptyList(), emptyList())

            summary.actionItemCompletionRate shouldBeExactly 1.0
        }
    }
}
