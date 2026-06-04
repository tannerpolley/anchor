package com.itsjeel01.remotevcsmanager.ui

import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

/**
 * Icon definitions for the Remote VCS Manager plugin.
 */
object Icons {
    @JvmField
    val PLUGIN_ICON: Icon = IconLoader.getIcon("/META-INF/pluginIcon.svg", javaClass)

    @JvmField
    val GITHUB_ICON: Icon = IconLoader.getIcon("/icons/github.svg", javaClass)

    @JvmField
    val REPOSITORY_ICON: Icon = IconLoader.getIcon("/icons/repository.svg", javaClass)

    @JvmField
    val PR_OPEN_ICON: Icon = IconLoader.getIcon("/icons/prOpen.svg", javaClass)

    @JvmField
    val PR_CLOSED_ICON: Icon = IconLoader.getIcon("/icons/prClosed.svg", javaClass)

    @JvmField
    val PR_MERGED_ICON: Icon = IconLoader.getIcon("/icons/prMerged.svg", javaClass)

    @JvmField
    val CLONE_ICON: Icon = IconLoader.getIcon("/icons/clone.svg", javaClass)

    @JvmField
    val CREATE_ICON: Icon = IconLoader.getIcon("/icons/create.svg", javaClass)

    @JvmField
    val OPEN_IN_BROWSER_ICON: Icon = IconLoader.getIcon("/icons/openInBrowser.svg", javaClass)

    @JvmField
    val ADD_REMOTE_ICON: Icon = IconLoader.getIcon("/icons/addRemote.svg", javaClass)
}
