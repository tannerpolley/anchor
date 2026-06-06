package com.itsjeel01.remotevcsmanager.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
import com.itsjeel01.remotevcsmanager.ui.components.StateBadge
import com.itsjeel01.remotevcsmanager.ui.detail.IssueDetailContent
import com.itsjeel01.remotevcsmanager.ui.detail.PullRequestDetailContent
import com.itsjeel01.remotevcsmanager.ui.theme.PlatformFonts
import com.itsjeel01.remotevcsmanager.ui.theme.ThemeColors
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.*
import javax.swing.JOptionPane
import javax.swing.SwingUtilities
import kotlin.concurrent.thread

// ── State holder (business logic — NOT UI) ────────────────────────────────

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

    var activeScreen by mutableStateOf<Screen>(Screen.List)
    var issueFilterState by mutableStateOf("all")
    var prFilterState by mutableStateOf("open")

    var issueData by mutableStateOf<List<Issue>>(emptyList())
    var prData by mutableStateOf<List<PullRequest>>(emptyList())
    var branchData by mutableStateOf<List<GitBranch>>(emptyList())
    var selectedIssue by mutableStateOf<Issue?>(null)
    var selectedPR by mutableStateOf<PullRequest?>(null)

    init {
        reloadConfig()
        val connection = ApplicationManager.getApplication().messageBus.connect()
        connection.subscribe(
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

    fun createIssue() { val o = remoteOwner ?: return; val r = remoteRepo ?: return
        CreateIssueDialog(myProject, o, r, provider).showAndGet(); refresh() }
    fun createPR() { val o = remoteOwner ?: return; val r = remoteRepo ?: return
        BrowserUtil.browse("https://github.com/$o/$r/compare") }
    fun checkout(name: String) { try { val root = gitDetector.detect()?.gitRoot ?: return
            Runtime.getRuntime().exec(arrayOf("git", "checkout", name), null, root)
            JOptionPane.showMessageDialog(null, "Switched to $name")
        } catch (e: Exception) { JOptionPane.showMessageDialog(null, "Failed: ${e.message}", "Error", JOptionPane.ERROR_MESSAGE) } }
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

sealed class Screen { data object List : Screen(); data object Detail : Screen() }

// ── Main Composable ────────────────────────────────────────────────────────

@Composable
fun RemoteVcsToolWindowContent(project: Project) {
    val state = remember { ToolWindowState(project) }
    Box(modifier = Modifier.fillMaxSize().background(ThemeColors.Bg.primary).onKeyEvent { event ->
        if (event.type == KeyEventType.KeyUp) {
            when {
                event.key == Key.R && state.activeScreen is Screen.List -> { state.refresh(); true }
                event.key == Key.Escape && state.activeScreen is Screen.Detail -> { state.backToList(); true }
                else -> false
            }
        } else false
    }) {
        when (state.activeScreen) {
            is Screen.List -> MainListScreen(state)
            is Screen.Detail -> DetailScreen(state)
        }
    }
}

// ── List Screen ────────────────────────────────────────────────────────────

@Composable
fun MainListScreen(state: ToolWindowState) {
    val fs = PlatformFonts.current()
    Column(modifier = Modifier.fillMaxSize()) {
        HeaderBar(state, fs)
        var activeTab by remember { mutableIntStateOf(0) }
        TabRow(selectedTabIndex = activeTab, backgroundColor = ThemeColors.Bg.primary) {
            Tab(selected = activeTab == 0, onClick = { activeTab = 0 }, selectedContentColor = ThemeColors.Text.primary,
                unselectedContentColor = ThemeColors.Text.secondary) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Text("Issues (${state.issueData.size})", fontSize = fs.label)
                }
            }
            Tab(selected = activeTab == 1, onClick = { activeTab = 1 }, selectedContentColor = ThemeColors.Text.primary,
                unselectedContentColor = ThemeColors.Text.secondary) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Text("PRs (${state.prData.size})", fontSize = fs.label)
                }
            }
            Tab(selected = activeTab == 2, onClick = { activeTab = 2 }, selectedContentColor = ThemeColors.Text.primary,
                unselectedContentColor = ThemeColors.Text.secondary) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Text("Branches (${state.branchData.size})", fontSize = fs.label)
                }
            }
        }
        Divider(color = ThemeColors.divider, thickness = 0.5.dp)
        when (activeTab) {
            0 -> IssuesPanel(state, fs)
            1 -> PRsPanel(state, fs)
            2 -> BranchesPanel(state, fs)
        }
        StatusBar(state, fs)
    }
}

