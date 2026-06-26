package com.itsjeel01.remotevcsmanager.ui.editor

import com.intellij.openapi.Disposable
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorLocation
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.util.ui.JBUI
import com.itsjeel01.remotevcsmanager.ui.components.JcefDiagnostics
import java.awt.BorderLayout
import java.beans.PropertyChangeListener
import javax.swing.JComponent
import javax.swing.JTextArea

class AnchorIssueFileEditor(
    private val file: AnchorIssueVirtualFile
) : UserDataHolderBase(), FileEditor {

    private val root = JBPanel<JBPanel<*>>(BorderLayout())
    private var payload = checkNotNull(AnchorIssuePreviewStore.get(file)) {
        "Anchor issue preview payload missing for ${file.presentableName}"
    }
    private val browser: JBCefBrowser?
    private val storeSubscription: Disposable

    init {
        val result = JcefDiagnostics.createBrowser()
        browser = result.browser
        storeSubscription = AnchorIssuePreviewStore.subscribe(file) { nextPayload ->
            payload = nextPayload
            browser?.loadHTML(nextPayload.html)
        }
        if (browser != null) {
            root.add(browser.component, BorderLayout.CENTER)
            browser.loadHTML(payload.html)
        } else {
            root.add(createMessagePanel(result.diagnostics.joinToString("\n")), BorderLayout.CENTER)
        }
    }

    override fun getComponent(): JComponent = root

    override fun getPreferredFocusedComponent(): JComponent = browser?.component ?: root

    override fun getName(): String = payload.title

    override fun setState(state: FileEditorState) = Unit

    override fun isModified(): Boolean = false

    override fun isValid(): Boolean = file.isValid

    override fun addPropertyChangeListener(listener: PropertyChangeListener) = Unit

    override fun removePropertyChangeListener(listener: PropertyChangeListener) = Unit

    override fun getCurrentLocation(): FileEditorLocation? = null

    override fun dispose(): Unit {
        browser?.let { Disposer.dispose(it) }
        storeSubscription.dispose()
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
