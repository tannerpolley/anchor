package com.itsjeel01.remotevcsmanager.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.util.ui.UIUtil
import com.itsjeel01.remotevcsmanager.GitRemoteDetector
import com.itsjeel01.remotevcsmanager.models.*
import com.itsjeel01.remotevcsmanager.providers.github.GitHubProvider
import com.itsjeel01.remotevcsmanager.settings.SettingsChangeNotifier
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.*
import javax.swing.JOptionPane
import javax.swing.SwingUtilities
import kotlin.concurrent.thread

sealed class Screen { data object List : Screen(); data object Detail : Screen() }

class ToolWindowState(project: Project) {

    private val gitDetector = GitRemoteDetector(project)
    val provider = GitHubProvider()
    val myProject = project

    var remoteOwner by mutableStateOf<String?>(null)
    var remoteRepo by mutableStateOf<String?>(null)
    var remoteDetected by mutableStateOf(false)
    var currentBranch by mutableStateOf<String?>(null)

    var statusText by mutableStateOf("Ready")
    var statusColor by mutableStateOf(UIUtil.getContextHelpForeground())

    var activeScreen: Screen by mutableStateOf(Screen.List)
    var issueFilterState by mutableStateOf("all")
    var prFilterState by mutableStateOf("open")

    var issueData by mutableStateOf<List<Issue>>(emptyList())
    var prData by mutableStateOf<List<PullRequest>>(emptyList())
    var branchData by mutableStateOf<List<GitBranch>>(emptyList())
    var selectedIssue by mutableStateOf<Issue?>(null)
    var selectedPR by mutableStateOf<PullRequest?>(null)

    init {
        reloadConfig()
        ApplicationManager.getApplication().messageBus.connect().subscribe(
            SettingsChangeNotifier.SETTINGS_CHANGED,
            SettingsChangeNotifier.SettingsChangeListener { reloadConfig() }
        )
    }

    fun reloadConfig() {
        remoteDetected = gitDetector.hasRemote() && provider.isConfigured()
        if (remoteDetected) {
            val info = gitDetector.detect()
            if (info != null) {
                remoteOwner = info.owner; remoteRepo = info.repoName; currentBranch = info.currentBranch
            }
            activeScreen = Screen.List; refresh()
        } else {
            remoteOwner = null; remoteRepo = null; currentBranch = null
            statusText = if (!gitDetector.hasRemote()) "Set a git remote first" else "Configure token in Settings"
            statusColor = UIUtil.getContextHelpForeground()
            issueData = emptyList(); prData = emptyList(); branchData = emptyList()
        }
    }

    fun refresh(silent: Boolean = false) {
        val o = remoteOwner ?: run { reloadConfig(); return }; val r = remoteRepo ?: return
        statusText = "Loading..."; statusColor = UIUtil.getContextHelpForeground()
        thread {
            try {
                val issues = runBlocking { provider.getIssues(o, r, "open", null, null) }
                val closed = runBlocking { provider.getIssues(o, r, "closed", null, null) }
                val prsOpen = runBlocking { provider.getPullRequests(o, r, "open") }
                val prsClosed = runBlocking { provider.getPullRequests(o, r, "closed") }
                val branches = runBlocking { provider.getBranches(o, r) }
                SwingUtilities.invokeLater {
                    issueData = issues + closed; prData = prsOpen + prsClosed; branchData = branches
                    statusText = "Synced successfully"; statusColor = UIUtil.getLabelForeground()
                }
            } catch (e: Exception) {
                SwingUtilities.invokeLater {
                    statusText = "Error syncing data"; statusColor = UIUtil.getErrorForeground()
                    val tw = com.intellij.openapi.wm.ToolWindowManager.getInstance(myProject)
                        .getToolWindow("Remote VCS Manager")
                    JBPopupFactory.getInstance().createHtmlTextBalloonBuilder(
                        e.message ?: "Failed to fetch data", MessageType.ERROR, null
                    ).setTitle("Sync Failed").createBalloon().show(
                        JBPopupFactory.getInstance().guessBestPopupLocation(tw?.component!!), Balloon.Position.below
                    )
                }
            }
        }
    }

    fun createIssue() {
        val o = remoteOwner ?: return; val r = remoteRepo ?: return
        CreateIssueDialog(myProject, o, r, provider).showAndGet(); refresh()
    }

    fun createPR() {
        val o = remoteOwner ?: return; val r = remoteRepo ?: return
        BrowserUtil.browse("https://github.com/$o/$r/compare")
    }

    fun checkout(name: String) {
        try {
            val root = gitDetector.detect()?.gitRoot ?: return
            Runtime.getRuntime().exec(arrayOf("git", "checkout", name), null, root)
            JOptionPane.showMessageDialog(null, "Switched to $name")
        } catch (e: Exception) {
            JOptionPane.showMessageDialog(null, "Failed: ${e.message}", "Error", JOptionPane.ERROR_MESSAGE)
        }
    }

    fun showIssueDetail(issue: Issue) { selectedIssue = issue; activeScreen = Screen.Detail }
    fun showPRDetail(pr: PullRequest) { selectedPR = pr; activeScreen = Screen.Detail }
    fun backToList() { activeScreen = Screen.List }

    companion object {
        fun fmt(iso: String): String = try {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
            val d = sdf.parse(iso.take(19)) ?: return iso
            val diff = Date().time - d.time; val m = diff / 60000; val h = m / 60; val dd = h / 24; val w = dd / 7
            when { m < 1 -> "now"; m < 60 -> "${m}m"; h < 24 -> "${h}h"; dd < 7 -> "${dd}d"; w < 5 -> "${w}w"
                else -> SimpleDateFormat("MMM d", Locale.US).format(d) }
        } catch (_: Exception) { iso }
    }
}
