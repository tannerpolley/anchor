package com.itsjeel01.remotevcsmanager

import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.ide.plugins.PluginStateListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManagerListener
import com.intellij.openapi.startup.ProjectActivity

/**
 * Plugin lifecycle handler for Remote VCS Manager.
 * Handles plugin start, shutdown, and project-level initialization.
 */
class RemoteVcsManagerPlugin : PluginStateListener {

    override fun install(descriptor: IdeaPluginDescriptor) {
        // Plugin installed
    }

    override fun uninstall(descriptor: IdeaPluginDescriptor) {
        // Plugin uninstalled
    }
}

/**
 * Project-level startup activity.
 * Initializes per-project components when a project is opened.
 */
class RemoteVcsManagerProjectActivity : ProjectActivity {

    override suspend fun execute(project: Project) {
        // Future: Initialize project-level components
        // e.g., register listeners for git events
    }
}

/**
 * Project manager listener for lifecycle events.
 */
class RemoteVcsManagerProjectListener : ProjectManagerListener {

    override fun projectOpened(project: Project) {
        // Project opened - perform any initialization
    }

    override fun projectClosed(project: Project) {
        // Project closed - cleanup resources
    }
}