@Composable
fun HeaderBar(state: ToolWindowState, fs: PlatformFonts.FontScale) {
    Row(
        modifier = Modifier.fillMaxWidth().background(ThemeColors.Bg.surface).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            if (state.remoteDetected) "${state.remoteOwner}/${state.remoteRepo}" else "No remote",
            color = ThemeColors.Text.primary, fontWeight = FontWeight.Bold, fontSize = fs.label
        )
        if (state.currentBranch != null) {
            Text("  ·  ${state.currentBranch}", color = ThemeColors.Text.secondary, fontSize = fs.small,
                modifier = Modifier.padding(start = 4.dp))
        }
        Spacer(Modifier.weight(1f))
        TextButton(
            onClick = { state.createIssue() },
            colors = ButtonDefaults.textButtonColors(contentColor = ThemeColors.Text.onAccent),
            modifier = Modifier.background(ThemeColors.Accent.green, RoundedCornerShape(4.dp)).height(26.dp)
        ) { Text("+ Issue", fontSize = fs.small) }
        Spacer(Modifier.width(6.dp))
        TextButton(
            onClick = { state.createPR() },
            colors = ButtonDefaults.textButtonColors(contentColor = ThemeColors.Text.onAccent),
            modifier = Modifier.background(ThemeColors.Accent.blue, RoundedCornerShape(4.dp)).height(26.dp)
        ) { Text("+ PR", fontSize = fs.small) }
    }
    Divider(color = ThemeColors.divider, thickness = 0.5.dp)
}

@Composable
fun StatusBar(state: ToolWindowState, fs: PlatformFonts.FontScale) {
    val awt = state.statusColor
    val color = Color(awt.red / 255f, awt.green / 255f, awt.blue / 255f, awt.alpha / 255f)
    Row(
        modifier = Modifier.fillMaxWidth().background(ThemeColors.Bg.surface).padding(horizontal = 8.dp, vertical = 2.dp)
    ) { Text(state.statusText, color = color, fontSize = fs.xsmall) }
}

// ── Issues Panel ───────────────────────────────────────────────────────────

@Composable
fun IssuesPanel(state: ToolWindowState, fs: PlatformFonts.FontScale) {
    FilterChipRow(mapOf("all" to "All", "open" to "Open", "closed" to "Closed"), state.issueFilterState, fs) { state.issueFilterState = it }
    val filtered = remember(state.issueData, state.issueFilterState) {
        state.issueData.filter { when (state.issueFilterState) { "open" -> it.state == IssueState.OPEN; "closed" -> it.state == IssueState.CLOSED; else -> true } }
    }
    if (filtered.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No issues found", color = ThemeColors.Text.disabled, fontSize = fs.label) }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) { items(filtered) { IssueRow(it, state, fs) } }
    }
}

