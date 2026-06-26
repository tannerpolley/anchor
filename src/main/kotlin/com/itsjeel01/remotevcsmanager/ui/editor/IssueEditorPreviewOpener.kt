package com.itsjeel01.remotevcsmanager.ui.editor

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.itsjeel01.remotevcsmanager.models.Issue
import com.itsjeel01.remotevcsmanager.models.IssueComment
import com.itsjeel01.remotevcsmanager.providers.github.GitHubProvider
import com.itsjeel01.remotevcsmanager.ui.RepoIssueTarget
import com.itsjeel01.remotevcsmanager.ui.detail.IssuePreviewDocument
import java.util.concurrent.atomic.AtomicLong
import javax.swing.SwingUtilities
import kotlinx.coroutines.runBlocking

internal class IssueEditorPreviewOpener(
    private val project: Project,
    private val provider: GitHubProvider
) {

    private val requests = AtomicLong()

    fun openIssue(target: RepoIssueTarget, issue: Issue): Unit {
        val requestId = requests.incrementAndGet()
        val file = AnchorIssueVirtualFile(
            provider = issue.provider,
            owner = target.owner,
            repo = target.repoName,
            issueNumber = issue.number,
            title = issue.title
        )
        AnchorIssuePreviewStore.put(file, loadingPayload(issue))
        openPreviewFile(file)

        ApplicationManager.getApplication().executeOnPooledThread {
            val result = runCatching {
                runBlocking {
                    val context = "${target.owner}/${target.repoName}"
                    val comments = provider.getIssueComments(target.owner, target.repoName, issue.number)
                    val body = renderMarkdown(issue.body.orEmpty(), context)
                    val renderedComments = comments.map { renderMarkdown(it.body, context) }
                    IssueDetail(comments, body, renderedComments)
                }
            }

            SwingUtilities.invokeLater {
                if (project.isDisposed || requests.get() != requestId) return@invokeLater
                AnchorIssuePreviewStore.put(
                    file,
                    result.fold(
                        onSuccess = { detail ->
                            issuePayload(issue, detail.comments, detail.body, detail.renderedComments)
                        },
                        onFailure = { error ->
                            errorPayload(issue, error.message ?: "GitHub API request failed")
                        }
                    )
                )
            }
        }
    }

    fun cancelPendingLoad(): Unit {
        requests.incrementAndGet()
    }

    private fun openPreviewFile(file: AnchorIssueVirtualFile): Unit {
        ApplicationManager.getApplication().invokeLater {
            if (project.isDisposed) return@invokeLater
            OpenFileDescriptor(project, file)
                .setUsePreviewTab(true)
                .navigate(true)
        }
    }

    private suspend fun renderMarkdown(markdown: String, context: String): String {
        if (markdown.isBlank()) return "<p><em>No description provided.</em></p>"
        val rendered = provider.renderMarkdown(markdown, context)
        return if (rendered == markdown) "<pre>${markdown.escapeHtml()}</pre>" else rendered
    }

    private data class IssueDetail(
        val comments: List<IssueComment>,
        val body: String,
        val renderedComments: List<String>
    )
}

internal fun loadingPayload(issue: Issue): AnchorIssuePreviewPayload =
    AnchorIssuePreviewPayload(
        title = "#${issue.number} ${issue.title}",
        html = IssuePreviewDocument.buildLoading(issue)
    )

internal fun issuePayload(
    issue: Issue,
    comments: List<IssueComment>,
    renderedBody: String,
    renderedComments: List<String>
): AnchorIssuePreviewPayload =
    AnchorIssuePreviewPayload(
        title = "#${issue.number} ${issue.title}",
        html = IssuePreviewDocument.buildIssue(
            issue = issue,
            comments = comments,
            renderedBody = renderedBody,
            renderedComments = renderedComments
        )
    )

internal fun errorPayload(issue: Issue, message: String): AnchorIssuePreviewPayload =
    AnchorIssuePreviewPayload(
        title = "#${issue.number} ${issue.title}",
        html = IssuePreviewDocument.buildError("#${issue.number} ${issue.title}", message)
    )

private fun String.escapeHtml(): String =
    replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
