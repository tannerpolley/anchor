# Single-Repository Issue Navigator Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove Anchor's Workspace multi-repository issue behavior while preserving the single-repository hierarchy and rendered GitHub issue previews in the main editor area.

**Architecture:** Resolve one target from the project base path, load one repository directly into the existing milestone and parent/sub-issue grouping model, and keep `RepoIssueTarget` as the provider/editor context. Delete discovery, inclusion, repository-node, ownership-filtering, and obsolete artifact paths in the same cutover.

**Tech Stack:** Kotlin 2.1, Swing, IntelliJ Platform 2025.3, JCEF, JUnit 5, Gradle 9

---

## Source and Route Evidence

- **Source Spec:** `docs/superpowers/specs/2026-07-10-single-repository-issue-navigator-design.md`
- **Milestone:** `docs/superpowers/milestones/M1-issue-workflow-hardening.md`
- **Execution Route:** Inline implementation on `codex/linux-development-toolchain-migration` after the Linux toolchain validators are available.
- **Issue Route:** No new issue mirror. The user requested direct revision of this plugin version.
- **Publish Route:** Local commits only. Do not push, publish, open a pull request, or claim GitHub issue closure.

## Outcome Proof

**Intent:** Limit the issue tool window to the current project repository and keep rendered GitHub issues opening in the main editor.
**Current Behavior:** Anchor discovers attached Workspace Git roots, loads several repositories, filters forks and non-owned repositories, persists inclusion choices, and renders repository nodes above milestone and issue rows.
**Expected Outcome:** Anchor detects one project GitHub remote, renders milestone and issue rows without repository selection or repository nodes, and continues opening selected issues through the Anchor editor preview.
**Target Output:** Single-target panel construction, single-load tree behavior, removed multi-repo production paths and tests, retained editor-preview paths, deleted obsolete artifacts, updated current context, and installed-plugin proof.
**Owner:** `RemoteVcsIssuesPanel` owns primary target resolution; `RepoIssuesTreePanel` owns one target's navigator; `IssueEditorPreviewOpener` continues to own editor navigation.
**Interface:** The tool-window factory creates one issue panel for the project; issue selection calls `openIssue(target, issue)` exactly as before.
**Cutover:** Replace list/inclusion constructors and repository-node rendering in one compiling change, then delete unused discovery, inclusion, and access-filtering files.
**Replaced Path:** Delete `GitRootDiscovery`, repository inclusion types/dialog, account-login and repository-access curation, repository tree items/rendering, and their tests.
**Evidence:** New target-mapping tests, retained hierarchy/editor tests, zero-reference scans, Gradle test/verification/package receipts, and installed-plugin interaction proof.
**Acceptance Proof:** The installed tool window has no repository selector or repository root row, shows the current repository hierarchy, and opens a selected issue's rendered Markdown/comments in the main editor.
**Stop Criteria:** Stop on editor-preview regression, ambiguous remaining caller, failed Gradle gate, inability to install/reload the built plugin in the current IDE window, or a required change outside the approved removal boundary.
**Avoid:** Flat-list redesign, editor-preview refactoring, sibling-root fallback, runtime dependency removal, `runIde`, a second IDE window, external publication, and compatibility wrappers for deleted features.
**Risk:** `RepoIssuesTreePanel` combines Swing state, asynchronous loading, and tree construction. Test the pure target boundary first, keep grouping unchanged, and require installed-plugin proof for the UI path.

## Implementation Boundaries

