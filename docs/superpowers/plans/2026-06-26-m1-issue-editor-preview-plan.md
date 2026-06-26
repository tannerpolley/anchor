# Issue Editor Preview Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Move rendered GitHub issue details from the Anchor tool window into a reusable main-editor preview tab, while grouping repository issues by milestone.

**Architecture:** The tool window becomes a repository/milestone/issue navigator. A preview opener loads issue details, builds sanitized GitHub-rendered HTML, and opens an Anchor virtual issue file through a custom JCEF-backed file editor. Milestone grouping is implemented as a pure tree-model transformation before Swing tree nodes are rendered.

**Tech Stack:** Kotlin, IntelliJ Platform OpenAPI, Swing, JCEF, Gradle IntelliJ Platform plugin, kotlin.test/JUnit5.

---

## Source

- Source spec: `docs/superpowers/specs/2026-06-26-issue-editor-preview-design.md`
- Milestone: `M1 - Issue Workflow Hardening`
- Recommended branch: `codex/issue-editor-preview`
- Recommended next route after this plan: `$superpowers-project:create-issues`

## Outcome Proof

- **Intent:** Users click issues in the Anchor tool window and read rendered GitHub Markdown in the main editor preview tab instead of inside a plugin split pane.
- **Current Behavior:** `RepoIssuesTreePanel` builds a `JSplitPane` with a tree and `SwingIssueDetailRenderer`; selecting an issue renders details inside the tool window.
- **Expected Outcome:** `RepoIssuesTreePanel` renders only navigation, issues are grouped as `Repo -> Milestone -> Issue`, and issue selection opens or updates one read-only editor preview tab containing issue metadata, labels, rendered body, and comments.
- **Target Output:** A sandbox IDE shows an Anchor issue preview editor tab after selecting an issue, and selecting another issue reuses the preview tab.
- **Owner:** Anchor UI/editor integration.
- **Interface:** User selects issue nodes in the Anchor tool window; the IntelliJ editor area displays the issue preview.
- **Cutover:** Remove the tool-window issue detail pane once the editor preview path owns issue display.
- **Replaced Path:** `SwingIssueDetailRenderer` as a tool-window detail surface and the `JSplitPane` path in `RepoIssuesTreePanel`.
- **Evidence:** Unit tests for document building, milestone grouping, virtual file identity, and preview payload storage; `.\gradlew.bat test`; `.\gradlew.bat verifyPlugin`; manual sandbox IDE preview-tab verification.
- **Acceptance Proof:** The implementation is complete only when automated tests pass, plugin verification passes, and HITL sandbox verification confirms preview reuse, rendered body/comments, milestone grouping, and removal of the plugin detail pane.
- **Stop Criteria:** Stop before implementation merge if the configured IntelliJ Platform build cannot compile a preview-tab editor open path, if JCEF editor ownership cannot be disposed safely, or if tests cannot prove grouping and document-builder behavior.
- **Avoid:** Disk temp files, Markdown-plugin dependency, duplicate issue detail surfaces, raw HTML shown as editor text, silent substitutions when GitHub rendering fails, and broad Compose tool-window refactors.
- **Risk:** The exact IntelliJ preview-tab API for build `253.1` must be verified during implementation; editor lifecycle bugs can leak JCEF resources if disposal is wrong.

## Implementation Boundaries

