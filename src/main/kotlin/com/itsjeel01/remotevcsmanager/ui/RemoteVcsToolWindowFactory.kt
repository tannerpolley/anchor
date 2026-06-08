package com.itsjeel01.remotevcsmanager.ui

import androidx.compose.ui.awt.ComposePanel
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

class RemoteVcsToolWindowFactory : ToolWindowFactory, DumbAware {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val icon = IconLoader.getIcon("/META-INF/toolWindow.svg", javaClass)
        toolWindow.setIcon(icon)

        val panel = ComposePanel()
        panel.setContent {
            RemoteVcsToolWindowContent(project)
        }

        val content = ContentFactory.getInstance().createContent(panel, "", false)
        toolWindow.contentManager.addContent(content)
    }
}
