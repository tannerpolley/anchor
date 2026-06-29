# Tool Window Navigation Refresh Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix the Anchor issue tool-window header overlap and extend the navigator with per-project repo inclusion, GitHub milestone dropdowns with empty milestones, and GitHub parent/sub-issue nesting.

**Architecture:** Keep `RepoIssuesTreePanel` as the Swing navigator and the Anchor editor preview as the issue detail surface. Add small provider/model seams for GitHub milestones and sub-issue relationships, then replace issue-derived milestone grouping with a pure `IssueTreeGrouping` model that drives the existing `JTree`.

**Tech Stack:** Kotlin, IntelliJ Platform Swing UI, GitHub REST/GraphQL API through OkHttp/Gson, Kotlin test/JUnit, Gradle IntelliJ plugin build.

---

## Source Intake

- **Source spec:** `docs/superpowers/specs/2026-06-29-tool-window-navigation-refresh-design.md`
- **Workflow mode ledger:** `.superpowers/runs/20260629-152829-tool-window-workflow/workflow-mode-ledger.json`
- **Auto Mode authorization ledger:** `.superpowers/runs/20260629-152829-tool-window-workflow/auto-mode-authorization.json`
- **Milestone:** `M1 - Issue Workflow Hardening`
- **GitHub tracker note:** `tannerpolley/superpowers-project#105` tracks the native-input visual companion gate conflict that was found during this route.

## Outcome Proof

- **Intent:** Make the Anchor issue navigator visually stable and structurally complete for selected Workspace repositories.
- **Current Behavior:** The header can overlap at narrow widths; every detected GitHub repo is loaded; milestones only appear when open issues reference them; parent/sub-issue relationships are not represented in the tree.
- **Expected Outcome:** The header reflows without overlap, users can persistently include/exclude detected repos per project, every GitHub milestone appears as a dropdown including `0 open`, and issues render as `Repo > Milestone > Parent Issue > Subissue`.
- **Target Output:** A Workspace screenshot showing the fixed header and a tree that contains an empty milestone plus an expanded parent issue with sub-issues.
- **Owner:** Anchor plugin issue workflow, primarily `com.itsjeel01.remotevcsmanager.ui` and `com.itsjeel01.remotevcsmanager.providers.github`.
- **Interface:** Anchor tool window issue navigator and existing Anchor editor issue preview.
- **Cutover:** Replace direct `IssueMilestoneGrouping.group(issues)` wiring with the new tree grouping model after provider data and repo inclusion state are available.
- **Replaced Path:** Replace the one-row overlapping header layout and issue-derived-only milestone grouping path.
- **Evidence:** Unit tests for repo inclusion, milestone grouping, sub-issue grouping, and provider parsing; `.\gradlew.bat test`; `.\gradlew.bat buildPlugin`; manual Workspace screenshot proof.
- **Acceptance Proof:** The plan is complete only when tests pass, plugin packaging succeeds, cleanup passes, and a current Workspace smoke check shows no header overlap, an excluded repo staying hidden, an empty milestone dropdown, and parent/sub-issue nesting opening rendered editor previews.
- **Stop Criteria:** Stop before implementation if the exact GitHub parent/sub-issue API cannot be verified, if persistence ownership is unclear after inspecting IntelliJ services, or if a required validator fails.
- **Avoid:** Do not parse issue bodies for relationships, do not create a tool-window detail pane, do not make repo inclusion global, do not hide API failures behind inferred data, and do not change pull request navigation.
- **Risk:** GitHub sub-issue API shape may require GraphQL or a newer REST media type; implementation must verify this before changing provider code.

## Implementation Boundaries