**Files To Create:** `src/test/kotlin/com/itsjeel01/remotevcsmanager/ui/RemoteVcsIssuesPanelTest.kt`.
**Files To Modify:** `GitRemoteDetector.kt`, `RemoteVcsIssuesPanel.kt`, `RepoIssuesTreePanel.kt`, `RepoIssueTreeItem.kt`, `RepoIssuesTreeRenderer.kt`, `GitHubProvider.kt`, `JetBrainsGithubTokenProvider.kt`, `docs/superpowers/PROJECT_CONTEXT.md`, and `docs/superpowers/milestones/M1-issue-workflow-hardening.md`.
**Files To Avoid:** All `ui/editor` production files, `IssuePreviewDocument.kt`, issue comment/Markdown APIs, JCEF registration, `IssueTreeGrouping.kt`, milestone/relationship models and parsers, pull request/branch UI, and Gradle dependencies.
**Source Of Truth:** The approved single-repository design spec and the preserved editor-preview tests.
**Read Path:** `GitRemoteDetector.detect()` reads the project base Git root and origin remote; the provider reads one repository's milestones, issues, relationships, comments, and rendered Markdown.
**Write Path:** The tool window writes Swing tree state and editor-preview payload state only; removal does not add persistent settings.
**Integration Points:** Tool-window creation, Git remote detection, GitHub provider calls, issue tree selection, Anchor virtual files, JCEF file editor, project context, and M1 index.
**Migration Or Cutover:** Introduce single-target tests, change panel/tree interfaces, remove dead callers/files, delete obsolete artifacts, then run installed-plugin proof.
**Replaced Path Handling:** Delete old files and symbols outright. Do not retain empty services, redirects, disabled buttons, compatibility constructors, or unused access models.
**Acceptance Proof Gate:** All automated gates and the installed-plugin navigation scenario must pass before completion.

## Test-Complete Definition and Metrics

- New target mapping tests prove GitHub remote conversion and rejection of non-GitHub remotes.
- `GitRootDiscoveryTest.kt` and `RepoIssueInclusionStateTest.kt` are deleted with production code.
- Existing hierarchy and editor-preview tests pass unchanged unless fixture construction requires a signature-only update.
- Searches return zero production references to `GitRootDiscovery`, inclusion types, `RepoIssueTreeItem.Repository`, `getAccountLogins`, `getIssueTrackingAccess`, `HiddenFork`, or `HiddenNotOwned`.
- `./gradlew --no-daemon test`, `verifyPlugin`, and `buildPlugin` return status 0.
- Installed-plugin proof confirms zero repository selector/root rows and one successful rendered issue preview.
- Numerical tolerances do not apply; each requirement has a deterministic pass/fail result.

## Proof Oracle

```bash
./gradlew --no-daemon test --tests "com.itsjeel01.remotevcsmanager.ui.RemoteVcsIssuesPanelTest"
./gradlew --no-daemon test --tests "com.itsjeel01.remotevcsmanager.ui.IssueTreeGroupingTest"
./gradlew --no-daemon test --tests "com.itsjeel01.remotevcsmanager.ui.detail.IssuePreviewDocumentTest"
./gradlew --no-daemon test --tests "com.itsjeel01.remotevcsmanager.ui.editor.AnchorIssueVirtualFileTest"
./gradlew --no-daemon test --tests "com.itsjeel01.remotevcsmanager.ui.editor.IssueEditorPreviewOpenerTest"
./gradlew --no-daemon test
./gradlew --no-daemon verifyPlugin
./gradlew --no-daemon buildPlugin
rg -n 'GitRootDiscovery|RepoIssueInclusion|RepositoryInclusionDialog|RepoIssueTreeItem\.Repository|getAccountLogins|getIssueTrackingAccess|HiddenFork|HiddenNotOwned' src/main src/test
bash "$HOME/.codex/hooks/codex-cleanup.sh" --repo-root .
```

### Task 1: Lock the Single-Target Boundary with Tests

**Use Cases:**
- A GitHub origin from the primary project root becomes one `RepoIssueTarget`.
- HTTPS, SSH, and git-protocol hosts map to the correct issues URL.
- A non-GitHub remote produces no issue target.
- Acceptance proof covers the replacement of Workspace target lists with one current target.