- **Files To Create:** `src/main/kotlin/com/itsjeel01/remotevcsmanager/ui/detail/IssuePreviewDocument.kt`; `src/main/kotlin/com/itsjeel01/remotevcsmanager/ui/IssueMilestoneGrouping.kt`; `src/main/kotlin/com/itsjeel01/remotevcsmanager/ui/editor/AnchorIssueVirtualFile.kt`; `src/main/kotlin/com/itsjeel01/remotevcsmanager/ui/editor/AnchorIssuePreviewStore.kt`; `src/main/kotlin/com/itsjeel01/remotevcsmanager/ui/editor/AnchorIssueFileEditorProvider.kt`; `src/main/kotlin/com/itsjeel01/remotevcsmanager/ui/editor/AnchorIssueFileEditor.kt`; `src/main/kotlin/com/itsjeel01/remotevcsmanager/ui/editor/IssueEditorPreviewOpener.kt`; targeted tests under matching `src/test/kotlin/...` paths.
- **Files To Modify:** `src/main/kotlin/com/itsjeel01/remotevcsmanager/ui/RemoteVcsIssuesPanel.kt`; `src/main/kotlin/com/itsjeel01/remotevcsmanager/ui/RepoIssuesTreePanel.kt`; `src/main/kotlin/com/itsjeel01/remotevcsmanager/ui/RepoIssuesTreeRenderer.kt`; `src/main/resources/META-INF/plugin.xml`.
- **Files To Avoid:** `RemoteVcsToolWindowContent.kt`, `ToolWindowState.kt`, pull request detail files, provider API methods unrelated to issues, Gradle dependency changes unless compile proof requires a platform dependency already provided by IntelliJ Platform.
- **Source Of Truth:** Approved source spec plus this plan.
- **Read Path:** GitHub issues and comments are read through existing `GitHubProvider` methods; rendered Markdown still comes from `GitHubProvider.renderMarkdown`.
- **Write Path:** No GitHub writes are part of this plan. Local writes are Kotlin source, tests, plugin XML, and plan/issue artifacts.
- **Integration Points:** Swing tree selection, IntelliJ `FileEditorProvider`, editor manager preview opening, JCEF diagnostics, HTML sanitizer, plugin XML extension registration.
- **Migration Or Cutover:** Introduce builder and editor preview path first, wire issue selection to the editor, then remove direct tool-window detail rendering.
- **Replaced Path Handling:** Delete or retire `SwingIssueDetailRenderer` only after its reusable HTML behavior is extracted and no references remain.
- **Acceptance Proof Gate:** Do not mark implementation complete until validators, tests, `verifyPlugin`, and HITL sandbox verification all pass.

## Test Complete And Metrics

- **Test complete:** `.\gradlew.bat test` passes, `.\gradlew.bat verifyPlugin` passes, and HITL sandbox verification confirms editor preview behavior.
- **Pass metrics:** zero failing tests, successful plugin verification exit code, and manual confirmation of four user-visible behaviors: reusable preview tab, rendered body/comments, `No Milestone` grouping, and no plugin detail pane.
- **Numerical tolerances:** No scientific or numerical tolerances apply; behavior is pass/fail by UI state and command results.
- **TDD policy:** TDD is required where behavior can be unit-tested. UI integration steps must still start by adding the closest testable seam.

## Acceptance Criteria

- Selecting an issue opens a read-only Anchor issue preview in the main editor area.
- Selecting a different issue reuses one temporary preview tab.
- The preview shows issue metadata, labels, rendered body, and comments.
- Issues are grouped under each repository by milestone title, with `No Milestone` for unassigned issues.
- Repository nodes open by default; milestone nodes are collapsed except the first active milestone.
- The old plugin-pane issue detail surface is removed.
- Detail load, render, and JCEF creation failures produce explicit preview error content.
- `.\gradlew.bat test` and `.\gradlew.bat verifyPlugin` pass.

## Non-Goals

- Editing GitHub issue bodies from the editor preview.
- Pull request editor previews.
- Full milestone due-date or description summaries.
- Creating GitHub tracker labels, milestones, or issues.
- Reworking the old Compose tool-window path.

## Proof Oracle

Run these commands from the repo root:

```powershell
pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\validate-plan-outcome-proof.ps1 -PlanPath docs\superpowers\plans\2026-06-26-m1-issue-editor-preview-plan.md
pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\validate-decision-ledger.ps1 -Path docs\superpowers\plans\2026-06-26-m1-issue-editor-preview-plan.md -Kind plan
pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\validate-plan-task-use-cases.ps1 -PlanPath docs\superpowers\plans\2026-06-26-m1-issue-editor-preview-plan.md
.\gradlew.bat test
.\gradlew.bat verifyPlugin
```

Manual HITL proof:

1. Run `.\gradlew.bat runIde`.
2. Open a project with at least one GitHub repo containing milestone and unassigned issues.
3. Open the Anchor tool window.
4. Confirm repository nodes contain milestone nodes.
5. Click one issue and confirm the main editor area opens an Anchor issue preview tab.
6. Click a second issue and confirm the preview tab is reused.
7. Confirm body and comments render as GitHub Markdown.

