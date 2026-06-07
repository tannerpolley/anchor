package com.itsjeel01.remotevcsmanager.ui.detail

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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.intellij.ide.BrowserUtil
import com.itsjeel01.remotevcsmanager.models.CommitSummary
import com.itsjeel01.remotevcsmanager.models.IssueComment
import com.itsjeel01.remotevcsmanager.models.PRState
import com.itsjeel01.remotevcsmanager.models.PullRequest
import com.itsjeel01.remotevcsmanager.providers.github.GitHubProvider
import com.itsjeel01.remotevcsmanager.ui.components.BranchPill
import com.itsjeel01.remotevcsmanager.ui.components.StateBadgeForPR
import com.itsjeel01.remotevcsmanager.ui.theme.LocalPlatformFonts
import com.itsjeel01.remotevcsmanager.ui.theme.LocalThemeColors
import kotlinx.coroutines.*
import kotlin.concurrent.thread

@Composable
fun PullRequestDetailContent(
    provider: GitHubProvider, owner: String, repo: String, pr: PullRequest,
    onBack: () -> Unit, onRefresh: () -> Unit
) {
    val theme = LocalThemeColors.current
    val fs = LocalPlatformFonts.current
    var comments by remember { mutableStateOf<List<IssueComment>>(emptyList()) }
    var commits by remember { mutableStateOf<List<CommitSummary>>(emptyList()) }
    var commitsLoading by remember { mutableStateOf(true) }
    var commentText by remember { mutableStateOf("") }
    var activeSubTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(pr.number) {
        withContext(Dispatchers.IO) {
            try { comments = provider.getIssueComments(owner, repo, pr.number) } catch (_: Exception) { }
            try { commits = provider.getPullRequestCommits(owner, repo, pr.number) } catch (_: Exception) { }
            commitsLoading = false
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(theme.Bg.primary)) {
        PRDetailHeader(pr, onBack, onRefresh, provider, owner, repo)
        TabRow(selectedTabIndex = activeSubTab, backgroundColor = theme.Bg.primary) {
            Tab(selected = activeSubTab == 0, onClick = { activeSubTab = 0 }, selectedContentColor = theme.Text.primary,
                unselectedContentColor = theme.Text.secondary) {
                Text("Conversation", fontSize = fs.label, modifier = Modifier.padding(12.dp, 8.dp))
            }
            Tab(selected = activeSubTab == 1, onClick = { activeSubTab = 1 }, selectedContentColor = theme.Text.primary,
                unselectedContentColor = theme.Text.secondary) {
                Text("Commits", fontSize = fs.label, modifier = Modifier.padding(12.dp, 8.dp))
            }
        }
        Divider(color = theme.divider, thickness = 0.5.dp)
        when (activeSubTab) {
            0 -> ConversationTab(pr, comments, commentText, { commentText = it }, {
                if (commentText.isNotBlank()) { val t = commentText; commentText = ""
                    bg({ provider.addIssueComment(owner, repo, pr.number, t) }) {
                        thread { try { comments = runBlocking { provider.getIssueComments(owner, repo, pr.number) } } catch (_: Exception) { } } } }
            })
            1 -> CommitsTab(commits, commitsLoading)
        }
    }
}

@Composable
fun PRDetailHeader(pr: PullRequest, onBack: () -> Unit, onRefresh: () -> Unit,
                   provider: GitHubProvider, owner: String, repo: String) {
                       val theme = LocalThemeColors.current
                       val fs = LocalPlatformFonts.current
    Column(modifier = Modifier.fillMaxWidth().background(theme.Bg.surface).padding(8.dp, 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack, modifier = Modifier.padding(0.dp)) { Text("←", fontSize = fs.title, color = theme.Text.primary) }
            Text("#${pr.number} ${pr.title}", fontWeight = FontWeight.Bold, fontSize = fs.title, color = theme.Text.primary,
                maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            TextButton(onClick = { BrowserUtil.browse(pr.url) }) { Text("↗", fontSize = fs.small, color = theme.Text.link) }
        }
        Row(modifier = Modifier.padding(start = 48.dp), verticalAlignment = Alignment.CenterVertically) {
            StateBadgeForPR(pr.state); Spacer(Modifier.width(6.dp))
            Text(pr.author, fontSize = fs.small, color = theme.Text.secondary); Spacer(Modifier.width(4.dp))
            Text(fmt(pr.createdAt), fontSize = fs.small, color = theme.Text.secondary); Spacer(Modifier.width(6.dp))
            BranchPill(pr.sourceBranch)
            Text(" → ", fontSize = fs.small, color = theme.Text.secondary)
            BranchPill(pr.targetBranch); Spacer(Modifier.weight(1f))
            if (pr.state != PRState.MERGED) {
                val isOpen = pr.state == PRState.OPEN
                TextButton(onClick = {
                    if (isOpen) bg({ provider.updateIssue(owner, repo, pr.number, state = "closed") }, onRefresh)
                    else bg({ provider.updateIssue(owner, repo, pr.number, state = "open") }, onRefresh)
                }, colors = ButtonDefaults.textButtonColors(contentColor = theme.Text.onAccent,
                    backgroundColor = if (isOpen) theme.Accent.red else theme.Accent.green),
                    modifier = Modifier.height(26.dp)) { Text(if (isOpen) "Close" else "Reopen", fontSize = fs.small) }
            }
        }
    }
    Divider(color = theme.divider, thickness = 0.5.dp)
}

@Composable
fun ConversationTab(pr: PullRequest, comments: List<IssueComment>,
                    commentText: String, onTextChange: (String) -> Unit, onSubmit: () -> Unit) {
                        val theme = LocalThemeColors.current
                        val fs = LocalPlatformFonts.current
    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
            item {
                Spacer(Modifier.height(8.dp))
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(6.dp), elevation = 0.dp,
                    backgroundColor = theme.Bg.card, border = BorderStroke(0.5.dp, theme.Border.default.copy(alpha = 0.4f))) {
                    Column(Modifier.padding(8.dp)) {
                        Text("Description", fontWeight = FontWeight.Bold, fontSize = fs.xsmall, color = theme.Text.secondary)
                        Spacer(Modifier.height(4.dp))
                        MarkdownCompose.Block(pr.description ?: "_No description provided._")
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            if (comments.isNotEmpty()) {
                item { Text("Comments (${comments.size})", fontWeight = FontWeight.Bold, fontSize = fs.label, color = theme.Text.secondary) }
                items(comments) { CommentCard(it); Spacer(Modifier.height(6.dp)) }
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
        CommentInputBar(commentText, onTextChange, onSubmit)
    }
}

@Composable
fun CommitsTab(commits: List<CommitSummary>, loading: Boolean) {
    val theme = LocalThemeColors.current
    val fs = LocalPlatformFonts.current
    if (loading) {
        Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
            Text("Loading commits...", color = theme.Text.disabled, fontSize = fs.label)
        }
    } else if (commits.isEmpty()) {
        Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
            Text("No commits found", color = theme.Text.disabled, fontSize = fs.label)
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
            items(commits) { commit ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(commit.sha.take(7), fontFamily = FontFamily.Monospace, fontSize = fs.mono, color = theme.Text.link,
                        modifier = Modifier.clickable { if (commit.url.isNotBlank()) BrowserUtil.browse(commit.url) })
                    Spacer(Modifier.width(8.dp))
                    Text(commit.message, fontFamily = FontFamily.Monospace, fontSize = fs.mono, color = theme.Text.primary,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Divider(color = theme.dividerSubtle, thickness = 0.5.dp)
            }
        }
    }
}