**Files:**
- Create: `src/test/kotlin/com/itsjeel01/remotevcsmanager/ui/RemoteVcsIssuesPanelTest.kt`
- Modify: `src/main/kotlin/com/itsjeel01/remotevcsmanager/ui/RemoteVcsIssuesPanel.kt`

- [ ] **Step 1: Write failing tests for `createTarget`**

Construct `GitRemoteInfo` values with temporary `File` roots and assert owner, repo name, root path, display name, issues URL, and non-GitHub rejection through an internal `RemoteVcsIssuesPanel.createTarget` function.

- [ ] **Step 2: Run the focused test and verify failure**

Run the first Proof Oracle command. Expected: compilation fails because `createTarget` is private.

- [ ] **Step 3: Expose only the pure mapping seam**

Change `createTarget` from private to internal. Do not expose project or provider state.

- [ ] **Step 4: Run the focused test and verify pass**

Run the first Proof Oracle command. Expected: all target-mapping cases pass.

- [ ] **Step 5: Commit**

```bash
git add src/main/kotlin/com/itsjeel01/remotevcsmanager/ui/RemoteVcsIssuesPanel.kt src/test/kotlin/com/itsjeel01/remotevcsmanager/ui/RemoteVcsIssuesPanelTest.kt
git commit -m "test: lock single-repository target mapping"
```

### Task 2: Replace Workspace Discovery and Access Curation

**Use Cases:**
- Tool-window creation calls `GitRemoteDetector.detect()` once for the project repository.
- Attached sibling roots are not discovered after primary resolution succeeds or fails.
- A current accessible fork or non-owned repository loads normally.
- Missing GitHub credentials or remote data produces an explicit panel/tree error rather than a hidden repository.

**Files:**
- Delete: `src/main/kotlin/com/itsjeel01/remotevcsmanager/GitRootDiscovery.kt`
- Delete: `src/main/kotlin/com/itsjeel01/remotevcsmanager/ui/RepoIssueInclusionSettings.kt`
- Delete: `src/main/kotlin/com/itsjeel01/remotevcsmanager/ui/RepoIssueInclusionState.kt`
- Delete: `src/main/kotlin/com/itsjeel01/remotevcsmanager/ui/RepositoryInclusionDialog.kt`
- Delete: `src/test/kotlin/com/itsjeel01/remotevcsmanager/GitRootDiscoveryTest.kt`
- Delete: `src/test/kotlin/com/itsjeel01/remotevcsmanager/ui/RepoIssueInclusionStateTest.kt`
- Modify: `src/main/kotlin/com/itsjeel01/remotevcsmanager/GitRemoteDetector.kt`
- Modify: `src/main/kotlin/com/itsjeel01/remotevcsmanager/ui/RemoteVcsIssuesPanel.kt`
- Modify: `src/main/kotlin/com/itsjeel01/remotevcsmanager/providers/github/GitHubProvider.kt`
- Modify: `src/main/kotlin/com/itsjeel01/remotevcsmanager/providers/github/JetBrainsGithubTokenProvider.kt`

- [ ] **Step 1: Change panel construction to one target**

Replace account-login and target-list logic with `GitRemoteDetector(project).detect()?.let(::createTarget)`. Pass one target and the existing preview opener to `RepoIssuesTreePanel`. Use `Anchor could not detect a GitHub remote for this project.` only when target resolution fails.

- [ ] **Step 2: Remove curation-only APIs and files**

Make `GitRemoteDetector.detect(gitRoot)` private, delete root discovery/inclusion paths and tests, delete `getAccountLogins`, delete `getIssueTrackingAccess`, and delete `GitHubIssueTrackingAccess` after zero-reference confirmation.

- [ ] **Step 3: Compile and inspect references**

Run `./gradlew --no-daemon compileKotlin compileTestKotlin` and the zero-reference search. Expected: compilation identifies only the tree constructor/load changes owned by Task 3; no deleted type has another caller.

