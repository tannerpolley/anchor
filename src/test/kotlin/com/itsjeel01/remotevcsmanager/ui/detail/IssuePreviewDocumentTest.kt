package com.itsjeel01.remotevcsmanager.ui.detail

import com.itsjeel01.remotevcsmanager.models.Issue
import com.itsjeel01.remotevcsmanager.models.IssueComment
import com.itsjeel01.remotevcsmanager.models.IssueState
import com.itsjeel01.remotevcsmanager.models.Label
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse

class IssuePreviewDocumentTest {

    private val theme = IssuePreviewTheme(
        background = "#ffffff",
        foreground = "#111111",
        border = "#d0d7de",
        secondary = "#57606a",
        muted = "#6e7781",
        link = "#0969da",
        codeBackground = "#f6f8fa",
        itemBackground = "#ffffff"
    )

    @Test
    fun issueDocumentIncludesBodyCommentsLabelsAndMetadata(): Unit {
        val issue = issueFixture(labels = listOf(Label("bug", "d73a4a")))
        val comment = IssueComment(
            id = "1",
            body = "raw comment",
            author = "octo",
            createdAt = "2026-06-26T00:00:00Z",
            updatedAt = "2026-06-26T00:00:00Z"
        )

        val html = IssuePreviewDocument.buildIssue(
            issue = issue,
            comments = listOf(comment),
            renderedBody = "<p>Rendered body</p>",
            renderedComments = listOf("<p>Rendered comment</p>"),
            theme = theme
        )

        assertContains(html, "Rendered body")
        assertContains(html, "Rendered comment")
        assertContains(html, "bug")
        assertContains(html, "#42")
        assertContains(html, "Issue title")
    }

    @Test
    fun errorDocumentEscapesTitleAndMessage(): Unit {
        val html = IssuePreviewDocument.buildError(
            title = "Issue <title>",
            message = "GitHub <failed>",
            theme = theme
        )

        assertContains(html, "Issue &lt;title&gt;")
        assertContains(html, "GitHub &lt;failed&gt")
        assertFalse(html.contains("Issue <title>"))
        assertFalse(html.contains("GitHub <failed>"))
    }

    private fun issueFixture(labels: List<Label> = emptyList()): Issue =
        Issue(
            id = "issue-42",
            number = 42,
            title = "Issue title",
            body = "raw body",
            state = IssueState.OPEN,
            url = "https://github.com/tannerpolley/anchor/issues/42",
            author = "tannerpolley",
            assignees = emptyList(),
            labels = labels,
            commentsCount = 1,
            createdAt = "2026-06-25T00:00:00Z",
            updatedAt = "2026-06-26T00:00:00Z",
            isPullRequest = false,
            provider = "github",
            milestone = "M1"
        )
}