- **Files To Create:** `src/main/kotlin/com/itsjeel01/remotevcsmanager/models/IssueMilestone.kt`; `src/main/kotlin/com/itsjeel01/remotevcsmanager/models/IssueRelationship.kt`; `src/main/kotlin/com/itsjeel01/remotevcsmanager/ui/RepoIssueInclusionState.kt`; `src/main/kotlin/com/itsjeel01/remotevcsmanager/ui/RepositoryInclusionDialog.kt`; `src/main/kotlin/com/itsjeel01/remotevcsmanager/ui/IssueTreeGrouping.kt`; matching tests under `src/test/kotlin/com/itsjeel01/remotevcsmanager/ui`.
- **Files To Modify:** `src/main/kotlin/com/itsjeel01/remotevcsmanager/providers/RemoteVcsProvider.kt`; `src/main/kotlin/com/itsjeel01/remotevcsmanager/providers/github/GitHubApiClient.kt`; `src/main/kotlin/com/itsjeel01/remotevcsmanager/providers/github/GitHubProvider.kt`; `src/main/kotlin/com/itsjeel01/remotevcsmanager/ui/RemoteVcsIssuesPanel.kt`; `src/main/kotlin/com/itsjeel01/remotevcsmanager/ui/RepoIssuesTreePanel.kt`; `src/main/kotlin/com/itsjeel01/remotevcsmanager/ui/RepoIssuesTreeRenderer.kt`; existing grouping tests.
- **Files To Avoid:** Editor preview files under `src/main/kotlin/com/itsjeel01/remotevcsmanager/ui/editor` unless a compile break requires an import-only adjustment; pull request UI files; Gradle build files; plugin.xml.
- **Source Of Truth:** GitHub milestone records and GitHub parent/sub-issue relationship data, filtered by the per-project repo inclusion model.
- **Read Path:** Workspace Git roots -> detected GitHub targets -> per-project inclusion -> GitHub access/milestones/issues/sub-issues -> pure grouping model -> `JTree`.
- **Write Path:** User checkbox selections update the per-project inclusion state; issue tree refresh writes only Swing model state.
- **Integration Points:** JetBrains project service or project properties for persistence; GitHub provider/API layer; `RepoIssuesTreePanel.reloadIssues`; `RepoIssuesTreeRenderer`; existing `IssueEditorPreviewOpener`.
- **Migration Or Cutover:** Keep the current tree loading path until `IssueTreeGrouping` tests pass, then wire `RepoIssuesTreePanel` to the new model and remove `IssueMilestoneGrouping` if no callers remain.
- **Replaced Path Handling:** Delete or retire the old issue-derived grouping path once the new model covers milestones, empty milestones, parent rows, sub-issues, standalone issues, and messages.
- **Acceptance Proof Gate:** Do not mark the implementation ready until automated tests, `buildPlugin`, cleanup, and Workspace screenshot proof all pass.

## Test Complete And Metrics

- **Test complete:** All targeted unit tests and full `.\gradlew.bat test` pass, followed by successful `.\gradlew.bat buildPlugin`.
- **Proof demonstration:** Manual Workspace smoke check must show the corrected header at the narrow width from the screenshot and the requested tree hierarchy.
- **Pass metrics:** zero failing tests; buildPlugin exit code 0; no fresh Anchor exceptions in the IDE log after smoke check; screenshot has no title/status/sort/button overlap.
- **Edge thresholds:** Header controls may wrap to two rows, but text must not draw under another control at widths down to the screenshot width.
- **Non-applicable metrics:** Numerical tolerances are not applicable because this is UI/navigation behavior, not scientific computation.

## Acceptance Criteria

- [ ] The header does not overlap when the tool window is narrowed to the screenshot width.
- [ ] Users can open a repo inclusion UI, deselect a detected repo, apply, refresh, and see that repo excluded.
- [ ] Repo inclusion persists for the IntelliJ project across tool-window recreation.
- [ ] Every GitHub milestone appears under each included repo, including milestones with zero open issues.
- [ ] `No Milestone` appears only when loaded open issues have no GitHub milestone.
- [ ] Parent issues render as expandable rows under their milestone.
- [ ] Sub-issues render under parent issue rows and do not duplicate as standalone issue rows.
- [ ] Selecting parent issues, sub-issues, and standalone issues opens the rendered Anchor issue preview.
- [ ] API failures produce visible per-repo messages rather than inferred data.

## Non-Goals

- Pull request grouping changes.
- Editing GitHub issue or milestone metadata.
- Local-only parent/sub-issue authoring.
- Body parsing for issue relationships.
- Global repo inclusion settings.
- Tool-window detail preview restoration.

## Decision Ledger