- [ ] **Step 4: Commit with Task 3 if compilation requires the interface cutover**

Do not commit a non-compiling intermediate state. Keep these deletions staged only with Task 3's compiling tree change.

### Task 3: Convert the Tree to One Repository Without a Repository Node

**Use Cases:**
- The header has no Repositories button or inclusion status.
- Refresh loads one target and shows one loading, error, empty, or hierarchy state.
- Milestones, parents, sub-issues, standalone issues, sorting, and open actions remain.
- Selecting any issue still invokes `IssueEditorPreviewOpener.openIssue(target, issue)`.
- The tree has no `RepoIssueTreeItem.Repository` row.

**Files:**
- Modify: `src/main/kotlin/com/itsjeel01/remotevcsmanager/ui/RepoIssuesTreePanel.kt`
- Modify: `src/main/kotlin/com/itsjeel01/remotevcsmanager/ui/RepoIssueTreeItem.kt`
- Modify: `src/main/kotlin/com/itsjeel01/remotevcsmanager/ui/RepoIssuesTreeRenderer.kt`

- [ ] **Step 1: Replace constructor and state**

Accept one `target`. Remove target lists, inclusion callbacks/state, repository dialog/button, selected-target mutation, and visible-target counts. Keep `selectedIssue` and make Open Repo use the constructor target.

- [ ] **Step 2: Replace batch loading**

Load issues, milestones, and relationships once inside `runCatching`. Reduce `RepoLoadResult` to `Loaded(issues, milestones, relationships)` and `Failed(message)`.

- [ ] **Step 3: Build rows directly under the invisible root**

Render loading/message or milestone nodes directly. Remove repository item creation, repository selection handling, hidden result types, hidden counts, and repository rendering. Keep selectable items and milestone items carrying `target`.

- [ ] **Step 4: Run focused hierarchy and editor tests**

Run the first five Proof Oracle test commands. Expected: target, grouping, document, virtual-file, and preview-opener tests pass.

- [ ] **Step 5: Commit Tasks 2 and 3 together**

```bash
git add -A src/main src/test
git commit -m "refactor: restore single-repository issue navigation"
```

### Task 4: Delete Obsolete Multi-Repository Artifacts

**Use Cases:**
- Completed multi-repository design, plan, and issue mirrors are absent from this version.
- The editor-preview design, plan, and issue 1 remain.
- Project context describes one current repository and Linux proof commands.
- Milestone/index pages do not link deleted artifacts or claim Workspace multi-repo behavior remains.

**Files:**
- Delete: `docs/superpowers/specs/2026-06-29-tool-window-navigation-refresh-design.md`
- Delete: `docs/superpowers/plans/2026-06-29-m1-tool-window-navigation-refresh-plan.md`
- Delete: `docs/superpowers/issues/3-load-github-milestones-and-issue-relationships.md`
- Delete: `docs/superpowers/issues/4-group-issue-tree-by-milestones-parent-issues-and-sub-issues.md`
- Delete: `docs/superpowers/issues/5-wire-complete-issue-tree-rendering-and-workspace-proof.md`
- Modify: `docs/superpowers/PROJECT_CONTEXT.md`
- Modify: `docs/superpowers/milestones/M1-issue-workflow-hardening.md`
- Modify: `docs/superpowers/specs/README.md`
- Modify: `docs/superpowers/plans/README.md`
- Modify: `docs/superpowers/issues/README.md`

- [ ] **Step 1: Delete the approved artifact set**

Delete only the five paths named above. Preserve the June 26 editor-preview artifacts and issue 1.

- [ ] **Step 2: Repair current indexes and context**

Remove links and current-state claims for Workspace discovery, repository inclusion, ownership filtering, and repository grouping. Document the single current-repository navigator and `./gradlew`/Bash proof path.

- [ ] **Step 3: Validate retained artifacts and references**

