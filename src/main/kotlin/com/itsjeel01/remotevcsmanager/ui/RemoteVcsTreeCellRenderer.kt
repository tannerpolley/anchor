package com.itsjeel01.remotevcsmanager.ui

import com.intellij.ui.ColoredTreeCellRenderer
import com.intellij.ui.SimpleTextAttributes
import com.itsjeel01.remotevcsmanager.models.PRState
import com.itsjeel01.remotevcsmanager.models.PullRequest
import com.itsjeel01.remotevcsmanager.models.RemoteRepository
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode

/**
 * Custom tree cell renderer for the Remote VCS Manager tool window.
 */
class RemoteVcsTreeCellRenderer : ColoredTreeCellRenderer() {

    override fun customizeCellRenderer(
        tree: JTree,
        value: Any?,
        selected: Boolean,
        expanded: Boolean,
        leaf: Boolean,
        row: Int,
        hasFocus: Boolean
    ) {
        if (value !is DefaultMutableTreeNode) return

        val userObject = value.userObject

        when (userObject) {
            is String -> {
                // Section headers like "Repositories", "Pull Requests"
                append(userObject, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES)
                icon = when (userObject) {
                    "Repositories" -> Icons.REPOSITORY_ICON
                    "Pull Requests" -> Icons.PR_OPEN_ICON
                    else -> Icons.PLUGIN_ICON
                }
            }

            is RemoteRepository -> {
                append(userObject.name, SimpleTextAttributes.REGULAR_ATTRIBUTES)
                append("  (${userObject.owner})", SimpleTextAttributes.GRAY_ATTRIBUTES)

                val privateText = if (userObject.isPrivate) " [Private]" else " [Public]"
                append(privateText, SimpleTextAttributes.GRAY_SMALL_ATTRIBUTES)

                icon = Icons.REPOSITORY_ICON
                toolTipText = userObject.description ?: userObject.fullName
            }

            is PullRequest -> {
                append("#${userObject.id.take(7)} ", SimpleTextAttributes.GRAY_ATTRIBUTES)
                append(userObject.title, SimpleTextAttributes.REGULAR_ATTRIBUTES)

                icon = when (userObject.state) {
                    PRState.OPEN -> Icons.PR_OPEN_ICON
                    PRState.CLOSED -> Icons.PR_CLOSED_ICON
                    PRState.MERGED -> Icons.PR_MERGED_ICON
                }

                toolTipText = "${userObject.author}: ${userObject.title}"
            }

            else -> {
                append(userObject?.toString() ?: "Unknown", SimpleTextAttributes.REGULAR_ATTRIBUTES)
            }
        }
    }
}