| Decision | Source | Answer | Impact | Deferred? | Risk owner |
|---|---|---|---|---|---|
| Source spec | User-approved brainstorm | `docs/superpowers/specs/2026-06-29-tool-window-navigation-refresh-design.md`. | Plan is grounded in the approved spec. | No | Codex |
| Auto Mode authority | User native answer | Bounded Auto Merge. | Allows recorded defaults for planning and later route decisions inside policy. | No | Codex |
| Milestone source | Source spec | GitHub milestone records. | Requires provider/API methods and enables empty milestone dropdowns. | No | Implementer |
| Repo inclusion persistence | Source spec | Per IntelliJ project. | Requires project-scoped persistence, not global settings. | No | Implementer |
| Sub-issue source | Source spec | GitHub sub-issues API. | Requires API verification and provider support. | No | Implementer |
| Tree hierarchy | Source spec | `Repo > Milestone > Parent > Subissue`. | Drives model and renderer task order. | No | Implementer |
| Parent row selection | Source spec | Parent group rows open the parent issue on selection. | Avoids duplicate parent issue rows. | No | Implementer |
| Header strategy | Source spec | Responsive reflow, exact Swing layout chosen during implementation. | Gives UI outcome while allowing narrow-width proof to guide implementation. | Yes | Implementer |
| GitHub sub-issue endpoint | Source spec | Verify exact route/version before coding. | Prevents provider code from relying on an unverified API shape. | Yes | Implementer |
| TDD policy | Project plan policy | Use TDD for feature work. | Each task starts with focused failing tests where practical. | No | Implementer |
| Debug discipline | Project plan policy | Use systematic diagnosis for API or UI runtime failures. | Runtime issues must be reproduced before fixes. | No | Implementer |
| Execution route | Auto Mode recorded default | Create issues or implement from this plan only after plan validators pass. | Keeps implementation behind durable plan proof. | No | Codex |

## Task Breakdown

### Task 1: Add Repo Inclusion State

**Use Cases:**
- A Workspace contains many GitHub roots and the user wants Anchor to track only a subset.
- A newly detected repo should appear in Anchor until the user explicitly excludes it.
- A repo excluded yesterday should remain excluded when the Workspace reloads.
- A stale stored key should not break tree loading when the repo root is no longer attached.

**Files:**
- Create: `src/main/kotlin/com/itsjeel01/remotevcsmanager/ui/RepoIssueInclusionState.kt`
- Test: `src/test/kotlin/com/itsjeel01/remotevcsmanager/ui/RepoIssueInclusionStateTest.kt`
- Modify: `src/main/kotlin/com/itsjeel01/remotevcsmanager/ui/RemoteVcsIssuesPanel.kt`

- [ ] **Step 1: Write failing tests for inclusion defaults**

Create `RepoIssueInclusionStateTest.kt` with tests named:

```kotlin
@Test
fun newlyDetectedTargetsDefaultIncluded(): Unit

@Test
fun excludedTargetIsRemovedFromIncludedTargets(): Unit

@Test
fun staleExcludedTargetDoesNotAffectCurrentTargets(): Unit
```

Use `RepoIssueTarget(displayName, owner, repoName, rootPath, issuesUrl)` fixtures and assert on returned issue URLs.

- [ ] **Step 2: Run the failing tests**

Run:

```powershell
.\gradlew.bat --no-daemon "-Dkotlin.compiler.execution.strategy=in-process" --console=plain test --tests "com.itsjeel01.remotevcsmanager.ui.RepoIssueInclusionStateTest"
```

Expected: fails because `RepoIssueInclusionState` does not exist.

- [ ] **Step 3: Implement the pure inclusion model**

Create `RepoIssueInclusionState` as a small pure class first:

```kotlin
internal class RepoIssueInclusionState(
    private val excludedKeys: Set<String>
) {
    fun includedTargets(targets: List<RepoIssueTarget>): List<RepoIssueTarget> =
        targets.filterNot { keyFor(it) in excludedKeys }

    fun keyFor(target: RepoIssueTarget): String =
        listOf(target.owner, target.repoName, target.rootPath).joinToString("|")
}
```

- [ ] **Step 4: Add project persistence**

Extend `RepoIssueInclusionState` or add a companion project service after checking IntelliJ persistence patterns in this repo. The persisted value is the excluded key set. Keep the pure `includedTargets` method testable without IntelliJ services.

- [ ] **Step 5: Wire `RemoteVcsIssuesPanel` to filter targets**

