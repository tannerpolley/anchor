---
title: Single-Repository Issue Navigator
milestone: M1 - Issue Workflow Hardening
status: approved-design
owner: codex
date: 2026-07-10
---

# Single-Repository Issue Navigator

## Project Context Evidence

- `GitRootDiscovery` merges Git repository manager roots, IDE VCS mappings, module content roots, `.idea/vcs.xml` mappings, and the project root to discover multiple Workspace repositories.
- `RemoteVcsIssuesPanel.resolveTargets` converts every discovered GitHub root into a `RepoIssueTarget` and supplies the full target list to `RepoIssuesTreePanel`.
- `RepoIssueInclusionSettings`, `RepoIssueInclusionState`, and `RepositoryInclusionDialog` exist only to persist and edit the multi-repository target set.
- `RepoIssuesTreePanel` performs batch loading across targets, filters forks and non-owned repositories, renders repository nodes, and tracks repository selection state.
- `GitRemoteDetector.detect()` already resolves one remote from the project base path. Its `detect(gitRoot: File)` overload exists for multi-root fan-out.
- The editor issue preview is a separate subsystem built from `AnchorIssueVirtualFile`, `AnchorIssuePreviewStore`, `AnchorIssueFileEditorProvider`, `AnchorIssueFileEditor`, `IssueEditorPreviewOpener`, and `IssuePreviewDocument`.
- `IssueEditorPreviewOpener.openIssue` consumes `RepoIssueTarget` for owner and repository context. Keeping that small model avoids coupling editor preview code to navigator internals.
- Milestone, parent/sub-issue, sorting, Markdown, and comment APIs work for one repository and do not require Workspace discovery.

## Problem

The current issue tool window treats an IntelliJ Workspace as a collection of GitHub repositories. It discovers attached roots, filters repositories by ownership and fork status, persists inclusion choices, and adds a repository level to the issue tree. That scope is larger than the desired product.

Anchor should navigate issues for the current project repository only. It must retain the feature that opens a GitHub issue as rendered Markdown in the main editor area.

## Goals

- Resolve one GitHub repository from the IntelliJ project base path.
- Stop discovering or loading attached Workspace repository roots.
- Remove repository inclusion persistence and selection UI.
- Remove repository-level nodes from the issue tree.
- Remove fork and account-ownership suppression for the current accessible repository.
- Keep milestone grouping, parent/sub-issue hierarchy, sorting, refresh, and open actions for the current repository.
- Keep editor-preview navigation, GitHub Markdown rendering, comments, metadata, JCEF rendering, and Compose fallback behavior.
- Delete obsolete multi-repository design, plan, and issue-mirror artifacts.
- Preserve the editor-preview design, plan, and issue mirror.

## Non-Goals

- Replacing the issue tree with a flat list.
- Removing milestones or GitHub parent/sub-issue relationships.
- Changing the editor preview HTML, virtual-file identity, or navigation behavior.
- Selecting a repository from the active editor file or current VCS root.
- Supporting a user-selected repository override.
- Changing pull request or branch behavior elsewhere in Anchor.
- Removing `Git4Idea` or the bundled GitHub plugin dependency.
- Rewriting completed editor-preview history.

## Approaches Considered

### Design 1: Single-Repository Hierarchy

Use `GitRemoteDetector.detect()` to resolve the project repository, remove Workspace discovery and repository inclusion, and render milestones and issues directly under the tree's invisible root. Retain one `RepoIssueTarget` as the context passed to provider and editor-preview calls.

This removes the unwanted feature at its boundary while preserving the issue organization and editor experience.

### Design 2: Single Visible Repository Node

Stop multi-root discovery and inclusion controls but retain the repository node above milestones and issues.

This is a smaller tree refactor, but it leaves a redundant grouping level and visible UI from the removed feature.

### Design 3: Flat Issue List

Remove repository, milestone, and parent/sub-issue nodes and show one sorted list of issues.

This produces the smallest navigator. It also discards useful single-repository organization that does not depend on Workspace support.

## Recommended Approach

Use Design 1. The user selected a single-repository hierarchy, removal of ownership/fork suppression, and deletion of obsolete multi-repository artifacts.

## Architecture

