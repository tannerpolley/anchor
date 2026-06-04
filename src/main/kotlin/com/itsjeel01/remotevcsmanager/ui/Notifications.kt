package com.itsjeel01.remotevcsmanager.ui

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project

/**
 * Notification utilities for the Remote VCS Manager plugin.
 * Uses the IntelliJ notification system to show messages to the user.
 */
object PluginNotifications {

    private const val GROUP_ID = "Remote VCS Manager"

    /**
     * Show a notification with the given type.
     */
    private fun notify(
        project: Project?,
        title: String,
        content: String,
        type: NotificationType
    ) {
        val notification = NotificationGroupManager.getInstance()
            .getNotificationGroup(GROUP_ID)
            .createNotification(content, type)
            .setTitle(title)
        notification.notify(project)
    }

    /**
     * Show an info notification.
     */
    fun info(project: Project?, title: String, content: String) {
        notify(project, title, content, NotificationType.INFORMATION)
    }

    /**
     * Show a warning notification.
     */
    fun warn(project: Project?, title: String, content: String) {
        notify(project, title, content, NotificationType.WARNING)
    }

    /**
     * Show an error notification.
     */
    fun error(project: Project?, title: String, content: String) {
        notify(project, title, content, NotificationType.ERROR)
    }
}
