package com.itsjeel01.remotevcsmanager.ui.editor

import com.itsjeel01.remotevcsmanager.models.Issue
import com.itsjeel01.remotevcsmanager.models.IssueComment
import com.itsjeel01.remotevcsmanager.models.IssueState
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class IssueEditorPreviewOpenerTest {

    @Test
    fun loadingPayloadUsesIssueTitle(): Unit {
        val payload = loadingPayload(issue())

        assertEquals("#12 Render issue previews", payload.title)
        assertContains(payload.html, "Loading issue details")
    }

    @Test
    fun issuePayloadRendersBodyAndComments(): Unit {
        val payload = issuePayload(
            issue = issue(),
            comments = listOf(comment()),
            renderedBody = "<p>Rendered body</p>",
            renderedComments = listOf("<p>Rendered comment</p>")
        )

        assertEquals("#12 Render issue previews", payload.title)
        assertContains(payload.html, "Rendered body")
        assertContains(payload.html, "Rendered comment")
    }

    @Test
    fun errorPayloadIncludesMessage(): Unit {
        val payload = errorPayload(issue(), "GitHub API request failed")

        assertEquals("#12 Render issue previews", payload.title)
        assertContains(payload.html, "GitHub API request failed")
    }

    private fun issue(): Issue =
        Issue(
            id = "issue-12",
            number = 12,
            title = "Render issue previews",
            body = "body",
            state = IssueState.OPEN,
            url = "https://github.com/octo/repo/issues/12",
            author = "octo",
            assignees = emptyList(),
            labels = emptyList(),
            commentsCount = 1,
            createdAt = "2026-06-26T00:00:00Z",
            updatedAt = "2026-06-26T00:00:00Z",
            isPullRequest = false,
            provider = "github",
            milestone = "M1"
        )

    private fun comment(): IssueComment =
        IssueComment(
            id = "comment-1",
            body = "comment",
            author = "octo",
            createdAt = "2026-06-26T00:00:00Z",
            updatedAt = "2026-06-26T00:00:00Z"
        )
}