Apply inclusion after `resolveTargets(project)` and before constructing `RepoIssuesTreePanel`. Pass both all detected targets and included targets if the dialog needs the full list.

- [ ] **Step 6: Run tests and commit**

Run the targeted test and full tests. Commit:

```powershell
git add src/main/kotlin/com/itsjeel01/remotevcsmanager/ui/RepoIssueInclusionState.kt src/test/kotlin/com/itsjeel01/remotevcsmanager/ui/RepoIssueInclusionStateTest.kt src/main/kotlin/com/itsjeel01/remotevcsmanager/ui/RemoteVcsIssuesPanel.kt
git commit -m "feat: persist issue repo inclusion"
```

### Task 2: Add GitHub Milestone And Sub-Issue API Models

**Use Cases:**
- A repo has a milestone with zero open issues and Anchor must still show it.
- A repo has a parent issue with sub-issues and Anchor must nest the loaded children.
- GitHub API data must be parsed into small models before UI grouping.
- If the parent/sub-issue API shape differs, implementation stops before wiring UI assumptions.

**Files:**
- Create: `src/main/kotlin/com/itsjeel01/remotevcsmanager/models/IssueMilestone.kt`
- Create: `src/main/kotlin/com/itsjeel01/remotevcsmanager/models/IssueRelationship.kt`
- Test: `src/test/kotlin/com/itsjeel01/remotevcsmanager/providers/github/GitHubProviderIssueStructureTest.kt`
- Modify: `src/main/kotlin/com/itsjeel01/remotevcsmanager/providers/RemoteVcsProvider.kt`
- Modify: `src/main/kotlin/com/itsjeel01/remotevcsmanager/providers/github/GitHubApiClient.kt`
- Modify: `src/main/kotlin/com/itsjeel01/remotevcsmanager/providers/github/GitHubProvider.kt`

- [ ] **Step 1: Verify GitHub API routes**

Use official GitHub API docs and, if needed, a read-only `gh api` probe against a repo with issues. Record the verified milestone and sub-issue route in the implementation notes before editing provider code.

- [ ] **Step 2: Add model tests**

Write tests that parse representative milestone JSON and representative relationship JSON through provider parsing helpers. Expected model shapes:

```kotlin
data class IssueMilestone(
    val id: String,
    val title: String,
    val openIssueCount: Int,
    val state: String
)

data class IssueRelationship(
    val parentIssueNumber: Int,
    val childIssueNumber: Int
)
```

- [ ] **Step 3: Run tests and verify expected failure**

Run:

```powershell
.\gradlew.bat --no-daemon "-Dkotlin.compiler.execution.strategy=in-process" --console=plain test --tests "com.itsjeel01.remotevcsmanager.providers.github.GitHubProviderIssueStructureTest"
```

Expected: fails because models and provider methods do not exist.

- [ ] **Step 4: Add provider methods**

Add required provider methods:

```kotlin
suspend fun getMilestones(owner: String, repo: String): List<IssueMilestone>

suspend fun getIssueRelationships(owner: String, repo: String, issues: List<Issue>): List<IssueRelationship>
```

Implement them in `GitHubProvider` with GitHub-backed implementations only. Do not add provider-level empty-list defaults; a provider that cannot supply milestone or relationship data should fail at compile time until it implements the contract directly.

- [ ] **Step 5: Add API client calls**

Add paginated API calls for milestones and the verified sub-issue relationship route. Reuse `buildGetRequest`, `executeRequest`, and JSON safety helpers.

- [ ] **Step 6: Run tests and commit**

Run provider tests and full tests. Commit:

```powershell
git add src/main/kotlin/com/itsjeel01/remotevcsmanager/models src/main/kotlin/com/itsjeel01/remotevcsmanager/providers src/test/kotlin/com/itsjeel01/remotevcsmanager/providers/github/GitHubProviderIssueStructureTest.kt
git commit -m "feat: load GitHub milestones and issue relationships"
```

### Task 3: Replace Issue-Derived Milestone Grouping With Tree Grouping

**Use Cases:**
- Every GitHub milestone appears even when it has zero open issues.
- `No Milestone` appears only when unassigned open issues exist.
- Parent issue rows contain sub-issue rows under the correct milestone.
- Sub-issues do not appear twice in the same milestone group.
- Standalone issues remain visible and sorted.

