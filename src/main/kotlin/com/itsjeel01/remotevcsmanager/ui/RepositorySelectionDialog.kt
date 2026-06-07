package com.itsjeel01.remotevcsmanager.ui

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.itsjeel01.remotevcsmanager.models.RemoteRepository
import java.awt.BorderLayout
import javax.swing.*

/**
 * Dialog for selecting a remote repository to clone.
 */
class RepositorySelectionDialog(
    private val project: com.intellij.openapi.project.Project,
    private val repositories: List<RemoteRepository>
) : DialogWrapper(project) {

    private val listModel: DefaultListModel<RemoteRepositoryListItem> = DefaultListModel()
    private val repositoryList: JBList<RemoteRepositoryListItem> = JBList(listModel)
    private var selectedRepository: RemoteRepository? = null

    init {
        title = "Select Repository to Clone"
        init()

        repositories.forEach { repo ->
            val label = "${repo.owner}/${repo.name} - ${repo.description ?: "No description"}"
            val icon = if (repo.isPrivate) "🔒" else "🌍"
            listModel.addElement(RemoteRepositoryListItem(repo, "$icon $label"))
        }

        repositoryList.cellRenderer = DefaultListCellRenderer()
        repositoryList.selectedIndex = 0

        if (repositories.isNotEmpty()) {
            selectedRepository = repositories[0]
        }

        repositoryList.addListSelectionListener {
            val selected = repositoryList.selectedValue
            selectedRepository = selected?.repository
        }
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(BorderLayout())
        val label = JLabel("Select a repository to clone:")
        label.border = BorderFactory.createEmptyBorder(0, 0, 5, 0)
        panel.add(label, BorderLayout.NORTH)
        panel.add(JBScrollPane(repositoryList), BorderLayout.CENTER)
        panel.preferredSize = java.awt.Dimension(500, 400)
        return panel
    }

    fun getSelectedRepository(): RemoteRepository? = selectedRepository

    /**
     * Data class wrapping a repository with a display label.
     */
    data class RemoteRepositoryListItem(
        val repository: RemoteRepository,
        val displayLabel: String
    ) {
        override fun toString(): String = displayLabel
    }
}
