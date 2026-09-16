package com.itsjeel01.remotevcsmanager.ui

import com.intellij.icons.AllIcons
import com.intellij.ui.ColoredTreeCellRenderer
import com.intellij.ui.SimpleTextAttributes
import com.itsjeel01.remotevcsmanager.models.Issue
import java.net.URI
import javax.accessibility.AccessibleContext
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
        icon = null
        putClientProperty(AccessibleContext.ACCESSIBLE_NAME_PROPERTY, null)
        putClientProperty(AccessibleContext.ACCESSIBLE_DESCRIPTION_PROPERTY, null)
        val node = value as? DefaultMutableTreeNode
        when (val item = node?.userObject) {
            is RepoIssueTreeItem.Milestone -> renderMilestone(item)
            is RepoIssueTreeItem.ParentIssue -> renderIssue(item, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES)
            is RepoIssueTreeItem.SubIssue -> renderIssue(item, SimpleTextAttributes.REGULAR_ATTRIBUTES)
            is RepoIssueTreeItem.StandaloneIssue -> renderIssue(item, SimpleTextAttributes.REGULAR_ATTRIBUTES)
            is RepoIssueTreeItem.Message -> append(item.text, SimpleTextAttributes.GRAYED_ATTRIBUTES)
            else -> append("GitHub Issues")
        }
    }

    private fun renderMilestone(item: RepoIssueTreeItem.Milestone): Unit {
        append(item.title, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES)
        append("  ${item.openIssueCount} open", SimpleTextAttributes.GRAYED_ATTRIBUTES)
        toolTipText = item.target.displayName
    }

    private fun renderIssue(
        item: RepoIssueTreeItem.SelectableIssue,
        titleAttributes: SimpleTextAttributes
    ): Unit {
        val issue = item.issue
        val blockers = item.openBlockers
        if (blockers.isNotEmpty()) icon = AllIcons.General.InspectionsError
        append("#${issue.number} ", SimpleTextAttributes.GRAYED_ATTRIBUTES)
        append(issue.title, titleAttributes)
        append("  ${TimeFormat.relative(issue.updatedAt)}", SimpleTextAttributes.GRAYED_ATTRIBUTES)
        val blockedBy = blockers.takeIf { it.isNotEmpty() }?.let(::blockerTooltip)
        toolTipText = blockedBy ?: issue.url
        putClientProperty(AccessibleContext.ACCESSIBLE_NAME_PROPERTY, buildString {
            if (blockedBy != null) append("Blocked. ")
            append("Issue ${issue.number}: ${issue.title}")
        })
        putClientProperty(
            AccessibleContext.ACCESSIBLE_DESCRIPTION_PROPERTY,
            blockedBy ?: issue.url
        )
    }
}

internal fun blockerTooltip(blockers: List<Issue>): String =
    blockers.joinToString(prefix = "Blocked by ", separator = "; ") {
        "${issueReference(it)} — ${it.title}"
    }

internal fun issueReference(issue: Issue): String {
    val path = runCatching { URI(issue.url).path.trim('/').split('/') }.getOrDefault(emptyList())
    return if (path.size >= 4 && path[2] == "issues") {
        "${path[0]}/${path[1]}#${issue.number}"
    } else {
        "#${issue.number}"
    }
}