**Files:**
- Create: `src/main/kotlin/com/itsjeel01/remotevcsmanager/ui/IssueTreeGrouping.kt`
- Test: `src/test/kotlin/com/itsjeel01/remotevcsmanager/ui/IssueTreeGroupingTest.kt`
- Modify: `src/main/kotlin/com/itsjeel01/remotevcsmanager/ui/IssueMilestoneGrouping.kt`
- Modify: `src/test/kotlin/com/itsjeel01/remotevcsmanager/ui/IssueMilestoneGroupingTest.kt`

- [ ] **Step 1: Write grouping tests**

Create tests:

```kotlin
@Test
fun includesMilestonesWithZeroOpenIssues(): Unit

@Test
fun createsNoMilestoneOnlyForUnassignedIssues(): Unit

@Test
fun nestsSubIssuesUnderParentIssue(): Unit

@Test
fun doesNotDuplicateSubIssuesAsStandaloneRows(): Unit
```

- [ ] **Step 2: Run tests and verify expected failure**

Run:

```powershell
.\gradlew.bat --no-daemon "-Dkotlin.compiler.execution.strategy=in-process" --console=plain test --tests "com.itsjeel01.remotevcsmanager.ui.IssueTreeGroupingTest"
```

Expected: fails because `IssueTreeGrouping` does not exist.

- [ ] **Step 3: Implement pure grouping model**

Create sealed row/group models, for example:

```kotlin
internal object IssueTreeGrouping {
    data class MilestoneGroup(
        val title: String,
        val openIssueCount: Int,
        val rows: List<IssueRow>
    )

    sealed interface IssueRow {
        data class Parent(val issue: Issue, val children: List<Issue>) : IssueRow
        data class Standalone(val issue: Issue) : IssueRow
    }
}
```

Implement grouping using milestone records first, then assign issues by `issue.milestone`.

- [ ] **Step 4: Retire old grouping path**

After `IssueTreeGroupingTest` passes, remove `IssueMilestoneGrouping` only if `rg "IssueMilestoneGrouping" src` shows no remaining production callers. If tests still need migration, replace them with equivalent `IssueTreeGroupingTest` coverage.

- [ ] **Step 5: Run tests and commit**

Run targeted grouping tests and full tests. Commit:

```powershell
git add src/main/kotlin/com/itsjeel01/remotevcsmanager/ui/IssueTreeGrouping.kt src/test/kotlin/com/itsjeel01/remotevcsmanager/ui/IssueTreeGroupingTest.kt src/main/kotlin/com/itsjeel01/remotevcsmanager/ui/IssueMilestoneGrouping.kt src/test/kotlin/com/itsjeel01/remotevcsmanager/ui/IssueMilestoneGroupingTest.kt
git commit -m "feat: group issue tree by milestones and sub-issues"
```

### Task 4: Wire Responsive Header, Repo Inclusion UI, And Tree Rendering

**Use Cases:**
- The screenshot-width header does not overlap title, status, sort, or buttons.
- The user can open a repo inclusion UI, toggle repos, apply, and reload.
- Empty milestone rows render as dropdowns with `0 open`.
- Parent issue rows open the parent issue on selection and expand to show sub-issues.
- Sub-issue rows open their own rendered preview.

**Files:**
- Create: `src/main/kotlin/com/itsjeel01/remotevcsmanager/ui/RepositoryInclusionDialog.kt`
- Modify: `src/main/kotlin/com/itsjeel01/remotevcsmanager/ui/RepoIssuesTreePanel.kt`
- Modify: `src/main/kotlin/com/itsjeel01/remotevcsmanager/ui/RepoIssuesTreeRenderer.kt`
- Test: `src/test/kotlin/com/itsjeel01/remotevcsmanager/ui/RepoIssuesTreeRendererTest.kt` if renderer assertions are practical; otherwise cover renderer-independent behavior in model tests and reserve UI proof for manual screenshot.

- [ ] **Step 1: Add repo inclusion action**

Add a compact `Repos` or icon+text action to the header. It opens `RepositoryInclusionDialog` with all detected targets and current inclusion state.

- [ ] **Step 2: Rework header layout**

