package com.itsjeel01.remotevcsmanager.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import com.itsjeel01.remotevcsmanager.providers.github.GitHubProvider
import com.itsjeel01.remotevcsmanager.ui.Icons
import com.itsjeel01.remotevcsmanager.ui.PluginNotifications
import kotlinx.coroutines.runBlocking
import javax.swing.*

/**
 * Action to create a new repository on the remote provider (GitHub).
 */
class CreateRepositoryAction : AnAction(), DumbAware {

    private val provider = GitHubProvider()

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = provider.isConfigured()
        e.presentation.text = "Create Repository on GitHub..."
        e.presentation.description = "Create a new repository on GitHub"
        e.presentation.icon = Icons.CREATE_ICON
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        if (!provider.isConfigured()) {
            PluginNotifications.error(project, "GitHub not configured",
                "Please set up your GitHub token in Settings > Remote VCS Manager")
            return
        }


        val dialog = CreateRepositoryDialog()
        if (!dialog.showAndGet()) return

        val name = dialog.repositoryName
        val description = dialog.repositoryDescription
        val isPrivate = dialog.isPrivate

        if (name.isBlank()) {
            Messages.showErrorDialog(project, "Repository name cannot be empty.", "Create Repository")
            return
        }


        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Creating repository '$name'...", true) {
            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = true

                try {
                    val repo = runBlocking {
                        provider.createRepository(name, description.ifBlank { null }, isPrivate)
                    }

                    com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
                        PluginNotifications.info(
                            project,
                            "Repository created",
                            "${repo.fullName} created successfully on GitHub.\nClone URL: ${repo.cloneUrl}"
                        )
                    }
                } catch (ex: Exception) {
                    com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
                        PluginNotifications.error(
                            project,
                            "Failed to create repository",
                            ex.message ?: "Unknown error"
                        )
                    }
                }
            }
        })
    }

    /**
     * Dialog for entering repository creation details.
     */
    private class CreateRepositoryDialog : DialogWrapper(null) {
        private val nameField: JBTextField = JBTextField().apply {
            emptyText.text = "Repository name (e.g., my-awesome-project)"
        }
        private val descriptionField: JBTextField = JBTextField().apply {
            emptyText.text = "Description (optional)"
        }
        private val privateCheckbox: JCheckBox = JCheckBox("Private repository").apply {
            isSelected = false
        }

        var repositoryName: String = ""
            private set
        var repositoryDescription: String = ""
            private set
        var isPrivate: Boolean = false
            private set

        init {
            title = "Create Repository on GitHub"
            init()
        }

        override fun createCenterPanel(): JComponent {
            val panel = FormBuilder.createFormBuilder()
                .addLabeledComponent("Repository Name:", nameField, true)
                .addLabeledComponent("Description:", descriptionField, true)
                .addComponent(privateCheckbox)
                .addVerticalGap(5)
                .addComponent(JLabel("<html><small>Repository will be created on GitHub under your account</small></html>"))
                .panel

            panel.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
            return panel
        }

        override fun doOKAction() {
            repositoryName = nameField.text.trim()
            repositoryDescription = descriptionField.text.trim()
            isPrivate = privateCheckbox.isSelected

            if (repositoryName.isBlank()) {
                Messages.showErrorDialog("Repository name cannot be empty.", "Invalid Input")
                return
            }

            super.doOKAction()
        }
    }
}
