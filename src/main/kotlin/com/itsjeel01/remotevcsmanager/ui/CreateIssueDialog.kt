package com.itsjeel01.remotevcsmanager.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposePanel
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.application.ApplicationManager
import com.itsjeel01.remotevcsmanager.models.Label as VsLabel
import com.itsjeel01.remotevcsmanager.providers.github.GitHubProvider
import com.itsjeel01.remotevcsmanager.ui.components.LabelChip
import com.itsjeel01.remotevcsmanager.ui.theme.LocalPlatformFonts
import com.itsjeel01.remotevcsmanager.ui.theme.LocalThemeColors
import com.itsjeel01.remotevcsmanager.ui.theme.rememberPlatformFonts
import com.itsjeel01.remotevcsmanager.ui.theme.rememberThemeColors
import kotlin.concurrent.thread
import kotlinx.coroutines.runBlocking
import javax.swing.JComponent

class CreateIssueDialog(
    project: com.intellij.openapi.project.Project?,
    private val owner: String,
    private val repo: String,
    private val provider: GitHubProvider
) : DialogWrapper(project) {

    private var titleText by mutableStateOf("")
    private var descriptionText by mutableStateOf("")
    private var assigneeText by mutableStateOf("")
    private var repoLabels by mutableStateOf<List<VsLabel>>(emptyList())
    private var selectedLabelNames by mutableStateOf(mutableSetOf<String>())

    var resultTitle: String = ""
        private set
    var resultDescription: String? = null
        private set
    var resultLabels: List<String>? = null
        private set
    var resultAssignees: List<String>? = null
        private set

    init {
        title = "New Issue — $owner/$repo"
        init()
        loadLabels()
    }

    private fun loadLabels() {
        val cached: List<VsLabel>? = VcsCache.getApi("labels_${owner}_$repo")
        if (cached != null) {
            repoLabels = cached
            return
        }
        thread {
            try {
                val labels = runBlocking { provider.getLabels(owner, repo) }
                VcsCache.putApi("labels_${owner}_$repo", labels)
                repoLabels = labels
                javax.swing.SwingUtilities.invokeAndWait { pack() }
            } catch (_: Exception) { }
        }
    }

    override fun createCenterPanel(): JComponent {
        val panel = ComposePanel()
        panel.setContent {
            val theme = rememberThemeColors()
            val fs = rememberPlatformFonts()

            CompositionLocalProvider(LocalThemeColors provides theme, LocalPlatformFonts provides fs) {
                val textStyle = TextStyle(fontSize = fs.label, color = theme.Text.primary)
                val fieldColors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = theme.Border.focused,
                    unfocusedBorderColor = theme.Border.default.copy(alpha = 0.4f),
                    backgroundColor = theme.Bg.input,
                    cursorColor = theme.Text.primary,
                    focusedLabelColor = theme.Text.secondary,
                    unfocusedLabelColor = theme.Text.disabled
                )

                Column(
                    Modifier.padding(8.dp).width(480.dp).heightIn(min = 340.dp, max = 460.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Title", fontSize = fs.small, color = theme.Text.secondary)
                    OutlinedTextField(
                        value = titleText, onValueChange = { titleText = it },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 36.dp),
                        placeholder = { Text("Issue title", style = textStyle.copy(color = theme.Text.disabled)) },
                        singleLine = true,
                        textStyle = textStyle,
                        colors = fieldColors
                    )

                    Text("Description", fontSize = fs.small, color = theme.Text.secondary)
                    OutlinedTextField(
                        value = descriptionText, onValueChange = { descriptionText = it },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp, max = 200.dp),
                        placeholder = { Text("Leave a description...", style = textStyle.copy(fontFamily = FontFamily.Monospace, color = theme.Text.disabled)) },
                        textStyle = textStyle.copy(fontFamily = FontFamily.Monospace),
                        maxLines = 8,
                        colors = fieldColors
                    )

                    Text("Assignees", fontSize = fs.small, color = theme.Text.secondary)
                    OutlinedTextField(
                        value = assigneeText, onValueChange = { assigneeText = it },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 36.dp),
                        placeholder = { Text("e.g. username (comma-separated)", style = textStyle.copy(color = theme.Text.disabled)) },
                        singleLine = true,
                        textStyle = textStyle,
                        colors = fieldColors
                    )

                    Text("Labels", fontSize = fs.small, color = theme.Text.secondary)
                    if (repoLabels.isEmpty()) {
                        Text("Loading labels…", fontSize = fs.xsmall, color = theme.Text.disabled)
                    } else {
                        @OptIn(ExperimentalLayoutApi::class)
                        FlowRow(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            repoLabels.forEach { label ->
                                LabelChip(
                                    label = label,
                                    selected = label.name in selectedLabelNames,
                                    onToggle = {
                                        selectedLabelNames = HashSet(selectedLabelNames).also {
                                            if (label.name in selectedLabelNames) it.remove(label.name) else it.add(label.name)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
        return panel
    }

    override fun doOKAction() {
        val t = titleText.trim()
        if (t.isBlank()) {
            com.intellij.openapi.ui.Messages.showErrorDialog("Title is required to create an issue.", "Validation Error")
            return
        }
        resultTitle = t
        resultDescription = descriptionText.ifBlank { null }
        resultLabels = if (selectedLabelNames.isEmpty()) null else selectedLabelNames.toList()
        resultAssignees = assigneeText.trim()
            .takeIf { it.isNotBlank() }
            ?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }
            ?.ifEmpty { null }
        close(OK_EXIT_CODE)
    }
}
