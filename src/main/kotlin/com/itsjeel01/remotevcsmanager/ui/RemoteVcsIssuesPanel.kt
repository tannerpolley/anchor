package com.itsjeel01.remotevcsmanager.ui

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.jcef.JBCefApp
import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.util.ui.JBUI
import com.itsjeel01.remotevcsmanager.GitRemoteDetector.GitRemoteInfo
import com.itsjeel01.remotevcsmanager.GitRootDiscovery
import com.itsjeel01.remotevcsmanager.GitRemoteDetector
import java.awt.BorderLayout
import java.awt.Component
import java.awt.FlowLayout
import javax.swing.DefaultListCellRenderer
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.JSplitPane
import javax.swing.JTextArea
import javax.swing.ListSelectionModel

object RemoteVcsIssuesPanel {

    fun create(project: Project): JComponent {
        val targets = resolveTargets(project)
        val panel = JBPanel<JBPanel<*>>(BorderLayout())
        panel.border = JBUI.Borders.empty(8)

        if (targets.isEmpty()) {
            panel.add(createMessagePanel("Anchor could not detect a GitHub remote for this project."), BorderLayout.CENTER)
            return panel
        }

        val selected = SelectedTarget(targets.first())
        panel.add(createHeader(selected), BorderLayout.NORTH)

        if (JBCefApp.isSupported()) {
            panel.add(createBrowserPanel(project, targets, selected), BorderLayout.CENTER)
        } else {
            panel.add(createBrowserlessPanel(targets, selected), BorderLayout.CENTER)
        }

        return panel
    }

    private fun createHeader(selected: SelectedTarget): JComponent {
        val header = JBPanel<JBPanel<*>>(BorderLayout())
        header.border = JBUI.Borders.emptyBottom(8)
        header.add(JBLabel("GitHub Issues"), BorderLayout.WEST)

        val actions = JPanel(FlowLayout(FlowLayout.RIGHT, JBUI.scale(6), 0))
        val open = JButton("Open in Browser")
        open.addActionListener { BrowserUtil.browse(selected.target.issuesUrl) }
        actions.add(open)
        header.add(actions, BorderLayout.EAST)
        return header
    }

    private fun createBrowserPanel(
        project: Project,
        targets: List<RepoTarget>,
        selected: SelectedTarget
    ): JComponent {
        val browser = JBCefBrowser(selected.target.issuesUrl)
        Disposer.register(project, browser)

        if (targets.size == 1) {
            return browser.component
        }

        val repoList = createRepoList(targets)
        repoList.addListSelectionListener {
            if (!it.valueIsAdjusting) {
                val target = repoList.selectedValue ?: return@addListSelectionListener
                selected.target = target
                browser.loadURL(target.issuesUrl)
            }
        }

        return JSplitPane(
            JSplitPane.HORIZONTAL_SPLIT,
            JBScrollPane(repoList),
            browser.component
        ).apply {
            border = JBUI.Borders.empty()
            resizeWeight = 0.0
            dividerLocation = JBUI.scale(220)
        }
    }

    private fun createBrowserlessPanel(
        targets: List<RepoTarget>,
        selected: SelectedTarget
    ): JComponent {
        val panel = JBPanel<JBPanel<*>>(BorderLayout())
        panel.add(
            createMessagePanel("This IDE runtime does not include JCEF. Select a repository and open it in your system browser."),
            BorderLayout.NORTH
        )
        val repoList = createRepoList(targets)
        repoList.addListSelectionListener {
            if (!it.valueIsAdjusting) {
                selected.target = repoList.selectedValue ?: return@addListSelectionListener
            }
        }
        panel.add(JBScrollPane(repoList), BorderLayout.CENTER)
        return panel
    }

    private fun createRepoList(targets: List<RepoTarget>): JBList<RepoTarget> =
        JBList(targets).apply {
            selectionMode = ListSelectionModel.SINGLE_SELECTION
            selectedIndex = 0
            cellRenderer = RepoTargetRenderer()
            visibleRowCount = 12
        }

    private fun createMessagePanel(message: String): JComponent {
        val area = JTextArea(message)
        area.isEditable = false
        area.isOpaque = false
        area.lineWrap = true
        area.wrapStyleWord = true
        area.border = JBUI.Borders.empty(12)
        return area
    }

    private fun resolveTargets(project: Project): List<RepoTarget> {
        val detector = GitRemoteDetector(project)
        return GitRootDiscovery.roots(project).mapNotNull { root ->
            detector.detect(root)?.let(::createTarget)
        }.distinctBy { it.issuesUrl }
            .sortedBy { it.displayName.lowercase() }
    }

    private fun createTarget(remote: GitRemoteInfo): RepoTarget? {
        if (remote.provider != "github") return null
        val host = parseRemoteHost(remote.remoteUrl) ?: "github.com"
        return RepoTarget(
            displayName = remote.repoName,
            rootPath = remote.gitRoot.absolutePath,
            issuesUrl = "https://$host/${remote.owner}/${remote.repoName}/issues"
        )
    }

    private fun parseRemoteHost(remoteUrl: String): String? {
        Regex("""https?://([^/]+)/""").find(remoteUrl)?.let {
            return it.groupValues[1]
        }
        Regex("""git@([^:]+):""").find(remoteUrl)?.let {
            return it.groupValues[1]
        }
        Regex("""git://([^/]+)/""").find(remoteUrl)?.let {
            return it.groupValues[1]
        }
        return null
    }

    private class RepoTargetRenderer : DefaultListCellRenderer() {
        override fun getListCellRendererComponent(
            list: JList<*>?,
            value: Any?,
            index: Int,
            isSelected: Boolean,
            cellHasFocus: Boolean
        ): Component {
            val component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
            if (value is RepoTarget) {
                text = value.displayName
                toolTipText = value.rootPath
            }
            return component
        }
    }

    private data class SelectedTarget(var target: RepoTarget)

    private data class RepoTarget(
        val displayName: String,
        val rootPath: String,
        val issuesUrl: String
    )
}