## Decision Ledger

| Decision | Source | Answer | Impact | Deferred? | Risk owner |
|---|---|---|---|---|---|
| Source spec | User-approved spec | Plan from `docs/superpowers/specs/2026-06-26-issue-editor-preview-design.md`. | Keeps implementation aligned with the approved brainstorm artifact. | No | Plan owner |
| Detail surface | Source spec | Replace plugin detail pane with editor preview. | Requires `RepoIssuesTreePanel` cutover and old renderer cleanup. | No | Implementer |
| Preview content | Source spec | Body plus comments. | Requires comment loading and shared document building. | No | Implementer |
| Tab lifecycle | Source spec | Reusable preview tab. | Requires stable virtual file identity and editor open behavior. | No | Implementer |
| Tree hierarchy | Source spec | `Repo -> Milestone -> Issue`. | Requires milestone grouping and renderer updates. | No | Implementer |
| Milestone metadata | Source spec | Title grouping only. | Avoids extra milestone API calls. | No | Implementer |
| Rendering approach | Source spec | Custom JCEF-backed file editor. | Requires plugin XML registration and editor lifecycle handling. | No | Implementer |
| Test complete | User planning grill | Automated tests plus HITL sandbox verification. | Defines final completion proof. | No | User |
| TDD policy | User planning grill | TDD required. | Tasks must start with tests where practical. | No | User |
| Execution strategy | User planning grill | Issue first, branch `codex/issue-editor-preview`. | Plan routes next to issue mirror creation. | No | User |
| Preview API | Source spec and plan | Verify exact IntelliJ preview open API during implementation. | Prevents guessing at a platform method. | Yes | Implementer |

### Task 1: Extract Issue Preview Document Builder

**Use Cases:**
- Render an issue description and comments into one themed document for JCEF.
- Render labels and metadata without tying the output to a tool-window component.
- Produce explicit error HTML when detail loading or Markdown rendering fails.
- Preserve sanitizer behavior for rendered GitHub HTML.

**Files:**
- Create: `src/main/kotlin/com/itsjeel01/remotevcsmanager/ui/detail/IssuePreviewDocument.kt`
- Create: `src/test/kotlin/com/itsjeel01/remotevcsmanager/ui/detail/IssuePreviewDocumentTest.kt`
- Modify: `src/main/kotlin/com/itsjeel01/remotevcsmanager/ui/detail/SwingIssueDetailRenderer.kt`

- [ ] **Step 1: Write failing document-builder tests**

```kotlin
@Test
fun issueDocumentIncludesBodyCommentsLabelsAndMetadata(): Unit {
    val issue = issueFixture(labels = listOf(Label("bug", "d73a4a")))
    val comment = IssueComment("1", "Rendered comment", "octo", "2026-06-26T00:00:00Z", "2026-06-26T00:00:00Z")

    val html = IssuePreviewDocument.buildIssue(
        issue = issue,
        comments = listOf(comment),
        renderedBody = "<p>Rendered body</p>",
        renderedComments = listOf("<p>Rendered comment</p>")
    )

    assertContains(html, "Rendered body")
    assertContains(html, "Rendered comment")
    assertContains(html, "bug")
    assertContains(html, "#${issue.number}")
}
```

- [ ] **Step 2: Run the targeted test and verify the expected failure**

Run: `.\gradlew.bat test --tests "com.itsjeel01.remotevcsmanager.ui.detail.IssuePreviewDocumentTest"`

Expected: fails because `IssuePreviewDocument` does not exist.

- [ ] **Step 3: Implement the document builder**

Create `IssuePreviewDocument` with `buildIssue(...)` and `buildError(...)`. Move HTML page assembly, label rendering, comment section rendering, and string escaping from `SwingIssueDetailRenderer` into this object. Keep sanitizer calls at the HTML insertion boundary.

- [ ] **Step 4: Make the old renderer call the builder**

Update `SwingIssueDetailRenderer.showIssue`, `showError`, `showLoading`, and `showPlaceholder` to call `IssuePreviewDocument` helpers so behavior stays stable before the editor cutover.

