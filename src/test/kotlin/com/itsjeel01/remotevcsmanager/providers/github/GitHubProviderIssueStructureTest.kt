package com.itsjeel01.remotevcsmanager.providers.github

import com.google.gson.JsonParser
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
}
