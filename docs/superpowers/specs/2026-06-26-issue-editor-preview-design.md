---
title: Issue Editor Preview and Milestone Grouping
milestone: M1 - Issue Workflow Hardening
status: approved-design
owner: codex
date: 2026-06-26
---

# Issue Editor Preview and Milestone Grouping

## Project Context Evidence

- `RepoIssuesTreePanel` currently builds a horizontal split pane with the issue tree on the left and `SwingIssueDetailRenderer.component` on the right.
- Issue selection currently calls `loadIssueDetail`, fetches comments, renders body and comments through the GitHub Markdown API, and sends the rendered HTML into the detail renderer.
- `SwingIssueDetailRenderer` owns a JCEF browser and already builds themed HTML for issue metadata, labels, body, and comments.
- `Issue` already has `milestone: String?`, and `GitHubProvider.toIssue` parses the GitHub issue milestone title.
- The current tree hierarchy is `Repo -> Issue`. `RepoIssuesTreeRenderer` displays a milestone suffix on issue rows but does not group by milestone.
- Recent commits are focused on issue workflow hardening: workspace repo discovery, authenticated issue rendering, collapsible repo nodes, and issue sorting.
- The Superpowers roadmap links this work to `M1 - Issue Workflow Hardening`.

## Problem

Anchor's issue browsing now supports repository grouping and rendered GitHub Markdown, but issue details still live inside the plugin tool window. The requested feature moves rendered issue details into the main editor pane using IDE preview-tab behavior, while keeping the tool window as a compact issue navigator.

The same feature should organize issues inside each repository by milestone dropdowns. The desired default hierarchy is `Repo -> Milestone -> Issue`, matching the existing collapsible repository behavior.

## Goals

- Replace the plugin-pane issue detail surface with a main editor preview tab.
- Render issue metadata, labels, body, and comments as GitHub-rendered HTML in the editor area.
- Reuse one temporary issue preview tab as different issues are clicked.
- Group issues inside each repository by milestone title, with a `No Milestone` group for unassigned issues.
- Keep repository nodes expanded by default and milestone nodes mostly collapsed, expanding the first active milestone.
- Keep milestone metadata scope title-based for this slice.
- Preserve explicit Open in Browser behavior for full GitHub UI work.

## Non-Goals

- Editing GitHub issue bodies from the editor preview.
- One persistent editor tab per issue.
- Pull request editor previews.
- Full GitHub milestone summaries, due-date ordering, or milestone descriptions.
- Creating GitHub labels, milestones, issue mirrors, or a GitHub Project board.
- Changing the current open-issue loading scope unless a later plan adds issue-state filters.

## Recommended Approach

Use a custom virtual issue file plus a custom JCEF-backed `FileEditorProvider`.

`RepoIssuesTreePanel` remains the navigator. When an issue is selected, it asks a small preview-opening service to load comments, render body and comments through GitHub Markdown rendering, build a sanitized HTML document, and open a stable Anchor issue virtual file in the IDE editor preview tab.

The custom file editor owns the JCEF browser in the editor area. HTML assembly should be extracted from the current `SwingIssueDetailRenderer` into a shared issue document builder so the old split-pane path can be removed cleanly instead of duplicating title, label, body, comment, sanitizer, and theme behavior.

## Alternatives Considered

### Design 2 - Markdown Virtual File With IDE Preview

This would create an in-memory Markdown document and rely on IDE Markdown preview behavior.

Tradeoff: it is smaller when the Markdown plugin is enabled, but it weakens the requirement to show GitHub-rendered Markdown, makes comments and metadata awkward, and varies by IDE setup.

### Design 3 - Editor HTML Document Adapter

This would open generated HTML in the editor area and rely on existing file associations.

Tradeoff: it avoids a custom file editor, but it risks showing HTML source or inconsistent rendering instead of a controlled issue preview.

## Architecture

The feature separates navigation from rendering.

- The tool window owns repository, milestone, and issue navigation.
- The preview-opening service owns async detail loading and editor-opening behavior.
- The virtual file owns stable issue identity.
- The file editor owns browser lifecycle and read-only rendered display.
- The document builder owns themed HTML construction.

The existing split-pane detail renderer should not remain as a parallel issue-detail surface after the editor preview path owns display behavior.

## Components

### Issue Tree Model

Add `RepoIssueTreeItem.Milestone` with:

- `target: RepoIssueTarget`
- `title: String`
- `openIssueCount: Int`

Update node construction so each loaded repo contains milestone nodes, and each milestone node contains issue nodes. Use `issue.milestone?.takeIf { it.isNotBlank() } ?: "No Milestone"` as the grouping key.

### Preview Opener Service

Add a small project-level service that accepts `RepoIssueTarget` plus `Issue`.

Responsibilities:

- maintain request ids so stale async detail loads cannot overwrite newer selections
- fetch issue comments
- render issue body and comments with GitHub Markdown API using `owner/repo` context
- build the issue preview document
- open the issue virtual file in editor preview mode

### Virtual Issue File

Create an Anchor-owned in-memory file keyed by:

- provider
- owner
- repo
- issue number

The presentable name should be stable and readable, for example `owner/repo#123.md`. The file content should not be the canonical source of the rendered preview; the custom editor should consume prepared issue preview data.