`RemoteVcsIssuesPanel` owns current-repository resolution. It calls `GitRemoteDetector(project).detect()` once and maps a GitHub remote to one `RepoIssueTarget`. A missing or unsupported remote produces the existing unavailable state.

`RepoIssuesTreePanel` owns one repository's issue data. Its constructor receives one target rather than a target list or inclusion state. Refresh performs one access path and loads milestones, issues, and relationships once.

The tree model starts at milestones and informational states. It no longer creates repository nodes. Issue rows continue to carry the single target so selection can call `IssueEditorPreviewOpener.openIssue` without changing the editor-preview interface.

The editor subsystem remains unchanged. The file-editor provider, virtual file, payload store, Markdown/comment loading, and HTML rendering do not depend on Workspace root discovery.

## Components

### Current Repository Resolution

- Keep `GitRemoteDetector.detect()`.
- Remove `GitRootDiscovery`.
- Remove `GitRemoteDetector.detect(gitRoot: File)` after its final caller disappears.
- Replace `RemoteVcsIssuesPanel.resolveTargets` with a single-target resolver.

The project base path and its ancestor Git root define the current repository. Attached sibling roots do not participate.

### Single-Repository Tree Panel

- Replace `allTargets`, `initialInclusionState`, and `onInclusionChanged` with one `target`.
- Remove the Repositories button and selection dialog.
- Remove mutable inclusion state and empty-selection states.
- Replace target batch loading with one load operation.
- Remove hidden-fork, hidden-owner, and excluded-repository status counts.
- Keep sort, refresh, open issue, and open repository actions.

### Tree Items and Renderer

- Remove `RepoIssueTreeItem.Repository`.
- Remove repository-row rendering.
- Build milestone, parent, sub-issue, standalone issue, information, and error rows directly beneath the invisible root.
- Keep `RepoIssueTarget` on selectable rows as provider/editor context.

### Access Policy

Remove multi-repository curation based on account login, repository ownership, and fork status. If the current GitHub repository is accessible through the provider, load it. API or authentication failures render explicit errors.

Remove `GitHubIssueTrackingAccess`, `RemoteVcsProvider.getIssueTrackingAccess`, its GitHub implementation, and `JetBrainsGithubTokenProvider.getAccountLogins` when no other caller remains.

### Editor Issue Preview

Keep these paths and their behavior:

- `ui/editor/AnchorIssueVirtualFile.kt`
- `ui/editor/AnchorIssuePreviewStore.kt`
- `ui/editor/AnchorIssueFileEditorProvider.kt`
- `ui/editor/AnchorIssueFileEditor.kt`
- `ui/editor/IssueEditorPreviewOpener.kt`
- `ui/detail/IssuePreviewDocument.kt`
- Markdown rendering and issue-comment provider methods
- JCEF module and file-editor-provider registration in `plugin.xml`

## Data Flow

1. The issue tool window asks `GitRemoteDetector.detect()` for the project repository.
2. A GitHub remote becomes one `RepoIssueTarget`.
3. The tree refresh loads milestones, open issues, and issue relationships for that target.
4. `IssueTreeGrouping` builds milestone and parent/sub-issue rows.
5. The tree renders those rows directly without a repository parent node.
6. Selecting an issue row calls `IssueEditorPreviewOpener.openIssue` with the target and issue.
7. The opener loads comments and rendered GitHub Markdown, stores the preview payload, and opens the Anchor virtual file in the main editor area.

## Error Handling

- If the project has no base path, Git root, remote, or GitHub remote, show the existing unavailable state with no synthetic target.
- If authentication or an API call fails, show the failure under the single tree root and keep refresh available.
- Do not hide the current repository merely because it is a fork or belongs to another account.
- Do not scan sibling Workspace roots after primary-project detection fails.
- Do not change editor-preview failure handling. Comment or Markdown failures remain visible through the existing preview error paths.
- Remove obsolete empty states that instruct users to select repositories.

## Artifact Cutover

Delete the obsolete multi-repository implementation records:

- `docs/superpowers/specs/2026-06-29-tool-window-navigation-refresh-design.md`
- `docs/superpowers/plans/2026-06-29-m1-tool-window-navigation-refresh-plan.md`
- `docs/superpowers/issues/3-load-github-milestones-and-issue-relationships.md`
- `docs/superpowers/issues/4-group-issue-tree-by-milestones-parent-issues-and-sub-issues.md`
- `docs/superpowers/issues/5-wire-complete-issue-tree-rendering-and-workspace-proof.md`

