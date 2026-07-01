package com.itsjeel01.remotevcsmanager.ui

import com.itsjeel01.remotevcsmanager.models.Issue
import com.itsjeel01.remotevcsmanager.models.IssueMilestone
import com.itsjeel01.remotevcsmanager.models.IssueRelationship
import com.itsjeel01.remotevcsmanager.models.IssueState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class IssueTreeGroupingTest {

    @Test
    fun includesEmptyMilestonesAndOnlyAddsNoMilestoneWhenUnassignedIssuesExist(): Unit {
        val groups = IssueTreeGrouping.group(
            milestones = listOf(
                milestoneFixture(title = "M1 - First", openIssueCount = 1),
                milestoneFixture(title = "M2 - Empty", openIssueCount = 0)
            ),
            issues = listOf(
                issueFixture(number = 1, milestone = "M1 - First"),
                issueFixture(number = 2, milestone = null)
            ),
            relationships = emptyList()
        )

        assertEquals(listOf("M1 - First", "M2 - Empty", IssueTreeGrouping.NO_MILESTONE), groups.map { it.title })
        assertEquals(listOf(1, 0, 1), groups.map { it.openIssueCount })
        assertEquals(listOf(1), groups[0].rows.map { it.issue.number })
        assertEquals(emptyList(), groups[1].rows)
        assertEquals(listOf(2), groups[2].rows.map { it.issue.number })
    }

    @Test
    fun omitsNoMilestoneWhenEveryIssueHasAMilestone(): Unit {
        val groups = IssueTreeGrouping.group(
            milestones = listOf(
                milestoneFixture(title = "M1 - First", openIssueCount = 1),
                milestoneFixture(title = "M2 - Empty", openIssueCount = 0)
            ),
            issues = listOf(issueFixture(number = 1, milestone = "M1 - First")),
            relationships = emptyList()
        )

        assertEquals(listOf("M1 - First", "M2 - Empty"), groups.map { it.title })
    }

    @Test
    fun nestsSubIssuesUnderParentIssuesWithoutStandaloneDuplicates(): Unit {
        val groups = IssueTreeGrouping.group(
            milestones = listOf(milestoneFixture(title = "M1 - First", openIssueCount = 4)),
            issues = listOf(
                issueFixture(number = 8, milestone = "M1 - First"),
                issueFixture(number = 5, milestone = "M1 - First"),
                issueFixture(number = 6, milestone = "M1 - First"),
                issueFixture(number = 3, milestone = "M1 - First")
            ),
            relationships = listOf(
                IssueRelationship(parentIssueNumber = 5, childIssueNumber = 6),
                IssueRelationship(parentIssueNumber = 5, childIssueNumber = 3)
            )
        )

        val rows = groups.single().rows
        assertEquals(listOf(8, 5), rows.map { it.issue.number })
        assertIs<IssueTreeGrouping.IssueRow.Standalone>(rows[0])
        val parent = assertIs<IssueTreeGrouping.IssueRow.Parent>(rows[1])
        assertEquals(5, parent.issue.number)
        assertEquals(listOf(6, 3), parent.children.map { it.number })
    }

    private fun milestoneFixture(title: String, openIssueCount: Int): IssueMilestone =
        IssueMilestone(
            id = title,
            title = title,
            openIssueCount = openIssueCount,
            state = "open"
        )

    private fun issueFixture(number: Int, milestone: String?): Issue =
        Issue(
            id = "issue-$number",
            number = number,
            title = "Issue $number",
            body = null,
            state = IssueState.OPEN,
            url = "https://github.com/tannerpolley/anchor/issues/$number",
            author = "tannerpolley",
            assignees = emptyList(),
            labels = emptyList(),
            commentsCount = 0,
            createdAt = "2026-06-25T00:00:00Z",
            updatedAt = "2026-06-26T00:00:00Z",
            isPullRequest = false,
            provider = "github",
            milestone = milestone
        )
}
