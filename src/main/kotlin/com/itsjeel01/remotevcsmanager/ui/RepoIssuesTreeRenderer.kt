package com.itsjeel01.remotevcsmanager.ui

import com.intellij.ui.ColoredTreeCellRenderer
import com.intellij.ui.SimpleTextAttributes
import com.itsjeel01.remotevcsmanager.models.Issue
import com.itsjeel01.remotevcsmanager.models.IssueState
import java.net.URI
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode

internal class RepoIssuesTreeRenderer : ColoredTreeCellRenderer() {

    override fun customizeCellRenderer(
        tree: JTree,
        value: Any?,
        selected: Boolean,
        expanded: Boolean,
        leaf: Boolean,
        row: Int,
        hasFocus: Boolean
    ): Unit {
        val node = value as? DefaultMutableTreeNode
        when (val item = node?.userObject) {
            is RepoIssueTreeItem.Milestone -> renderMilestone(item)
            is RepoIssueTreeItem.ParentIssue -> renderIssue(item.issue, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES)
            is RepoIssueTreeItem.SubIssue -> renderIssue(item.issue, SimpleTextAttributes.REGULAR_ATTRIBUTES)
            is RepoIssueTreeItem.StandaloneIssue -> renderIssue(item.issue, SimpleTextAttributes.REGULAR_ATTRIBUTES)
            is RepoIssueTreeItem.Dependency -> renderDependency(item)
            is RepoIssueTreeItem.Message -> append(item.text, SimpleTextAttributes.GRAYED_ATTRIBUTES)
            else -> append("GitHub Issues")
        }
    }

    private fun renderMilestone(item: RepoIssueTreeItem.Milestone): Unit {
        append(item.title, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES)
        append("  ${item.openIssueCount} open", SimpleTextAttributes.GRAYED_ATTRIBUTES)
        toolTipText = item.target.displayName
    }

    private fun renderIssue(issue: Issue, titleAttributes: SimpleTextAttributes): Unit {
        append("#${issue.number} ", SimpleTextAttributes.GRAYED_ATTRIBUTES)
        append(issue.title, titleAttributes)
        append("  ${TimeFormat.relative(issue.updatedAt)}", SimpleTextAttributes.GRAYED_ATTRIBUTES)
        toolTipText = issue.url
    }

    private fun renderDependency(item: RepoIssueTreeItem.Dependency): Unit {
        val issue = item.dependency.blockingIssue
        append("Blocked by · ", SimpleTextAttributes.GRAYED_ATTRIBUTES)
        append(
            issue.state.name,
            if (issue.state == IssueState.OPEN) {
                SimpleTextAttributes.ERROR_ATTRIBUTES
            } else {
                SimpleTextAttributes.GRAYED_ATTRIBUTES
            }
        )
        append(" · ${issueReference(issue)} · ${issue.title}", SimpleTextAttributes.REGULAR_ATTRIBUTES)
        toolTipText = issue.url
    }
}

internal fun issueReference(issue: Issue): String {
    val path = runCatching { URI(issue.url).path.trim('/').split('/') }.getOrDefault(emptyList())
    return if (path.size >= 4 && path[2] == "issues") {
        "${path[0]}/${path[1]}#${issue.number}"
    } else {
        "#${issue.number}"
    }
}