Keep the editor-preview records:

- `docs/superpowers/specs/2026-06-26-issue-editor-preview-design.md`
- `docs/superpowers/plans/2026-06-26-m1-issue-editor-preview-plan.md`
- `docs/superpowers/issues/1-render-github-issues-in-editor-preview-and-group-by-milestone.md`

Update milestone and project-context indexes so they do not reference deleted artifacts or describe Workspace multi-repository behavior as current.

## Testing and Proof Oracles

Automated proof:

- Delete `GitRootDiscoveryTest.kt` and `RepoIssueInclusionStateTest.kt` with their production paths.
- Keep `IssueTreeGroupingTest.kt` and `GitHubProviderIssueStructureTest.kt` for single-repository hierarchy behavior.
- Keep `IssuePreviewDocumentTest.kt`, `AnchorIssueVirtualFileTest.kt`, `AnchorIssueFileEditorTest.kt`, and `IssueEditorPreviewOpenerTest.kt` for editor behavior.
- Add focused coverage for single-target resolution or extracted tree-model construction where the logic can remain independent from Swing.
- Confirm no production reference remains to `GitRootDiscovery`, repository inclusion types, `RepoIssueTreeItem.Repository`, `getAccountLogins`, or `getIssueTrackingAccess`.
- Run `./gradlew --no-daemon test`.
- Run `./gradlew --no-daemon verifyPlugin`.
- Run `./gradlew --no-daemon buildPlugin`.

Installed-plugin proof:

- Install the built ZIP into the currently open IntelliJ IDEA Workspace window.
- Reload that same window without starting `runIde` or another IDE instance.
- Confirm the issue tool window has no repository selector and no repository root row.
- Confirm the current repository still shows milestones, parents, sub-issues, and standalone issues.
- Select an issue and confirm its GitHub Markdown and comments open in the main editor preview.

## Open Questions

None. The user approved the single-repository hierarchy, accessible-repository policy, artifact deletion boundary, and installed-plugin proof path.

## Decision Ledger

| Decision | Source | Answer | Impact | Deferred? | Risk owner |
|---|---|---|---|---|---|
| Product scope | User request | Remove Workspace multiple-repository features while keeping rendered GitHub issues in the main editor area. | Defines the removal boundary and preserved outcome. | No | User |
| Navigator shape | User native answer | Use a single-repository hierarchy without a visible repository node. | Keeps milestones and parent/sub-issues while removing repository grouping. | No | User |
| Repository source | Repo evidence and approved design | Use `GitRemoteDetector.detect()` from the project base path. | Ignores attached sibling roots and avoids a new selection rule. | No | Codex |
| Access policy | User native answer | Show the current repository whenever it is accessible. | Removes fork and ownership suppression plus account-login filtering. | No | User |
| Editor preview | User request and repo evidence | Preserve the complete Anchor file-editor preview subsystem. | Protects virtual-file, JCEF, Markdown, comment, and navigation behavior. | No | User |
| Issue hierarchy | User native answer | Keep sorting, milestones, parent/sub-issues, and standalone issues. | Limits removal to Workspace behavior rather than useful single-repo structure. | No | User |
| Target context | Approved design | Retain `RepoIssueTarget` as the provider and editor-preview context. | Avoids coupling the editor subsystem to navigator internals. | No | Codex |
| Multi-repo artifacts | User native answer | Delete obsolete multi-repository specs, plans, and issue mirrors. | Removes records for behavior intentionally removed from this version. | No | User |
| Editor artifacts | Repo evidence and approved scope | Preserve the June 26 editor-preview artifacts and issue mirror 1. | Retains design history for the feature that remains. | No | Codex |
| Failure behavior | User design approval | Show explicit primary-repository and API failures without scanning siblings. | Prevents fallback to the removed Workspace behavior. | No | User |
| Test policy | User design approval | Retain editor and hierarchy tests, delete obsolete tests, add single-target proof, and run full Gradle gates. | Provides regression coverage across removal and preservation boundaries. | No | User |
| Visual proof | Project instructions and user design approval | Install the built plugin into the current Workspace IDE window and reload it. | Verifies the actual tool-window and editor interaction without `runIde`. | No | User |
