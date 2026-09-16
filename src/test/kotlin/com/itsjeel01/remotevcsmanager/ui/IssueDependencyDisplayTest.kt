package com.itsjeel01.remotevcsmanager.ui

import com.intellij.icons.AllIcons
import com.intellij.ui.treeStructure.Tree
import com.itsjeel01.remotevcsmanager.models.Issue
import com.itsjeel01.remotevcsmanager.models.IssueState
import javax.accessibility.AccessibleContext
import javax.swing.tree.DefaultMutableTreeNode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class IssueDependencyDisplayTest {

    @Test
    fun includesRepositoryForCrossRepositoryDependencies(): Unit {
        assertEquals("octo/repo#23", issueReference(issue("https://github.com/octo/repo/issues/23")))
        assertEquals("#23", issueReference(issue("not a URL")))
    }

    @Test
    fun summarizesOpenBlockersInline(): Unit {
        assertEquals(
            "Blocked by octo/repo#23 — Accept design; octo/repo#24 — Ship API",
            blockerTooltip(
                listOf(
                    issue("https://github.com/octo/repo/issues/23"),
                    issue("https://github.com/octo/repo/issues/24", number = 24, title = "Ship API")
                )
            )
        )
    }

    @Test
    fun rendersMarkerOnlyForBlockedIssues(): Unit {
        val target = RepoIssueTarget(
            "repo",
            "octo",
            "repo",
            "/repo",
            "https://github.com/octo/repo/issues"
        )
        val currentIssue = issue(
            "https://github.com/octo/repo/issues/10",
            number = 10,
            title = "Current issue"
        )
        val blocker = issue("https://github.com/octo/repo/issues/23")
        val renderer = RepoIssuesTreeRenderer()

        render(renderer, RepoIssueTreeItem.StandaloneIssue(target, currentIssue, listOf(blocker)))
        assertSame(AllIcons.General.InspectionsError, renderer.icon)
        assertEquals("Blocked by octo/repo#23 — Accept design", renderer.toolTipText)
        assertEquals(
            "Blocked. Issue 10: Current issue",
            renderer.getClientProperty(AccessibleContext.ACCESSIBLE_NAME_PROPERTY)
        )

        render(renderer, RepoIssueTreeItem.StandaloneIssue(target, currentIssue, emptyList()))
        assertNull(renderer.icon)
        assertEquals(currentIssue.url, renderer.toolTipText)
        assertEquals(
            "Issue 10: Current issue",
            renderer.getClientProperty(AccessibleContext.ACCESSIBLE_NAME_PROPERTY)
        )
    }

    private fun render(renderer: RepoIssuesTreeRenderer, item: RepoIssueTreeItem.SelectableIssue): Unit {
        renderer.getTreeCellRendererComponent(
            Tree(),
            DefaultMutableTreeNode(item),
            false,
            false,
            true,
            0,
            false
        )
    }

    private fun issue(
        url: String,
        number: Int = 23,
        title: String = "Accept design"
    ): Issue =
        Issue(
            id = "issue-$number",
            number = number,
            title = title,
            body = null,
            state = IssueState.OPEN,
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