- [ ] **Step 5: Run the targeted test and commit**

Run: `.\gradlew.bat test --tests "com.itsjeel01.remotevcsmanager.ui.detail.IssuePreviewDocumentTest"`

Expected: pass.

Commit: `git commit -m "test: cover issue preview document building"`

### Task 2: Add Milestone Grouping To Issue Navigation

**Use Cases:**
- Repositories with milestone issues show milestone dropdown nodes.
- Issues without milestones show under `No Milestone`.
- Empty repositories still show the existing empty message.
- Issue sorting inside milestone groups follows the selected sort option.

**Files:**
- Create: `src/main/kotlin/com/itsjeel01/remotevcsmanager/ui/IssueMilestoneGrouping.kt`
- Create: `src/test/kotlin/com/itsjeel01/remotevcsmanager/ui/IssueMilestoneGroupingTest.kt`
- Modify: `src/main/kotlin/com/itsjeel01/remotevcsmanager/ui/RepoIssuesTreePanel.kt`
- Modify: `src/main/kotlin/com/itsjeel01/remotevcsmanager/ui/RepoIssuesTreeRenderer.kt`

- [ ] **Step 1: Write failing grouping tests**

```kotlin
@Test
fun groupsIssuesByMilestoneWithNoMilestoneLast(): Unit {
    val grouped = IssueMilestoneGrouping.group(
        listOf(
            issueFixture(number = 1, milestone = "Beta"),
            issueFixture(number = 2, milestone = null),
            issueFixture(number = 3, milestone = "Alpha")
        )
    )

    assertEquals(listOf("Alpha", "Beta", "No Milestone"), grouped.map { it.title })
    assertEquals(listOf(3), grouped[0].issues.map { it.number })
    assertEquals(listOf(2), grouped[2].issues.map { it.number })
}
```

- [ ] **Step 2: Run the targeted test and verify the expected failure**

Run: `.\gradlew.bat test --tests "com.itsjeel01.remotevcsmanager.ui.IssueMilestoneGroupingTest"`

Expected: fails because `IssueMilestoneGrouping` does not exist.

- [ ] **Step 3: Implement grouping and tree item type**

Create `IssueMilestoneGrouping.MilestoneGroup(title: String, issues: List<Issue>)` and `group(issues: List<Issue>)`. Add `RepoIssueTreeItem.Milestone(target, title, openIssueCount)`.

- [ ] **Step 4: Wire milestone nodes into `showIssueNodes`**

For each loaded repo, build a repo node, then milestone child nodes, then issue nodes under each milestone. Keep existing failure, hidden fork, hidden non-owned, and empty repo behavior.

- [ ] **Step 5: Update renderer and expansion behavior**

Render milestone nodes in bold or regular text with issue count. Keep repo nodes expanded by default and expand the first milestone row for each repo after reload.

- [ ] **Step 6: Run targeted and existing tests, then commit**

Run:

```powershell
.\gradlew.bat test --tests "com.itsjeel01.remotevcsmanager.ui.IssueMilestoneGroupingTest"
.\gradlew.bat test --tests "com.itsjeel01.remotevcsmanager.GitRootDiscoveryTest"
```

Expected: pass.

Commit: `git commit -m "feat: group issues by milestone"`

### Task 3: Add Anchor Issue Virtual File And Editor

**Use Cases:**
- The same owner/repo/issue maps to one stable virtual file identity.
- Different issues do not collide.
- A custom file editor accepts only Anchor issue virtual files.
- JCEF resources are disposed with the editor lifecycle.

**Files:**
- Create: `src/main/kotlin/com/itsjeel01/remotevcsmanager/ui/editor/AnchorIssueVirtualFile.kt`
- Create: `src/main/kotlin/com/itsjeel01/remotevcsmanager/ui/editor/AnchorIssuePreviewStore.kt`
- Create: `src/main/kotlin/com/itsjeel01/remotevcsmanager/ui/editor/AnchorIssueFileEditorProvider.kt`
- Create: `src/main/kotlin/com/itsjeel01/remotevcsmanager/ui/editor/AnchorIssueFileEditor.kt`
- Create: `src/test/kotlin/com/itsjeel01/remotevcsmanager/ui/editor/AnchorIssueVirtualFileTest.kt`
- Modify: `src/main/resources/META-INF/plugin.xml`

