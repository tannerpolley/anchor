package com.itsjeel01.remotevcsmanager.ui

import com.intellij.ui.treeStructure.Tree
import com.itsjeel01.remotevcsmanager.models.Issue
import com.itsjeel01.remotevcsmanager.models.IssueState
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RepoIssueTreeStateTest {

    @Test
    fun restoresExpandedNodesAndSelectedIssueAfterTreeReplacement(): Unit {
        val target = RepoIssueTarget("repo", "octo", "repo", "/repo", "https://github.com/octo/repo/issues")
        val original = tree(target, issue(26))
        original.tree.expandPath(TreePath(original.milestone.path))
        original.tree.expandPath(TreePath(original.issue.path))
        original.tree.selectionPath = TreePath(original.issue.path)

        val state = RepoIssueTreeStateKeeper.capture(original.tree, original.root, 26)
        val replacement = tree(target, issue(26, title = "Updated title"))
        val selected = RepoIssueTreeStateKeeper.restore(replacement.tree, replacement.root, state)

        assertTrue(replacement.tree.isExpanded(TreePath(replacement.milestone.path)))
        assertTrue(replacement.tree.isExpanded(TreePath(replacement.issue.path)))
        assertEquals(26, selected?.number)
        assertEquals("Updated title", selected?.title)
    }

    private fun tree(target: RepoIssueTarget, issue: Issue): TreeFixture {
        val root = DefaultMutableTreeNode("root")
        val milestone = DefaultMutableTreeNode(RepoIssueTreeItem.Milestone(target, "M1", 1))
        val issueNode = DefaultMutableTreeNode(RepoIssueTreeItem.StandaloneIssue(target, issue))
        issueNode.add(DefaultMutableTreeNode(RepoIssueTreeItem.Message("child")))
        milestone.add(issueNode)
        root.add(milestone)
        return TreeFixture(Tree(DefaultTreeModel(root)), root, milestone, issueNode)
    }

    private fun issue(number: Int, title: String = "Issue $number"): Issue =
        Issue(
            id = "issue-$number",
            number = number,
            title = title,
            body = null,
            state = IssueState.OPEN,
            url = "https://github.com/octo/repo/issues/$number",
            author = "octo",
            assignees = emptyList(),
            labels = emptyList(),
            commentsCount = 0,
            createdAt = "2026-09-01T00:00:00Z",
            updatedAt = "2026-09-01T00:00:00Z",
            isPullRequest = false,
            provider = "github",
            milestone = "M1"
        )

    private data class TreeFixture(
        val tree: Tree,
        val root: DefaultMutableTreeNode,
        val milestone: DefaultMutableTreeNode,
        val issue: DefaultMutableTreeNode
    )
}
