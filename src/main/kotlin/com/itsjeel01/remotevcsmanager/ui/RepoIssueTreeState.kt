package com.itsjeel01.remotevcsmanager.ui

import com.intellij.ui.treeStructure.Tree
import com.itsjeel01.remotevcsmanager.models.Issue
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreePath

internal data class RepoIssueTreeState(
    val expandedKeys: Set<String>,
    val selectedIssueNumber: Int?
)

internal object RepoIssueTreeStateKeeper {

    fun capture(
        tree: Tree,
        rootNode: DefaultMutableTreeNode,
        selectedIssueNumber: Int?
    ): RepoIssueTreeState {
        val expandedKeys = mutableSetOf<String>()
        val nodes = rootNode.breadthFirstEnumeration()
        while (nodes.hasMoreElements()) {
            val node = nodes.nextElement() as? DefaultMutableTreeNode ?: continue
            if (tree.isExpanded(TreePath(node.path))) nodeKey(node)?.let(expandedKeys::add)
        }
        return RepoIssueTreeState(expandedKeys, selectedIssueNumber)
    }

    fun restore(
        tree: Tree,
        rootNode: DefaultMutableTreeNode,
        state: RepoIssueTreeState
    ): Issue? {
        var selectedIssue: Issue? = null
        var selectedPath: TreePath? = null
        val nodes = rootNode.breadthFirstEnumeration()
        while (nodes.hasMoreElements()) {
            val node = nodes.nextElement() as? DefaultMutableTreeNode ?: continue
            val path = TreePath(node.path)
            if (nodeKey(node) in state.expandedKeys) tree.expandPath(path)
            val item = node.userObject as? RepoIssueTreeItem.SelectableIssue
            if (item != null && item.issue.number == state.selectedIssueNumber) {
                selectedIssue = item.issue
                selectedPath = path
            }
        }
        tree.selectionPath = selectedPath
        return selectedIssue
    }

    private fun nodeKey(node: DefaultMutableTreeNode): String? =
        when (val item = node.userObject) {
            is RepoIssueTreeItem.Milestone -> "milestone:${item.title}"
            is RepoIssueTreeItem.SelectableIssue -> "issue:${item.issue.number}"
            else -> null
        }
}