@Composable
fun IssueRow(issue: Issue, state: ToolWindowState, fs: PlatformFonts.FontScale) {
    Column(modifier = Modifier.fillMaxWidth().clickable { state.showIssueDetail(issue) }.padding(0.dp)) {
        Row(modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            StateBadge(issueState = issue.state); Spacer(Modifier.width(8.dp))
            Text("#${issue.number}", color = ThemeColors.Text.link, fontWeight = FontWeight.Bold, fontSize = fs.small,
                modifier = Modifier.clickable { BrowserUtil.browse(issue.url) })
            Spacer(Modifier.width(6.dp))
            Text(issue.title, color = ThemeColors.Text.primary, fontSize = fs.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Row(modifier = Modifier.padding(start = 56.dp, bottom = 6.dp)) {
            Text("by ${issue.author} · ${ToolWindowState.fmt(issue.updatedAt)}", color = ThemeColors.Text.secondary, fontSize = fs.xsmall)
        }
        Divider(color = ThemeColors.dividerSubtle, thickness = 0.5.dp)
    }
}

// ── PRs Panel ──────────────────────────────────────────────────────────────

@Composable
fun PRsPanel(state: ToolWindowState, fs: PlatformFonts.FontScale) {
    FilterChipRow(mapOf("open" to "Open", "merged" to "Merged", "closed" to "Closed", "all" to "All"), state.prFilterState, fs) { state.prFilterState = it }
    val filtered = remember(state.prData, state.prFilterState) {
        state.prData.filter { when (state.prFilterState) { "open" -> it.state == PRState.OPEN; "merged" -> it.state == PRState.MERGED; "closed" -> it.state == PRState.CLOSED; else -> true } }
    }
    if (filtered.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No pull requests found", color = ThemeColors.Text.disabled, fontSize = fs.label) }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) { items(filtered) { PullRequestRow(it, state, fs) } }
    }
}

@Composable
fun PullRequestRow(pr: PullRequest, state: ToolWindowState, fs: PlatformFonts.FontScale) {
    Column(modifier = Modifier.fillMaxWidth().clickable { state.showPRDetail(pr) }.padding(0.dp)) {
        Row(modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            StateBadge(prState = pr.state); Spacer(Modifier.width(8.dp))
            Text("#${pr.number}", color = ThemeColors.Text.link, fontWeight = FontWeight.Bold, fontSize = fs.small,
                modifier = Modifier.clickable { BrowserUtil.browse(pr.url) })
            Spacer(Modifier.width(6.dp))
            Text(pr.title, color = ThemeColors.Text.primary, fontSize = fs.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Row(modifier = Modifier.padding(start = 56.dp, bottom = 6.dp)) {
            Text("${pr.sourceBranch} → ${pr.targetBranch} · ${ToolWindowState.fmt(pr.updatedAt)}", color = ThemeColors.Text.secondary, fontSize = fs.xsmall)
        }
        Divider(color = ThemeColors.dividerSubtle, thickness = 0.5.dp)
    }
}

// ── Branches Panel ─────────────────────────────────────────────────────────

@Composable
fun BranchesPanel(state: ToolWindowState, fs: PlatformFonts.FontScale) {
    if (state.branchData.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No branches found", color = ThemeColors.Text.disabled, fontSize = fs.label) }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) { items(state.branchData) { BranchRow(it, state, fs) } }
    }
}

@Composable
fun BranchRow(branch: GitBranch, state: ToolWindowState, fs: PlatformFonts.FontScale) {
    val owner = state.remoteOwner; val repo = state.remoteRepo
    Row(modifier = Modifier.fillMaxWidth().clickable { state.checkout(branch.name) }.padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Text(branch.name, color = ThemeColors.Text.link, fontSize = fs.mono,
            modifier = Modifier.weight(1f).clickable { state.checkout(branch.name) })
        Text(branch.sha.take(7), color = ThemeColors.Text.secondary, fontSize = fs.xsmall, fontFamily = FontFamily.Monospace,
            modifier = Modifier.clickable { owner?.let { o -> repo?.let { r -> BrowserUtil.browse("https://github.com/$o/$r/tree/${branch.name}") } } })
    }
    Divider(color = ThemeColors.dividerSubtle, thickness = 0.5.dp)
}

// ── Filter Chips ───────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun FilterChipRow(options: Map<String, String>, selected: String, fs: PlatformFonts.FontScale, onSelect: (String) -> Unit) {
    Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        options.forEach { (value, label) ->
            val isSel = value == selected
            Surface(
                onClick = { onSelect(value) },
                shape = RoundedCornerShape(12.dp),
                color = if (isSel) ThemeColors.Accent.blue.copy(alpha = 0.2f) else ThemeColors.Bg.surface,
                border = BorderStroke(1.dp, if (isSel) ThemeColors.Accent.blue.copy(alpha = 0.5f) else ThemeColors.Border.default.copy(alpha = 0.4f))
            ) {
                Text(" $label ", fontSize = fs.small,
                    color = if (isSel) ThemeColors.Accent.blue else ThemeColors.Text.secondary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
            }
        }
    }
}

// ── Detail Screen dispatcher ───────────────────────────────────────────────

@Composable
fun DetailScreen(state: ToolWindowState) {
    val issue = state.selectedIssue; val pr = state.selectedPR
    when {
        issue != null -> IssueDetailContent(provider = state.provider, owner = state.remoteOwner ?: "", repo = state.remoteRepo ?: "",
            issue = issue, onBack = { state.backToList() }, onRefresh = { state.refresh(silent = true) })
        pr != null -> PullRequestDetailContent(provider = state.provider, owner = state.remoteOwner ?: "", repo = state.remoteRepo ?: "",
            pr = pr, onBack = { state.backToList() }, onRefresh = { state.refresh(silent = true) })
        else -> state.backToList()
    }
}