Change `createHeader` to a layout that cannot draw controls over title/status. A two-row `BorderLayout` or `GridBagLayout` is acceptable when it passes screenshot proof.

- [ ] **Step 3: Update reload data fetch**

For each included repo, load milestones, open issues, and issue relationships before building the tree nodes. Keep request-id cancellation and EDT model updates.

- [ ] **Step 4: Add tree items**

Extend `RepoIssueTreeItem` with parent/sub-issue row types or enough fields to distinguish parent rows from standalone rows.

- [ ] **Step 5: Update renderer**

Render repositories, milestones, parent issue rows, sub-issues, standalone issues, and messages. Milestones always show `N open`; empty milestones show `0 open`.

- [ ] **Step 6: Update selection behavior**

Selection rules:

```text
Repository -> set selected target, clear selected issue
Milestone -> set selected target, clear selected issue
Parent issue -> set selected target and selected issue, open preview
Sub-issue -> set selected target and selected issue, open preview
Standalone issue -> set selected target and selected issue, open preview
Message -> clear selected issue
```

- [ ] **Step 7: Run tests and commit**

Run UI-adjacent tests and full tests. Commit:

```powershell
git add src/main/kotlin/com/itsjeel01/remotevcsmanager/ui src/test/kotlin/com/itsjeel01/remotevcsmanager/ui
git commit -m "feat: refresh issue tool window navigation"
```

### Task 5: Package And Prove In The Workspace IDE

**Use Cases:**
- The plugin installs into the current Workspace IDE window.
- The header overlap from the screenshot is gone.
- Repo exclusion persists after tool-window recreation.
- Empty milestones and parent/sub-issue rows are visible.
- Existing rendered issue editor preview still opens for all issue row types.

**Files:**
- Modify: `docs/superpowers/plans/2026-06-29-m1-tool-window-navigation-refresh-plan.md` only if proof instructions need correction during execution.
- Avoid: Source code changes unless a failed proof identifies a concrete defect that routes back to the relevant task.

- [ ] **Step 1: Run full test suite**

Run:

```powershell
.\gradlew.bat --no-daemon "-Dkotlin.compiler.execution.strategy=in-process" --console=plain test
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 2: Build plugin**

Run:

```powershell
.\gradlew.bat --no-daemon "-Dkotlin.compiler.execution.strategy=in-process" --console=plain buildPlugin
```

Expected: `BUILD SUCCESSFUL` and a zip under `build/distributions`.

- [ ] **Step 3: Install into current Workspace IDE**

Use the IntelliJ MCP plugin install tool with the built zip and restart the existing Workspace IDE. Do not open a new project window.

- [ ] **Step 4: Capture UI proof**

Use screenshot/computer-use proof against the current Workspace IDE:

```text
Anchor tool window open
Header at screenshot-width or narrower
Repo inclusion control visible
At least one empty milestone row visible as 0 open
At least one parent issue expanded with sub-issue rows
Rendered editor preview open for selected issue row
```

- [ ] **Step 5: Check logs and cleanup**

Scan the fresh IDE log tail for Anchor exceptions, run the cleanup hook, and record exact proof in the closeout.

- [ ] **Step 6: Commit final proof or cleanup changes**

If implementation changed files after earlier commits, commit them with a focused message. Do not commit generated build outputs.

## Proof Commands

```powershell
.\gradlew.bat --no-daemon "-Dkotlin.compiler.execution.strategy=in-process" --console=plain test
.\gradlew.bat --no-daemon "-Dkotlin.compiler.execution.strategy=in-process" --console=plain buildPlugin
pwsh.exe -NoProfile -ExecutionPolicy Bypass -File "$env:USERPROFILE\.codex\hooks\codex-cleanup.ps1" -RepoRoot .
```

## Plan Validation

Run before using this plan for issue creation or implementation:

```powershell
pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\validate-plan-outcome-proof.ps1 -PlanPath docs\superpowers\plans\2026-06-29-m1-tool-window-navigation-refresh-plan.md
pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\validate-plan-task-use-cases.ps1 -PlanPath docs\superpowers\plans\2026-06-29-m1-tool-window-navigation-refresh-plan.md
pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\validate-decision-ledger.ps1 -Path docs\superpowers\plans\2026-06-29-m1-tool-window-navigation-refresh-plan.md -Kind plan
```
