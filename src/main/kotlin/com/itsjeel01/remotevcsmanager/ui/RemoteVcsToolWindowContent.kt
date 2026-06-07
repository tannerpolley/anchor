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
import androidx.compose.ui.unit.sp
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.project.Project
import com.itsjeel01.remotevcsmanager.models.*
import com.itsjeel01.remotevcsmanager.ui.components.CompactButton
import com.itsjeel01.remotevcsmanager.ui.components.StateBadge
import com.itsjeel01.remotevcsmanager.ui.detail.IssueDetailContent
import com.itsjeel01.remotevcsmanager.ui.detail.PullRequestDetailContent
import com.itsjeel01.remotevcsmanager.ui.theme.*

@Composable
fun RemoteVcsToolWindowContent(project: Project) {
    val state = remember { ToolWindowState(project) }
    val theme = rememberThemeColors()
    val fs = rememberPlatformFonts()

    CompositionLocalProvider(LocalThemeColors provides theme, LocalPlatformFonts provides fs) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(theme.Bg.primary)
                .onKeyEvent { event ->
                    if (event.type == KeyEventType.KeyUp) {
                        when {
                            event.key == Key.R && state.activeScreen is Screen.List -> {
                                state.refresh()
                                true
                            }
                            event.key == Key.Escape && state.activeScreen is Screen.Detail -> {
                                state.backToList()
                                true
                            }
                            else -> false
                        }
                    } else false
                }
        ) {
            when (state.activeScreen) {
                is Screen.List -> MainListScreen(state)
                is Screen.Detail -> DetailScreen(state)
            }
        }
    }
}

@Composable
fun MainListScreen(state: ToolWindowState) {
    val theme = rememberThemeColors()
    val fs = rememberPlatformFonts()
    var activeTab by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        HeaderBar(state)

        TabRow(selectedTabIndex = activeTab, backgroundColor = theme.Bg.primary) {
            Tab(
                selected = activeTab == 0, onClick = { activeTab = 0 },
                selectedContentColor = theme.Text.primary,
                unselectedContentColor = theme.Text.secondary
            ) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    TabLabel("Issues", state.issueData.size, fs)
                }
            }
            Tab(
                selected = activeTab == 1, onClick = { activeTab = 1 },
                selectedContentColor = theme.Text.primary,
                unselectedContentColor = theme.Text.secondary
            ) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    TabLabel("PRs", state.prData.size, fs)
                }
            }
            Tab(
                selected = activeTab == 2, onClick = { activeTab = 2 },
                selectedContentColor = theme.Text.primary,
                unselectedContentColor = theme.Text.secondary
            ) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    TabLabel("Branches", state.branchData.size, fs)
                }
            }
        }

        Divider(color = theme.divider, thickness = 0.5.dp)
        when (activeTab) {
            0 -> IssuesPanel(state)
            1 -> PRsPanel(state)
            2 -> BranchesPanel(state)
        }
        StatusBar(state)
    }
}

@Composable
private fun TabLabel(name: String, count: Int, fs: PlatformFonts) {
    Text("$name ($count)", fontSize = fs.mono)
}

@Composable
fun HeaderBar(state: ToolWindowState) {
    val theme = rememberThemeColors()
    val fs = rememberPlatformFonts()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(theme.Bg.surface)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (state.remoteDetected) "${state.remoteOwner}/${state.remoteRepo}" else "No remote",
            color = theme.Text.primary,
            fontWeight = FontWeight.Bold,
            fontSize = fs.mono
        )
        if (state.currentBranch != null) {
            Text(
                text = "  ·  ${state.currentBranch}",
                color = theme.Text.secondary,
                fontSize = fs.small,
                modifier = Modifier.padding(start = 4.dp)
            )
        }

        Spacer(Modifier.weight(1f))

        CompactButton(
            text = "New Issue",
            onClick = { state.createIssue() },
            backgroundColor = theme.Button.background
        )

        Spacer(Modifier.width(6.dp))

        CompactButton(
            text = "Pull Request",
            onClick = { state.createPR() },
            backgroundColor = theme.Button.background
        )
    }
    Divider(color = theme.divider, thickness = 0.5.dp)
}

@Composable
fun StatusBar(state: ToolWindowState) {
    val theme = rememberThemeColors()
    val fs = rememberPlatformFonts()
    val awt = state.statusColor
    val color = Color(awt.red / 255f, awt.green / 255f, awt.blue / 255f, awt.alpha / 255f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(theme.Bg.surface)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(state.statusText, color = color, fontSize = fs.xsmall)
    }
}

@Composable
fun IssuesPanel(state: ToolWindowState) {
    val theme = rememberThemeColors()
    val fs = rememberPlatformFonts()

    FilterChipRow(
        mapOf("all" to "All", "open" to "Open", "closed" to "Closed"),
        state.issueFilterState
    ) { state.issueFilterState = it }

    val filtered = remember(state.issueData, state.issueFilterState) {
        state.issueData.filter { item ->
            when (state.issueFilterState) {
                "open" -> item.state == IssueState.OPEN
                "closed" -> item.state == IssueState.CLOSED
                else -> true
            }
        }
    }

    if (filtered.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No issues found", color = theme.Text.disabled, fontSize = fs.mono)
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(filtered) { issue -> IssueRow(issue, state) }
        }
    }
}

