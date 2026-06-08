package com.itsjeel01.remotevcsmanager.ui.detail

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.itsjeel01.remotevcsmanager.models.CommitSummary
import com.itsjeel01.remotevcsmanager.models.IssueComment
import com.itsjeel01.remotevcsmanager.models.PRState
import com.itsjeel01.remotevcsmanager.models.PullRequest
import com.itsjeel01.remotevcsmanager.providers.github.GitHubProvider
import com.itsjeel01.remotevcsmanager.ui.components.BranchPill
import com.itsjeel01.remotevcsmanager.ui.components.ButtonVariant
import com.itsjeel01.remotevcsmanager.ui.components.CompactButton
import com.itsjeel01.remotevcsmanager.ui.components.StateBadgeForPR
import com.itsjeel01.remotevcsmanager.ui.components.ClickableIcon
import com.itsjeel01.remotevcsmanager.ui.TimeFormat
import com.itsjeel01.remotevcsmanager.ui.theme.LocalPlatformFonts
import com.itsjeel01.remotevcsmanager.ui.theme.LocalThemeColors
import kotlinx.coroutines.*
import kotlin.concurrent.thread

@Composable
fun PullRequestDetailContent(
    provider: GitHubProvider, owner: String, repo: String, pr: PullRequest,
    onBack: () -> Unit, onRefresh: () -> Unit, onStateToggle: (PRState) -> Unit
) {
    val theme = LocalThemeColors.current
    val fs = LocalPlatformFonts.current
    var comments by remember { mutableStateOf<List<IssueComment>>(emptyList()) }
    var commits by remember { mutableStateOf<List<CommitSummary>>(emptyList()) }
    var commentText by remember { mutableStateOf("") }

    LaunchedEffect(pr.number) {
        withContext(Dispatchers.IO) {
            try { comments = provider.getIssueComments(owner, repo, pr.number) } catch (_: Exception) { }
            try { commits = provider.getPullRequestCommits(owner, repo, pr.number) } catch (_: Exception) { }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.Bg.primary)
    ) {
        PRDetailHeader(pr, onBack, onRefresh, provider, owner, repo)
        VcsDetailHtmlRenderer(
            description = pr.description,
            comments = comments,
            commits = commits,
            provider = provider,
            context = "$owner/$repo",
            modifier = Modifier.weight(1f)
        )
        val isOpen = pr.state == PRState.OPEN
        CommentInputBar(commentText, { commentText = it }, {
            if (commentText.isNotBlank()) {
                val t = commentText
                commentText = ""
                bg({ provider.addIssueComment(owner, repo, pr.number, t) }) {
                    thread {
                        try {
                            comments = runBlocking { provider.getIssueComments(owner, repo, pr.number) }
                        } catch (_: Exception) { }
                    }
                }
            }
        }, extraActions = {
            if (pr.state != PRState.MERGED) {
                CompactButton(
                    text = if (isOpen) "Close" else "Reopen",
                    onClick = {
                        val newState = if (isOpen) PRState.CLOSED else PRState.OPEN
                        bg({ provider.updateIssue(owner, repo, pr.number, state = newState.name.lowercase()) }) {
                            onStateToggle(newState)
                            onBack()
                        }
                    },
                    variant = ButtonVariant.Primary
                )
            }
        })
    }
}

@Composable
fun PRDetailHeader(
    pr: PullRequest, onBack: () -> Unit,
    onRefresh: () -> Unit,
    provider: GitHubProvider,
    owner: String,
    repo: String
) {
    val theme = LocalThemeColors.current
    val fs = LocalPlatformFonts.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(theme.Bg.surface)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ClickableIcon(AllIcons.Actions.Back, onClick = onBack, description = "Back")
            Text(
                "#${pr.number} ${pr.title}",
                fontWeight = FontWeight.Bold,
                fontSize = fs.title,
                color = theme.Text.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            ClickableIcon(AllIcons.Ide.External_link_arrow, onClick = { BrowserUtil.browse(pr.url) }, description = "Open in browser")
        }

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.padding(start = 28.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StateBadgeForPR(pr.state)
            Text(pr.author, fontSize = fs.small, color = theme.Text.secondary)
            Text(TimeFormat.relative(pr.createdAt), fontSize = fs.small, color = theme.Text.secondary)

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                BranchPill(
                    branchName = pr.sourceBranch,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Text("→", fontSize = fs.small, color = theme.Text.secondary)
                BranchPill(
                    branchName = pr.targetBranch,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }
        }
    }
    Divider(color = theme.divider, thickness = 0.5.dp)
}


