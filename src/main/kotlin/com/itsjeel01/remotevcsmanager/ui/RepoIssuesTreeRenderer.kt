package com.itsjeel01.remotevcsmanager.ui

import com.intellij.ui.ColoredTreeCellRenderer
import com.intellij.ui.SimpleTextAttributes
import com.itsjeel01.remotevcsmanager.models.Issue
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
            is RepoIssueTreeItem.Repository -> renderRepository(item)
            is RepoIssueTreeItem.Milestone -> renderMilestone(item)
            is RepoIssueTreeItem.IssueNode -> renderIssue(item.issue)
            is RepoIssueTreeItem.Message -> append(item.text, SimpleTextAttributes.GRAYED_ATTRIBUTES)
            else -> append("GitHub Issues")
        }
    }

    private fun renderRepository(item: RepoIssueTreeItem.Repository): Unit {
        append(item.target.displayName, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES)
        val count = item.openIssueCount
        if (count != null) {
            append("  $count open", SimpleTextAttributes.GRAYED_ATTRIBUTES)
        }
        toolTipText = item.target.rootPath
    }

    private fun renderMilestone(item: RepoIssueTreeItem.Milestone): Unit {
        append(item.title, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES)
        append("  ${item.openIssueCount} open", SimpleTextAttributes.GRAYED_ATTRIBUTES)
        toolTipText = item.target.displayName
    }

    private fun renderIssue(issue: Issue): Unit {
        append("#${issue.number} ", SimpleTextAttributes.GRAYED_ATTRIBUTES)
        append(issue.title, SimpleTextAttributes.REGULAR_ATTRIBUTES)
        append("  ${TimeFormat.relative(issue.updatedAt)}", SimpleTextAttributes.GRAYED_ATTRIBUTES)
        toolTipText = issue.url
    }
}