Run the repository Bash validators against retained specs/plans/issues and search for links to each deleted filename. Expected: validators pass and deleted-path references equal zero.

- [ ] **Step 4: Commit**

```bash
git add -A docs/superpowers
git commit -m "docs: remove multi-repository workflow artifacts"
```

### Task 5: Verify, Install, and Exercise the Revised Plugin

**Use Cases:**
- Automated tests prove the single-target boundary, hierarchy, and editor preview.
- Packaging and plugin verification pass after dead-path removal.
- The current IntelliJ Workspace window runs the newly built ZIP.
- Visual proof confirms no repository selector/root row and successful rendered issue preview navigation.

**Files:**
- Verify: all production, test, and documentation paths changed by Tasks 1 through 4
- Build artifact: `build/distributions/anchor-*.zip`

- [ ] **Step 1: Apply completion verification discipline**

Use `superpowers:verification-before-completion` before any success claim.

- [ ] **Step 2: Run all automated proof**

Run the complete Proof Oracle. The final zero-reference search should return no matches and therefore status 1; treat any printed match as failure.

- [ ] **Step 3: Install into the current IDE window**

Install the generated ZIP into the currently open IntelliJ IDEA Workspace window and reload that same window. Do not run `runIde` or open another project window.

- [ ] **Step 4: Exercise the UI path**

Confirm the tool window has no repository selector/root row, shows the current repository hierarchy, and opens one issue's rendered Markdown and comments in the main editor.

- [ ] **Step 5: Run cleanup and record evidence**

Run the repo cleanup hook, inspect `git diff --check`, list branch commits, and record automated plus installed-plugin results. Do not create an empty proof commit.

## Decision Ledger

| Decision | Source | Answer | Impact | Deferred? | Risk owner |
|---|---|---|---|---|---|
| Product scope | Source spec and user request | Remove Workspace multi-repository behavior and preserve editor-rendered issues. | Defines the cutover and preservation boundary. | No | User |
| Navigator shape | User native answer | Use one repository with milestones and parent/sub-issues directly under the root. | Removes repository grouping without flattening useful structure. | No | User |
| Access policy | User native answer | Show the current accessible repository regardless of fork or owner. | Deletes account-login and repository-access curation. | No | User |
| Primary repository | Source spec | Resolve from the project base Git root through `GitRemoteDetector.detect()`. | Ignores attached sibling roots and avoids a replacement selector. | No | Codex |
| Editor interface | Repo evidence | Keep `RepoIssueTarget` and `openIssue(target, issue)`. | Preserves the editor subsystem without signature churn. | No | Codex |
| Artifact policy | User native answer | Delete multi-repository artifacts and preserve editor-preview artifacts. | Removes obsolete history while retaining the desired feature record. | No | User |
| TDD policy | Planning discipline | Add failing pure target-mapping tests before exposing the mapping seam. | Proves the single-target boundary before production behavior changes. | No | Codex |
| Tree testing | Repo evidence | Keep grouping/editor tests and use installed-plugin proof for Swing integration. | Covers pure behavior and the actual UI path without brittle Swing internals. | No | Codex |
| Commit boundary | Compilation dependency | Commit discovery/access deletion with the single-tree interface change. | Avoids a deliberately broken intermediate commit. | No | Codex |
| Documentation | Source spec | Repair current context and indexes after deleting exactly five artifacts. | Prevents dangling links and stale current-state claims. | No | Codex |
| Visual verification | Project instructions | Install the built ZIP into the current Workspace IDE and reload it. | Verifies the deployed plugin rather than a sandbox instance. | No | User |
| Publish behavior | Request scope | Keep changes local and do not push or publish. | Avoids external mutation outside the request. | No | Codex |
| Stop criteria | Outcome Proof | Stop on editor regression, ambiguous caller, failed Gradle gate, unavailable IDE install/reload, or scope expansion. | Prevents incomplete removal or unproven preservation. | No | Codex |
