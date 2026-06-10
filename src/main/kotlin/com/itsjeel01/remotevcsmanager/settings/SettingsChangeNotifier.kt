package com.itsjeel01.remotevcsmanager.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.util.messages.Topic

/**
 * Application-level message bus topic for token changes.
 * Publish from Configurable.apply(), subscribe in ToolWindow panel.
 */
object SettingsChangeNotifier {

    fun interface SettingsChangeListener {
        fun onSettingsChanged()
    }

    val SETTINGS_CHANGED: Topic<SettingsChangeListener> = Topic.create(
        "RemoteVCSManager.SettingsChanged",
        SettingsChangeListener::class.java
    )

    fun publish() {
        ApplicationManager.getApplication().messageBus
            .syncPublisher(SETTINGS_CHANGED)
            .onSettingsChanged()
    }
}
