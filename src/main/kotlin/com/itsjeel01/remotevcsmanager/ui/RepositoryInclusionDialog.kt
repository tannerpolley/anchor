package com.itsjeel01.remotevcsmanager.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JPanel

internal class RepositoryInclusionDialog(
    project: Project,
    private val targets: List<RepoIssueTarget>,
    initialState: RepoIssueInclusionState
) : DialogWrapper(project) {

    private val checkBoxes: Map<RepoIssueTarget, JBCheckBox> = targets.associateWith { target ->
        JBCheckBox(labelFor(target)).apply {
            isSelected = initialState.isIncluded(target)
        }
    }

    init {
        title = "Repositories"
        init()
    }

    fun selectedState(): RepoIssueInclusionState =
        RepoIssueInclusionState(
            checkBoxes
                .filterValues { !it.isSelected }
                .keys
                .map(RepoIssueInclusionState::keyFor)
                .toSet()
        )

    override fun createCenterPanel(): JComponent {
        val content = JPanel()
        content.layout = BoxLayout(content, BoxLayout.Y_AXIS)
        content.border = JBUI.Borders.empty(8)
        checkBoxes.values.forEach(content::add)

        return JPanel(BorderLayout()).apply {
            preferredSize = JBUI.size(440, 260)
            add(JBScrollPane(content), BorderLayout.CENTER)
        }
    }

    companion object {
        private fun labelFor(target: RepoIssueTarget): String =
            "${target.owner}/${target.repoName} - ${target.rootPath}"
    }
}