### File Editor Provider And Editor

Register a file editor provider that accepts only Anchor issue virtual files.

The editor should:

- be read-only
- render through JCEF
- show issue title and repo identity in editor presentation
- load sanitized HTML built from GitHub-rendered body and comments
- dispose browser resources through the IntelliJ disposable lifecycle

### Issue Document Builder

Extract issue HTML assembly from `SwingIssueDetailRenderer` into a reusable builder.

Inputs:

- issue
- comments
- rendered body HTML
- rendered comment HTML
- IDE theme colors
- explicit error state when detail rendering fails

Outputs:

- a complete HTML document for JCEF

## Data Flow

1. Refresh loads open issues for each visible repository through the existing GitHub provider path.
2. Loaded issues are grouped by milestone title under each repo.
3. Tree rendering shows repo counts and milestone counts.
4. Selecting a repo or milestone updates navigator selection only.
5. Selecting an issue starts a new preview detail request.
6. The preview opener fetches comments and renders body/comment Markdown through GitHub.
7. The issue document builder creates sanitized themed HTML.
8. The opener creates or updates the virtual issue file identity.
9. The IDE editor manager opens that file as a reusable preview tab.
10. The custom file editor loads the prepared HTML into JCEF.

## Error Handling

- Repo issue loading failures stay represented as per-repo failure nodes in the tree.
- Detail loading failures open an editor preview error document for the selected issue with the exact failure message.
- GitHub Markdown rendering failures are explicit issue preview errors, not silent substitutions.
- JCEF browser creation failures show a clear editor-tab diagnostic panel.
- Async races keep the existing request-id pattern so older issue clicks cannot overwrite newer preview content.
- Issues without milestones group under `No Milestone`.
- Empty milestone groups are not shown.
- The implementation plan must verify the JetBrains preview-tab API against the configured platform build before coding against it.

## Testing And Proof Oracles

Automated proof candidates:

- Unit-test milestone grouping with multiple milestones, no milestone, empty lists, and mixed updated dates.
- Unit-test virtual issue file identity so the same owner/repo/issue reuses the same preview target while different issues do not collide.
- Unit-test the issue document builder for labels, body, comments, sanitizer boundaries, and explicit error documents.
- Add stale-request tests if the preview opener is extracted behind a testable boundary.
- Run `.\gradlew.bat test`.
- Run `.\gradlew.bat verifyPlugin` because the feature adds a `FileEditorProvider` extension.

HITL proof candidates:

- In a sandbox IDE, click issues across multiple repos and milestones.
- Confirm the main editor preview tab reuses for successive issue selections.
- Confirm the plugin tool window no longer shows its own issue detail pane.
- Confirm rendered body and comments match the current detail renderer capability.
- Confirm `No Milestone` grouping works.

## Open Questions

- Which exact IntelliJ Platform preview-tab API should be used for build `253.1`? This is deferred to implementation planning and must be checked against the configured platform dependency before code changes.
- Should a later slice add state filters before milestone grouping, or keep the current open-issues-only scope? This spec keeps current open-issue behavior.

## Decision Ledger

| Decision | Source | Answer | Impact | Deferred? | Risk owner |
|---|---|---|---|---|---|
| Spec topic | User custom answer | Create a feature spec for rendered GitHub Markdown issues in the main file editor pane and milestone dropdown grouping inside each repo. | Sets the spec scope around editor preview and milestone tree structure. | No | User |
| Visual companion | User native answer | No visual companion. | Brainstorm stays text-only. | No | User |
| Issue detail surface | User native answer | Replace the plugin detail pane. | Tool window becomes a navigator and editor preview owns issue detail display. | No | User |
| Preview content | User native answer | Body plus comments. | Maintains parity with the current detail renderer. | No | User |
| Editor tab lifecycle | User native answer | Reusable preview tab. | Successive issue clicks should update one temporary preview tab. | No | User |
| Tree hierarchy | User native answer | `Repo -> Milestone -> Issue`. | Adds milestone nodes under each repository. | No | User |
| Default expansion | User native answer | Repos open by default, milestone groups mostly collapsed. | Keeps the navigator dense but scannable. | No | User |
| Milestone metadata scope | User native answer | Title grouping only. | Avoids new milestone API calls in the first slice. | No | User |
| Design approach | User native answer | Design 1, custom virtual issue file plus JCEF file editor. | Provides controlled GitHub-rendered preview in the main editor pane. | No | User |
| Architecture approval | User native answer | Approved. | Confirms navigation/rendering split. | No | User |
| Component approval | User native answer | Approved. | Confirms issue tree model, preview opener, virtual file, editor provider, and document builder boundaries. | No | User |
| Data flow approval | User native answer | Approved. | Confirms async load, render, virtual file, and preview editor flow. | No | User |
| Error handling approval | User native answer | Approved. | Confirms explicit error documents and request-id race handling. | No | User |
| Testing approval | User native answer | Approved. | Confirms unit, plugin verification, and manual IDE proof expectations. | No | User |
| Preview-tab API | Repo evidence | Verify exact IntelliJ Platform API during implementation planning. | Prevents coding against an assumed editor-preview method. | Yes | Implementation plan |