- [ ] **Step 1: Write failing virtual-file identity tests**

```kotlin
@Test
fun issueFilePathIsStableForSameIssue(): Unit {
    val first = AnchorIssueVirtualFile("github", "octo", "repo", 42, "Issue title")
    val second = AnchorIssueVirtualFile("github", "octo", "repo", 42, "Issue title")

    assertEquals(first.path, second.path)
    assertEquals("octo/repo#42.md", first.presentableName)
}
```

- [ ] **Step 2: Run the targeted test and verify the expected failure**

Run: `.\gradlew.bat test --tests "com.itsjeel01.remotevcsmanager.ui.editor.AnchorIssueVirtualFileTest"`

Expected: fails because editor package classes do not exist.

- [ ] **Step 3: Implement virtual file and preview store**

Implement `AnchorIssueVirtualFile` as an in-memory virtual file with stable path/name values. Implement `AnchorIssuePreviewStore` keyed by virtual file path and storing the latest prepared HTML plus title metadata.

- [ ] **Step 4: Implement file editor provider and editor**

Implement `AnchorIssueFileEditorProvider.accept(project, file)` for `AnchorIssueVirtualFile`. Implement `AnchorIssueFileEditor` with JCEF browser creation through `JcefDiagnostics.createBrowser()`, explicit diagnostic panel on browser creation failure, and `Disposer` cleanup.

- [ ] **Step 5: Register provider in plugin XML**

Add the file editor provider extension under `<extensions defaultExtensionNs="com.intellij">` in `src/main/resources/META-INF/plugin.xml`.

- [ ] **Step 6: Run targeted tests and compile/plugin verification, then commit**

Run:

```powershell
.\gradlew.bat test --tests "com.itsjeel01.remotevcsmanager.ui.editor.AnchorIssueVirtualFileTest"
.\gradlew.bat verifyPlugin
```

Expected: pass.

Commit: `git commit -m "feat: add issue preview editor"`

### Task 4: Wire Issue Selection To Editor Preview

**Use Cases:**
- Selecting an issue opens the main editor preview tab.
- Selecting another issue updates the same preview target.
- Stale async detail loads cannot overwrite the latest selected issue.
- Open in Browser remains available for GitHub web UI handoff.

**Files:**
- Create: `src/main/kotlin/com/itsjeel01/remotevcsmanager/ui/editor/IssueEditorPreviewOpener.kt`
- Modify: `src/main/kotlin/com/itsjeel01/remotevcsmanager/ui/RemoteVcsIssuesPanel.kt`
- Modify: `src/main/kotlin/com/itsjeel01/remotevcsmanager/ui/RepoIssuesTreePanel.kt`
- Delete after no references remain: `src/main/kotlin/com/itsjeel01/remotevcsmanager/ui/detail/SwingIssueDetailRenderer.kt`

- [ ] **Step 1: Add the closest testable seam**

Add tests for request-id behavior if `IssueEditorPreviewOpener` exposes a pure `isCurrent(requestId)` or request-token helper. Keep editor-manager calls behind a method that can be exercised through a lightweight fake in tests.

- [ ] **Step 2: Implement `IssueEditorPreviewOpener`**

The opener should fetch comments, render body/comments through `GitHubProvider.renderMarkdown`, build `IssuePreviewDocument`, store the payload in `AnchorIssuePreviewStore`, and open the virtual file through the IntelliJ editor manager.

- [ ] **Step 3: Verify preview-tab API by compile**

Use the IntelliJ Platform API available to this build to request preview-tab behavior. If the first compile fails because the preview option name differs, adjust to the platform-supported API and record the chosen method in the commit message body.

- [ ] **Step 4: Cut over `RepoIssuesTreePanel`**

Remove `JSplitPane`, `detailRenderer`, `loadIssueDetail`, and direct detail rendering from `RepoIssuesTreePanel`. On issue selection, call `IssueEditorPreviewOpener.open(target, issue)`.

