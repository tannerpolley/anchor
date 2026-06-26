package com.itsjeel01.remotevcsmanager.ui

import com.itsjeel01.remotevcsmanager.models.Issue
import com.itsjeel01.remotevcsmanager.models.IssueState
import kotlin.test.Test
import kotlin.test.assertEquals

class IssueMilestoneGroupingTest {

    @Test
    fun groupsIssuesByMilestoneWithNoMilestoneLast(): Unit {
        val grouped = IssueMilestoneGrouping.group(
            listOf(
                issueFixture(number = 1, milestone = "Beta"),
                issueFixture(number = 2, milestone = null),
                issueFixture(number = 3, milestone = "Alpha")
            )
        )

        assertEquals(listOf("Alpha", "Beta", "No Milestone"), grouped.map { it.title })
        assertEquals(listOf(3), grouped[0].issues.map { it.number })
        assertEquals(listOf(2), grouped[2].issues.map { it.number })
    }

    @Test
    fun preservesIssueOrderInsideMilestoneGroups(): Unit {
        val grouped = IssueMilestoneGrouping.group(
            listOf(
                issueFixture(number = 7, milestone = "Beta"),
                issueFixture(number = 5, milestone = "Beta"),
                issueFixture(number = 3, milestone = "Beta")
            )
        )

        assertEquals(listOf(7, 5, 3), grouped.single().issues.map { it.number })
    }

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
