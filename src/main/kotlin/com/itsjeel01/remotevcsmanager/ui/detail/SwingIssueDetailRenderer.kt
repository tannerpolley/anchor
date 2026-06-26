package com.itsjeel01.remotevcsmanager.ui.detail

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.util.ui.JBUI
import com.itsjeel01.remotevcsmanager.models.Issue
import com.itsjeel01.remotevcsmanager.models.IssueComment
import com.itsjeel01.remotevcsmanager.ui.components.JcefDiagnostics
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JTextArea

class SwingIssueDetailRenderer : Disposable {

    private val browser: JBCefBrowser?
    private val root = JBPanel<JBPanel<*>>(BorderLayout())

    val component: JComponent = root

    init {
        val result = JcefDiagnostics.createBrowser()
        browser = result.browser
        if (browser != null) {
            root.add(browser.component, BorderLayout.CENTER)
        } else {
            root.add(createMessagePanel(result.diagnostics.joinToString("\n")), BorderLayout.CENTER)
        }
        showPlaceholder("Select an issue to read its body and comments.")
    }

    fun showPlaceholder(message: String): Unit {
        loadPage(IssuePreviewDocument.buildPlaceholder(message))
    }

    fun showLoading(issue: Issue): Unit {
        loadPage(IssuePreviewDocument.buildLoading(issue))
    }

    fun showIssue(
        issue: Issue,
        comments: List<IssueComment>,
        renderedBody: String,
        renderedComments: List<String>
    ): Unit {
        loadPage(
            IssuePreviewDocument.buildIssue(
                issue = issue,
                comments = comments,
                renderedBody = renderedBody,
                renderedComments = renderedComments
            )
        )
    }

    fun showError(issue: Issue?, message: String): Unit {
        val title = issue?.let { "#${it.number} ${it.title}" } ?: "GitHub Issue"
        loadPage(IssuePreviewDocument.buildError(title, message))
    }

    override fun dispose(): Unit {
        browser?.let { Disposer.dispose(it) }
    }

    private fun loadPage(html: String): Unit {
        browser?.loadHTML(html)
    }

    private fun createMessagePanel(message: String): JComponent {
        val area = JTextArea(message)
        area.isEditable = false
        area.isOpaque = false
        area.lineWrap = true
        area.wrapStyleWord = true
        area.border = JBUI.Borders.empty(12)
        return JBScrollPane(area)
    }
}