- [ ] **Step 5: Cut over `RemoteVcsIssuesPanel`**

Stop creating and registering `SwingIssueDetailRenderer`. Construct `RepoIssuesTreePanel` with the preview opener or the dependencies it needs.

- [ ] **Step 6: Delete the old renderer when unused**

Run `rg -n "SwingIssueDetailRenderer|detailRenderer" src/main/kotlin src/test/kotlin`. Delete `SwingIssueDetailRenderer.kt` only when no production path references it.

- [ ] **Step 7: Run tests and commit**

Run:

```powershell
.\gradlew.bat test
.\gradlew.bat verifyPlugin
```

Expected: pass.

Commit: `git commit -m "feat: open issues in editor preview"`

### Task 5: Validate Integration And Manual IDE Behavior

**Use Cases:**
- Plugin metadata accepts the new editor provider extension.
- The editor preview tab appears in a sandbox IDE.
- Selecting issues across repos and milestones reuses the preview tab.
- Error documents are visible when detail loading fails.

**Files:**
- Modify: `src/main/resources/META-INF/plugin.xml`
- Modify only if needed by verification: files touched in Tasks 1-4

- [ ] **Step 1: Run full automated proof**

Run:

```powershell
.\gradlew.bat test
.\gradlew.bat verifyPlugin
```

Expected: both commands exit 0.

- [ ] **Step 2: Run sandbox IDE**

Run: `.\gradlew.bat runIde`

Expected: sandbox IDE starts with Anchor loaded.

- [ ] **Step 3: Perform HITL preview proof**

In the sandbox IDE, open a project with GitHub issues that include at least one milestone issue and one unassigned issue. Click three issues across at least two milestone groups. Confirm the editor preview tab is reused and rendered content includes body and comments.

- [ ] **Step 4: Perform HITL cutover proof**

Confirm the Anchor tool window shows only navigation and actions, not a split-pane issue detail surface.

- [ ] **Step 5: Commit validation fixes if needed**

If validation required code changes, run tests again and commit with `git commit -m "fix: stabilize issue editor preview"`.

### Task 6: Final Cleanup And Handoff

**Use Cases:**
- Dead issue-detail code is removed.
- Plan proof commands are reproducible.
- Git status is clean before pushing or creating issues.
- The next workflow can create issue mirrors from this plan.

**Files:**
- Modify only if cleanup is required: files touched in Tasks 1-5
- Verify: `docs/superpowers/plans/2026-06-26-m1-issue-editor-preview-plan.md`

- [ ] **Step 1: Search for obsolete path references**

Run:

```powershell
rg -n "SwingIssueDetailRenderer|detailRenderer|JSplitPane" src/main/kotlin src/test/kotlin
```

Expected: no references to removed issue-detail ownership remain, except unrelated Swing layout references if still valid.

- [ ] **Step 2: Run plan validators**

Run:

```powershell
pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\validate-plan-outcome-proof.ps1 -PlanPath docs\superpowers\plans\2026-06-26-m1-issue-editor-preview-plan.md
pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\validate-decision-ledger.ps1 -Path docs\superpowers\plans\2026-06-26-m1-issue-editor-preview-plan.md -Kind plan
pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\validate-plan-task-use-cases.ps1 -PlanPath docs\superpowers\plans\2026-06-26-m1-issue-editor-preview-plan.md
```

Expected: all validators pass.

- [ ] **Step 3: Run final automated proof**

Run:

```powershell
.\gradlew.bat test
.\gradlew.bat verifyPlugin
```

Expected: both commands exit 0.

- [ ] **Step 4: Run repo cleanup hook**

Run:

```powershell
pwsh.exe -NoProfile -ExecutionPolicy Bypass -File "$env:USERPROFILE\.codex\hooks\codex-cleanup.ps1" -RepoRoot .
```

Expected: no task-owned leftover processes.

- [ ] **Step 5: Commit final cleanup**

Run:

```powershell
git status --short
git add src/main/kotlin src/test/kotlin src/main/resources/META-INF/plugin.xml
git commit -m "chore: finish issue editor preview cleanup"
```

Expected: clean worktree after commit.
