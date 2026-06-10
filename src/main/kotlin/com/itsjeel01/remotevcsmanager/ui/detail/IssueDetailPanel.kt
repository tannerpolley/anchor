package com.itsjeel01.remotevcsmanager.ui.detail

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.RowScope
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.itsjeel01.remotevcsmanager.models.Issue
import com.itsjeel01.remotevcsmanager.models.IssueComment
import com.itsjeel01.remotevcsmanager.models.IssueState
import com.itsjeel01.remotevcsmanager.providers.github.GitHubProvider
import com.itsjeel01.remotevcsmanager.ui.components.ButtonVariant
import com.itsjeel01.remotevcsmanager.ui.components.CompactButton
import com.itsjeel01.remotevcsmanager.ui.components.LabelChip
import com.itsjeel01.remotevcsmanager.ui.components.StateBadgeForIssue
import com.itsjeel01.remotevcsmanager.ui.components.ClickableIcon
import com.itsjeel01.remotevcsmanager.ui.TimeFormat
import com.itsjeel01.remotevcsmanager.ui.theme.LocalPlatformFonts
import com.itsjeel01.remotevcsmanager.ui.theme.LocalThemeColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import javax.swing.JOptionPane
import javax.swing.SwingUtilities
import kotlin.concurrent.thread

@Composable
fun IssueDetailContent(
    provider: GitHubProvider, owner: String, repo: String, issue: Issue,
    onBack: () -> Unit, onRefresh: () -> Unit, onStateToggle: (IssueState) -> Unit
) {
    val theme = LocalThemeColors.current
    var comments by remember { mutableStateOf<List<IssueComment>>(emptyList()) }
    var commentText by remember { mutableStateOf("") }

    LaunchedEffect(issue.number) {
        val loadedComments = withContext(Dispatchers.IO) {
            try {
                provider.getIssueComments(owner, repo, issue.number)
            } catch (_: Exception) {
                emptyList()
            }
        }
        comments = loadedComments
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.Bg.primary)) {
        IssueDetailHeader(issue, onBack, onRefresh, provider, owner, repo)
        VcsDetailHtmlRenderer(
            description = issue.body,
            comments = comments,
            provider = provider,
            context = "$owner/$repo",
            modifier = Modifier.weight(1f)
        )
        val isOpen = issue.state == IssueState.OPEN
        CommentInputBar(commentText, { commentText = it }, {
            if (commentText.isNotBlank()) {
                val t = commentText; commentText = ""
                bg({ provider.addIssueComment(owner, repo, issue.number, t) }) {
                    thread {
                        try {
                            val updatedComments = runBlocking { provider.getIssueComments(owner, repo, issue.number) }
                            SwingUtilities.invokeLater { comments = updatedComments }
                        } catch (_: Exception) { }
                    }
                }
            }
        }, extraActions = {
            CompactButton(
                text = if (isOpen) "Close" else "Reopen",
                onClick = {
                    val newState = if (isOpen) IssueState.CLOSED else IssueState.OPEN
                    bg({ provider.updateIssue(owner, repo, issue.number, state = newState.name.lowercase()) }) {
                        onStateToggle(newState)
                        onBack()
                    }
                },
                variant = ButtonVariant.Primary
            )
        })
    }
}

@Composable
fun IssueDetailHeader(
    issue: Issue, onBack: () -> Unit, onRefresh: () -> Unit,
    provider: GitHubProvider, owner: String, repo: String
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
                "#${issue.number} ${issue.title}",
                fontWeight = FontWeight.Bold,
                fontSize = fs.title,
                color = theme.Text.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            ClickableIcon(AllIcons.Ide.External_link_arrow, onClick = { BrowserUtil.browse(issue.url) }, description = "Open in browser")
        }

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.padding(start = 28.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StateBadgeForIssue(issue.state)
            Text(issue.author, fontSize = fs.small, color = theme.Text.secondary)
            Text(TimeFormat.relative(issue.createdAt), fontSize = fs.small, color = theme.Text.secondary)

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                issue.labels.forEach { label ->
                    LabelChip(label = label)
                }
            }
        }
    }
    Divider(color = theme.divider, thickness = 0.5.dp)
}

@Composable
fun CommentInputBar(
    text: String, onTextChange: (String) -> Unit, onSubmit: () -> Unit,
    extraActions: @Composable RowScope.() -> Unit = {}
) {
    val theme = LocalThemeColors.current
    val fs = LocalPlatformFonts.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(theme.Bg.surface).padding(8.dp, 4.dp)) {
        OutlinedTextField(
            value = text, onValueChange = onTextChange,
            modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 34.dp, max = 100.dp),
            placeholder = { Text("Leave a comment...", fontSize = fs.label, color = theme.Text.disabled) },
            textStyle = LocalTextStyle.current.copy(fontSize = fs.label, color = theme.Text.primary),
            singleLine = false, maxLines = 3,
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = theme.Border.focused,
                unfocusedBorderColor = theme.Border.default.copy(alpha = 0.4f),
                backgroundColor = theme.Bg.input
            )
        )
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            extraActions()
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = onSubmit, enabled = text.isNotBlank()) {
                Text("Post Comment", fontSize = fs.small,
                    color = if (text.isNotBlank()) theme.Text.secondary else theme.Text.disabled)
            }
        }
    }
}

internal fun bg(fn: suspend () -> Unit, onDone: () -> Unit) {
    ProgressManager.getInstance().run(object : Task.Backgroundable(null, "Processing...", true) {
        override fun run(i: ProgressIndicator) {
            i.isIndeterminate = true
            try { runBlocking { fn() }; ApplicationManager.getApplication().invokeLater { onDone() } }
            catch (e: Exception) {
                ApplicationManager.getApplication().invokeLater {
                    JOptionPane.showMessageDialog(null, "Failed: ${e.message}", "Error", JOptionPane.ERROR_MESSAGE)
                }
            }
        }
    })
}


