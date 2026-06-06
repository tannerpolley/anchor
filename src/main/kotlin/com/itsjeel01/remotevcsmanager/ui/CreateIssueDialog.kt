package com.itsjeel01.remotevcsmanager.ui

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.itsjeel01.remotevcsmanager.models.Label as VsLabel
import com.itsjeel01.remotevcsmanager.providers.github.GitHubProvider
import kotlinx.coroutines.runBlocking
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import kotlin.concurrent.thread

/**
 * Enhanced Create Issue Dialog — keep as Swing (DialogWrapper requires JComponent).
 */
class CreateIssueDialog(
    project: Project?,
    private val owner: String,
    private val repo: String,
    private val provider: GitHubProvider
) : com.intellij.openapi.ui.DialogWrapper(project) {

    private val tf = JBTextField().apply { emptyText.text = "Issue title" }
    private val ta = JBTextArea().apply {
        emptyText.text = "Leave a description..."; lineWrap = true; rows = 6
        font = Font(Font.MONOSPACED, Font.PLAIN, JBUI.Fonts.label().size)
    }
    private val assigneeField = JBTextField().apply {
        emptyText.text = "e.g. username (comma-separated)"
    }

    // Label selection
    private val labelChipsPanel = JPanel(FlowLayout(FlowLayout.LEFT, 4, 2)).apply { isOpaque = false }
    private val selectedLabels = mutableSetOf<VsLabel>()
    private var repoLabels: List<VsLabel> = emptyList()

    init {
        title = "New Issue — $owner/$repo"
        init()
        fetchLabels()
    }

    private fun fetchLabels() {
        thread {
            try {
                repoLabels = runBlocking { provider.getLabels(owner, repo) }
                SwingUtilities.invokeLater { renderLabelChips() }
            } catch (_: Exception) {
                SwingUtilities.invokeLater {
                    labelChipsPanel.add(JBLabel(" (failed to load labels)").apply {
                        font = JBUI.Fonts.smallFont()
                        foreground = UIUtil.getContextHelpForeground()
                    })
                    labelChipsPanel.revalidate()
                }
            }
        }
    }

    private fun renderLabelChips() {
        labelChipsPanel.removeAll()
        repoLabels.forEach { label ->
            val chip = JBLabel(" ${label.name} ").apply {
                font = JBUI.Fonts.smallFont()
                isOpaque = true
                cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                border = JBUI.Borders.empty(2, 6)
                val bgColor = try {
                    Color(label.color.substring(0, 2).toInt(16),
                        label.color.substring(2, 4).toInt(16),
                        label.color.substring(4, 6).toInt(16))
                } catch (_: Exception) { Color.GRAY }
                val isDark = (bgColor.red * 299 + bgColor.green * 587 + bgColor.blue * 114) / 1000 < 140
                foreground = if (isDark) Color.WHITE else Color.BLACK
                background = bgColor

                addMouseListener(object : MouseAdapter() {
                    override fun mouseClicked(e: MouseEvent) {
                        if (selectedLabels.contains(label)) {
                            selectedLabels.remove(label); border = JBUI.Borders.empty(2, 6)
                        } else {
                            selectedLabels.add(label)
                            border = BorderFactory.createCompoundBorder(
                                JBUI.Borders.customLine(UIUtil.getLabelForeground(), 2),
                                JBUI.Borders.empty(2, 6))
                        }
                    }
                })
            }
            labelChipsPanel.add(chip)
        }
        labelChipsPanel.revalidate(); labelChipsPanel.repaint()
    }

    override fun createCenterPanel(): JComponent {
        val form = com.intellij.util.ui.FormBuilder.createFormBuilder()
            .addLabeledComponent("Title:", tf, true)
            .addVerticalGap(8)
            .addLabeledComponent("Description:", JBScrollPane(ta), true)
            .addVerticalGap(8)
            .addLabeledComponent("Assignees:", assigneeField, true)
            .addVerticalGap(8)
            .addLabeledComponent("Labels:", JPanel(BorderLayout()).apply {
                isOpaque = false; add(labelChipsPanel, BorderLayout.CENTER)
            }, true)

        return JPanel(BorderLayout()).apply {
            add(form.panel.apply {
                border = JBUI.Borders.empty(8)
                preferredSize = Dimension(520, 400)
            }, BorderLayout.CENTER)
        }
    }

    override fun doOKAction() {
        val t = tf.text.trim()
        if (t.isBlank()) {
            com.intellij.openapi.ui.Messages.showErrorDialog("Title is required to create an issue.", "Validation Error")
            return
        }

        val issueLabels: List<String>? = if (selectedLabels.isEmpty()) null else selectedLabels.map { it.name }
        val issueAssignees = assigneeField.text.trim()
            .takeIf { it.isNotBlank() }
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }

        ProgressManager.getInstance().run(object :
            Task.Backgroundable(null, "Creating issue...", true) {
            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = true
                try {
                    runBlocking {
                        provider.createIssue(owner, repo, t, ta.text.ifBlank { null }, issueLabels, issueAssignees)
                    }
                    ApplicationManager.getApplication().invokeLater {
                        this@CreateIssueDialog.close(OK_EXIT_CODE)
                    }
                } catch (e: Exception) {
                    ApplicationManager.getApplication().invokeLater {
                        com.intellij.openapi.ui.Messages.showErrorDialog("Failed: ${e.message}", "Error")
                    }
                }
            }
        })
    }
}
