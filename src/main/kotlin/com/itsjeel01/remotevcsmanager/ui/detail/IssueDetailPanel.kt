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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.itsjeel01.remotevcsmanager.models.Issue
import com.itsjeel01.remotevcsmanager.models.IssueComment
import com.itsjeel01.remotevcsmanager.models.IssueState
import com.itsjeel01.remotevcsmanager.providers.github.GitHubProvider
import com.itsjeel01.remotevcsmanager.ui.components.LabelChip
import com.itsjeel01.remotevcsmanager.ui.components.StateBadgeForIssue
import com.itsjeel01.remotevcsmanager.ui.theme.LocalPlatformFonts
import com.itsjeel01.remotevcsmanager.ui.theme.LocalThemeColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import javax.swing.JOptionPane
import javax.swing.SwingUtilities
import kotlin.concurrent.thread

@Composable
fun IssueDetailContent(
    provider: GitHubProvider, owner: String, repo: String, issue: Issue,
    onBack: () -> Unit, onRefresh: () -> Unit
) {
    val theme = LocalThemeColors.current
    val fs = LocalPlatformFonts.current
    var comments by remember { mutableStateOf<List<IssueComment>>(emptyList()) }
    var commentText by remember { mutableStateOf("") }

    LaunchedEffect(issue.number) {
        withContext(Dispatchers.IO) {
            try { comments = provider.getIssueComments(owner, repo, issue.number) } catch (_: Exception) { }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.Bg.primary)) {
        IssueDetailHeader(issue, onBack, onRefresh, provider, owner, repo)
        LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
            item {
                Spacer(Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(6.dp),
                    elevation = 0.dp,
                    backgroundColor = theme.Bg.card,
                    border = BorderStroke(0.5.dp, theme.Border.default.copy(alpha = 0.4f))
                ) {
                    Column(Modifier.padding(8.dp)) {
                        Text("Description", fontWeight = FontWeight.Bold, fontSize = fs.xsmall,
                            color = theme.Text.secondary)
                        Spacer(Modifier.height(4.dp))
                        MarkdownCompose.Block(issue.body ?: "_No description provided._")
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            if (comments.isNotEmpty()) {
                item {
                    Text("Comments (${comments.size})", fontWeight = FontWeight.Bold, fontSize = fs.label,
                        color = theme.Text.secondary)
                }
                items(comments) { CommentCard(it); Spacer(Modifier.height(6.dp)) }
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
        CommentInputBar(commentText, { commentText = it }, {
            if (commentText.isNotBlank()) {
                val t = commentText; commentText = ""
                bg({ provider.addIssueComment(owner, repo, issue.number, t) }) {
                    thread {
                        try { comments = runBlocking { provider.getIssueComments(owner, repo, issue.number) } }
                        catch (_: Exception) { }
                    }
                }
            }
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
            .background(theme.Bg.surface).padding(8.dp, 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack, modifier = Modifier.padding(0.dp)) {
                Text("←", fontSize = fs.title, color = theme.Text.primary)
            }
            Text("#${issue.number} ${issue.title}", fontWeight = FontWeight.Bold, fontSize = fs.title,
                color = theme.Text.primary, maxLines = 2, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f))
            TextButton(onClick = { BrowserUtil.browse(issue.url) }) {
                Text("↗", fontSize = fs.small, color = theme.Text.link)
            }
        }
        Row(modifier = Modifier.padding(start = 48.dp), verticalAlignment = Alignment.CenterVertically) {
            StateBadgeForIssue(issue.state); Spacer(Modifier.width(6.dp))
            Text(issue.author, fontSize = fs.small, color = theme.Text.secondary); Spacer(Modifier.width(4.dp))
            Text(fmt(issue.createdAt), fontSize = fs.small, color = theme.Text.secondary); Spacer(Modifier.width(6.dp))
            issue.labels.take(6).forEach { label ->
                LabelChip(label = label, modifier = Modifier.padding(end = 3.dp))
            }
            Spacer(Modifier.weight(1f))
            val isOpen = issue.state == IssueState.OPEN
            Button(
                onClick = {
                    if (isOpen) bg({ provider.closeIssue(owner, repo, issue.number) }, onRefresh)
                    else bg({ provider.updateIssue(owner, repo, issue.number, state = "open") }, onRefresh)
                },
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = if (isOpen) theme.Accent.red else theme.Accent.green,
                    contentColor = theme.Text.onAccent
                ),
                elevation = ButtonDefaults.elevation(defaultElevation = 0.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                shape = RoundedCornerShape(2.dp),
                modifier = Modifier.height(28.dp)
            ) { Text(if (isOpen) "Close" else "Reopen", fontSize = fs.small) }
        }
    }
    Divider(color = theme.divider, thickness = 0.5.dp)
}

@Composable
fun CommentCard(comment: IssueComment) {
    val theme = LocalThemeColors.current
    val fs = LocalPlatformFonts.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(6.dp),
        elevation = 0.dp,
        backgroundColor = theme.Bg.card,
        border = BorderStroke(0.5.dp, theme.Border.default.copy(alpha = 0.4f))
    ) {
        Column(Modifier.padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(comment.author, fontWeight = FontWeight.Bold, fontSize = fs.label, color = theme.Text.primary)
                Spacer(Modifier.width(6.dp))
                Text(fmt(comment.createdAt), fontSize = fs.xsmall, color = theme.Text.secondary)
            }
            Spacer(Modifier.height(4.dp))
            MarkdownCompose.Block(comment.body)
        }
    }
}

@Composable
fun CommentInputBar(
    text: String, onTextChange: (String) -> Unit, onSubmit: () -> Unit) {
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
        Row(Modifier
            .fillMaxWidth()
            .padding(top = 4.dp), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onSubmit, enabled = text.isNotBlank()) {
                Text("Post Comment", fontSize = fs.small,
                    color = if (text.isNotBlank()) theme.Accent.blue else theme.Text.disabled)
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

internal fun fmt(iso: String): String = try {
    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
    val d = sdf.parse(iso.take(19)) ?: return iso
    val diff = Date().time - d.time; val m = diff / 60000; val h = m / 60; val dd = h / 24; val w = dd / 7
    when { m < 1 -> "now"; m < 60 -> "${m}m ago"; h < 24 -> "${h}h ago"; dd < 7 -> "${dd}d ago"; w < 5 -> "${w}w ago"
        else -> SimpleDateFormat("MMM d", Locale.US).format(d) }
} catch (_: Exception) { iso }