@Composable
fun IssueRow(issue: Issue, state: ToolWindowState) {
    val theme = rememberThemeColors()
    val fs = rememberPlatformFonts()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { state.showIssueDetail(issue) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RowMetaRow(
                number = issue.number,
                title = issue.title,
                badge = { StateBadge(issueState = issue.state) },
                meta = "by ${issue.author} · ${ToolWindowState.fmt(issue.updatedAt)}",
                url = issue.url,
                theme = theme,
                fs = fs,
                modifier = Modifier.weight(1f)
            )
        }

        Divider(color = theme.divider, thickness = 0.5.dp)
    }
}

@Composable
fun PRsPanel(state: ToolWindowState) {
    val theme = rememberThemeColors()
    val fs = rememberPlatformFonts()

    FilterChipRow(
        mapOf("open" to "Open", "merged" to "Merged", "closed" to "Closed", "all" to "All"),
        state.prFilterState
    ) { state.prFilterState = it }

    val filtered = remember(state.prData, state.prFilterState) {
        state.prData.filter { item ->
            when (state.prFilterState) {
                "open" -> item.state == PRState.OPEN
                "merged" -> item.state == PRState.MERGED
                "closed" -> item.state == PRState.CLOSED
                else -> true
            }
        }
    }

    if (filtered.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No pull requests found", color = theme.Text.disabled, fontSize = fs.mono)
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(filtered) { pr -> PullRequestRow(pr, state) }
        }
    }
}

@Composable
fun PullRequestRow(pr: PullRequest, state: ToolWindowState) {
    val theme = rememberThemeColors()
    val fs = rememberPlatformFonts()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { state.showPRDetail(pr) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RowMetaRow(
                number = pr.number,
                title = pr.title,
                badge = { StateBadge(prState = pr.state) },
                meta = "${pr.sourceBranch} → ${pr.targetBranch} · ${ToolWindowState.fmt(pr.updatedAt)}",
                url = pr.url,
                theme = theme,
                fs = fs,
                modifier = Modifier.weight(1f)
            )
        }

        Divider(color = theme.divider, thickness = 0.5.dp)
    }
}

@Composable
private fun RowMetaRow(
    number: Int,
    title: String,
    badge: @Composable () -> Unit,
    meta: String,
    url: String,
    theme: ThemeColors,
    fs: PlatformFonts,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "#$number",
                color = theme.Text.link,
                fontWeight = FontWeight.Bold,
                fontSize = fs.small,
                modifier = Modifier.clickable { BrowserUtil.browse(url) }
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = title,
                color = theme.Text.primary,
                fontSize = fs.mono,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 6.dp)
        ) {
            badge()
            Text(text = meta, color = theme.Text.secondary, fontSize = fs.xsmall)
        }
    }
}

@Composable
fun BranchesPanel(state: ToolWindowState) {
    val theme = rememberThemeColors()
    val fs = rememberPlatformFonts()

    if (state.branchData.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No branches found", color = theme.Text.disabled, fontSize = fs.mono)
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(state.branchData) { branch -> BranchRow(branch, state) }
        }
    }
}

@Composable
fun BranchRow(branch: GitBranch, state: ToolWindowState) {
    val theme = rememberThemeColors()
    val fs = rememberPlatformFonts()
    val owner = state.remoteOwner
    val repo = state.remoteRepo

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { state.checkout(branch.name) }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = branch.name,
            color = theme.Text.link,
            fontSize = fs.mono,
            modifier = Modifier
                .weight(1f)
                .clickable { state.checkout(branch.name) }
        )
        Text(
            text = branch.sha.take(7),
            color = theme.Text.secondary,
            fontSize = fs.xsmall,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.clickable {
                if (owner != null && repo != null) {
                    BrowserUtil.browse("https://github.com/$owner/$repo/tree/${branch.name}")
                }
            }
        )
    }

    Divider(color = theme.dividerSubtle, thickness = 0.5.dp)
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun FilterChipRow(
    options: Map<String, String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    val theme = rememberThemeColors()
    val fs = rememberPlatformFonts()

    Row(
        modifier = Modifier
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEach { (value, label) ->
            val isSelected = value == selected
            Surface(
                onClick = { onSelect(value) },
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) theme.Bg.selected else theme.Bg.surface,
                border = BorderStroke(
                    1.dp,
                    if (isSelected) theme.Button.background.copy(alpha = 0.5f) else theme.Border.default.copy(alpha = 0.4f)
                )
            ) {
                Text(
                    text = " $label ",
                    fontSize = fs.small,
                    color = if (isSelected) theme.Text.primary else theme.Text.secondary,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
fun DetailScreen(state: ToolWindowState) {
    val issue = state.selectedIssue
    val pr = state.selectedPR

    when {
        issue != null -> IssueDetailContent(
            provider = state.provider,
            owner = state.remoteOwner ?: "",
            repo = state.remoteRepo ?: "",
            issue = issue,
            onBack = { state.backToList() },
            onRefresh = { state.refresh(silent = true) }
        )
        pr != null -> PullRequestDetailContent(
            provider = state.provider,
            owner = state.remoteOwner ?: "",
            repo = state.remoteRepo ?: "",
            pr = pr,
            onBack = { state.backToList() },
            onRefresh = { state.refresh(silent = true) }
        )
        else -> state.backToList()
    }
}
