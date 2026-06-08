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
import javax.swing.SwingUtilities
import kotlin.concurrent.thread

sealed class Screen { data object List : Screen(); data object Detail : Screen() }

enum class SyncPhase {
    IDLE,
    FETCHING_ISSUES,
    FETCHING_PRS,
    FETCHING_BRANCHES,
    RENDERING,
    ERROR
}

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
    var syncPhase by mutableStateOf(SyncPhase.IDLE)
    var lastSyncTime by mutableStateOf<String?>(null)

    var activeScreen: Screen by mutableStateOf(Screen.List)
    var issueFilterState by mutableStateOf("all")
    var prFilterState by mutableStateOf("open")

    var issueData by mutableStateOf<List<Issue>>(emptyList())
    var prData by mutableStateOf<List<PullRequest>>(emptyList())
    var branchData by mutableStateOf<List<GitBranch>>(emptyList())
    var selectedIssue by mutableStateOf<Issue?>(null)
    var selectedPR by mutableStateOf<PullRequest?>(null)

    var issueCount by mutableStateOf(0)
    var prCount by mutableStateOf(0)
    var branchCount by mutableStateOf(0)
    var activeTab by mutableStateOf(0)
    var gitRoot by mutableStateOf<java.io.File?>(null)

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
                gitRoot = info.gitRoot
            }
            activeScreen = Screen.List; refresh()
        } else {
            remoteOwner = null; remoteRepo = null; currentBranch = null
            statusText = if (!gitDetector.hasRemote()) "Set a git remote first" else "Configure token in Settings"
            statusColor = UIUtil.getContextHelpForeground()
            syncPhase = SyncPhase.IDLE
            issueData = emptyList(); prData = emptyList(); branchData = emptyList()
        }
    }

    fun refresh(silent: Boolean = false) {
        val o = remoteOwner ?: run { reloadConfig(); return }; val r = remoteRepo ?: return
        syncPhase = SyncPhase.FETCHING_ISSUES
        statusText = "Fetching issues..."
        statusColor = UIUtil.getContextHelpForeground()
        thread {
            try {
                val issues = runBlocking { provider.getIssues(o, r, "open", null, null) }
                val closed = runBlocking { provider.getIssues(o, r, "closed", null, null) }
                val combined = issues + closed
                SwingUtilities.invokeLater {
                    issueData = combined
                    issueCount = combined.size
                    syncPhase = SyncPhase.FETCHING_PRS
                    statusText = "Fetching pull requests..."
                }

                val prsOpen = runBlocking { provider.getPullRequests(o, r, "open") }
                val prsClosed = runBlocking { provider.getPullRequests(o, r, "closed") }
                val combinedPR = prsOpen + prsClosed
                SwingUtilities.invokeLater {
                    prData = combinedPR
                    prCount = combinedPR.size
                    syncPhase = SyncPhase.FETCHING_BRANCHES
                    statusText = "Fetching branches..."
                }

                val branches = runBlocking { provider.getBranches(o, r) }
                val now = TimeFormat.now()
                SwingUtilities.invokeLater {
                    branchData = branches
                    branchCount = branches.size
                    lastSyncTime = now
                    syncPhase = SyncPhase.IDLE
                    statusText = "Synced"
                    statusColor = UIUtil.getLabelForeground()
                }
            } catch (e: Exception) {
                SwingUtilities.invokeLater {
                    syncPhase = SyncPhase.ERROR
                    statusText = "Sync failed"
                    statusColor = UIUtil.getErrorForeground()
                    val tw = com.intellij.openapi.wm.ToolWindowManager.getInstance(myProject)
                        .getToolWindow("Remote VCS")
                    if (tw != null) {
                        JBPopupFactory.getInstance().createHtmlTextBalloonBuilder(
                            e.message ?: "Failed to fetch data", MessageType.ERROR, null
                        ).setTitle("Sync Failed").createBalloon().show(
                            JBPopupFactory.getInstance().guessBestPopupLocation(tw.component), Balloon.Position.below
                        )
                    }
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
        val repo = try {
            git4idea.repo.GitRepositoryManager.getInstance(myProject).repositories
                .firstOrNull { it.root.toNioPath().toString() == gitRoot?.toPath()?.toString() }
                ?: git4idea.repo.GitRepositoryManager.getInstance(myProject).repositories.firstOrNull()
        } catch (_: Exception) { null }
        if (repo == null) {
            PluginNotifications.error(myProject, "Checkout failed", "No git repository found")
            return
        }
        syncPhase = SyncPhase.RENDERING
        statusText = "Switching to $name…"
        statusColor = UIUtil.getContextHelpForeground()
        val brancher = git4idea.branch.GitBrancher.getInstance(myProject)
        brancher.checkout(name, false, listOf(repo), java.lang.Runnable {
            SwingUtilities.invokeLater {
                currentBranch = name
                statusText = "On $name"
                statusColor = UIUtil.getLabelForeground()
                syncPhase = SyncPhase.IDLE
                PluginNotifications.info(myProject, "Switched", "Now on $name")
            }
        })
    }

    fun showIssueDetail(issue: Issue) { selectedIssue = issue; selectedPR = null; activeTab = 0; activeScreen = Screen.Detail }
    fun showPRDetail(pr: PullRequest) { selectedPR = pr; selectedIssue = null; activeTab = 1; activeScreen = Screen.Detail }
    fun backToList() { selectedIssue = null; selectedPR = null; activeScreen = Screen.List }
}
