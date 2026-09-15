package com.itsjeel01.remotevcsmanager.providers.github

import com.google.gson.JsonParser
import com.itsjeel01.remotevcsmanager.models.Issue
import com.itsjeel01.remotevcsmanager.models.IssueState
import kotlin.test.Test
import kotlin.test.assertEquals

class GitHubProviderIssueStructureTest {

    @Test
    fun parsesMilestonePayload(): Unit {
        val json = JsonParser.parseString(
            """
            {
              "id": 1001,
              "title": "M1 - Issue Workflow Hardening",
              "open_issues": 4,
              "state": "open"
            }
            """.trimIndent()
        ).asJsonObject

        val milestone = GitHubIssueStructureParser.toMilestone(json)

        assertEquals("1001", milestone.id)
        assertEquals("M1 - Issue Workflow Hardening", milestone.title)
        assertEquals(4, milestone.openIssueCount)
        assertEquals("open", milestone.state)
    }

    @Test
    fun parsesSubIssueRelationshipsForParentIssue(): Unit {
        val json = JsonParser.parseString(
            """
            [
              { "number": 11, "title": "Child one" },
              { "number": 12, "title": "Child two" }
            ]
            """.trimIndent()
        ).asJsonArray

        val relationships = GitHubIssueStructureParser.toIssueRelationships(
            parentIssueNumber = 7,
            subIssues = json
        )

        assertEquals(listOf(7 to 11, 7 to 12), relationships.map {
            it.parentIssueNumber to it.childIssueNumber
        })
    }

    @Test
    fun parsesClosedBlockingIssueWithoutDroppingTheDependency(): Unit {
        val dependency = GitHubIssueStructureParser.toIssueDependency(
            blockedIssueNumber = 26,
            blockingIssue = issue(number = 23, state = IssueState.CLOSED)
        )

        assertEquals(26, dependency.blockedIssueNumber)
        assertEquals(23, dependency.blockingIssue.number)
        assertEquals("Accept design", dependency.blockingIssue.title)
        assertEquals(IssueState.CLOSED, dependency.blockingIssue.state)
    }

    private fun issue(number: Int, state: IssueState): Issue =
        Issue(
            id = "issue-$number",
            number = number,
            title = "Accept design",
            body = null,
            state = state,
            url = "https://github.com/octo/repo/issues/$number",
            author = "octo",
            assignees = emptyList(),
            labels = emptyList(),
            commentsCount = 0,
            createdAt = "2026-09-01T00:00:00Z",
            updatedAt = "2026-09-02T00:00:00Z",
            isPullRequest = false,
            provider = "github"
        )
}
