package com.itsjeel01.remotevcsmanager.ui

import com.itsjeel01.remotevcsmanager.models.Issue
import com.itsjeel01.remotevcsmanager.models.IssueState
import kotlin.test.Test
import kotlin.test.assertEquals

class IssueDependencyDisplayTest {

    @Test
    fun includesRepositoryForCrossRepositoryDependencies(): Unit {
        assertEquals("octo/repo#23", issueReference(issue("https://github.com/octo/repo/issues/23")))
        assertEquals("#23", issueReference(issue("not a URL")))
    }

    private fun issue(url: String): Issue =
        Issue(
            id = "issue-23",
            number = 23,
            title = "Accept design",
            body = null,
            state = IssueState.CLOSED,
            url = url,
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
