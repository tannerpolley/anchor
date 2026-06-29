---
title: Tool Window Navigation Refresh
milestone: M1 - Issue Workflow Hardening
status: draft-design
owner: codex
date: 2026-06-29
---

# Tool Window Navigation Refresh

## Project Context Evidence

- `RemoteVcsIssuesPanel` detects GitHub repository targets from Workspace Git roots and passes them into `RepoIssuesTreePanel`.
- `RepoIssuesTreePanel` currently builds a single-row header with a west title and east action row. In narrow tool-window widths, `GitHub Issues`, status text, sort controls, and buttons can visually collide.
- `RepoIssuesTreePanel` currently loads open issues for every detected target, then hides forks and non-owned repos through GitHub access checks.
- `IssueMilestoneGrouping` only derives milestone groups from loaded open issues, so GitHub milestones with zero open issues do not appear.
- The earlier approved spec `2026-06-26-issue-editor-preview-design.md` explicitly kept empty milestone groups out of scope. This spec revises that scope.
- `IssueEditorPreviewOpener` and the Anchor issue editor are now the issue detail surface. This spec must not reintroduce a tool-window detail pane.

## Problem

The Anchor issue tool window is now useful as a multi-repository navigator, but the top header does not hold up visually at narrow widths. The tree also treats every detected repository as included, only shows milestones that appear on open issues, and has no parent/sub-issue hierarchy.

For real Workspace use, users need to decide which detected repos belong in the Anchor issue navigator. They also need the tree to reflect GitHub organization more completely: every GitHub milestone should be visible as a dropdown, even when it has no open issues, and parent issues should be expandable with their sub-issues nested underneath.

## Goals

- Fix the top header so title, status, sort, refresh, and open actions do not overlap.
- Add a per-project repo include/exclude control for detected GitHub repos.
- Persist repo inclusion choices for the IntelliJ project.
- Load GitHub milestone records for each included repo and show every milestone as a dropdown.
- Keep `No Milestone` as a generated group only when open issues have no milestone.
- Use GitHub's parent/sub-issue data as the source of truth for nesting.
- Render issue hierarchy as `Repo > Milestone > Parent Issue > Subissue`.
- Keep the main editor Anchor issue preview as the detail surface.

## Non-Goals

- Building a full GitHub Project board clone.
- Adding pull request hierarchy or PR milestone grouping.
- Inferring sub-issues from body links or task lists.
- Letting users locally invent parent/sub-issue relationships.
- Global plugin-wide repo inclusion preferences.
- Editing GitHub milestones, issue parentage, or issue metadata from Anchor.
- Reintroducing a tool-window detail pane.

## Recommended Approach

Use a focused navigator refresh inside the existing Swing issue tool window.

The header should become responsive: when the tool window is too narrow for one row, status and actions reflow instead of drawing over each other. The tree should continue to use `JTree`, but the tree model should be built from richer API-backed inputs: detected repo targets, per-project inclusion preferences, GitHub milestones, open issues, and GitHub parent/sub-issue relationships.

This keeps the work in `M1 - Issue Workflow Hardening` and avoids a larger tool-window workspace manager.

## Alternatives Considered

### Design 2 - Tool-Window Workspace Manager

Turn the Anchor issue panel into a broader management surface with a repo picker drawer, saved filter presets, multiple hierarchy modes, issue-state filters, milestone metadata, and parent/sub-issue view modes.

Tradeoff: this could become powerful, but it is too broad for one M1 implementation plan and risks mixing issue browsing with project-board behavior.

### Design 3 - Split Bugfix And Feature Spec

Handle the top-header visual overlap as a separate bugfix and write a separate feature spec for repo inclusion, empty milestones, and sub-issue nesting.

Tradeoff: this would reduce the immediate UI risk, but it splits one navigator experience across multiple artifacts and delays verifying that the header layout still works once the repo inclusion entry point exists.

## Architecture

Anchor's issue tool window remains a navigator with four clear responsibilities.

The header layout owns compact controls only. It should present title, status, sort, refresh, issue action, repo action, and repo inclusion entry point without overlap. Narrow widths must reflow controls instead of hiding state or drawing text under controls.

Repository inclusion owns the per-project set of tracked repositories. Auto-detected GitHub remotes form the available repo list. Newly detected repos default to included; user-excluded repos stay excluded for this IntelliJ project.

Issue structure owns API-backed grouping. For each included repo, Anchor loads GitHub milestones, open issues, and parent/sub-issue relationships, then builds `Repo > Milestone > Parent Issue > Subissue`.

The editor preview remains the issue-detail surface. Selecting an issue, parent issue, or sub-issue opens the rendered Anchor issue preview. Selecting a repository or milestone only changes navigator selection and button state.

## Components

### Responsive Issue Header

`RepoIssuesTreePanel.createHeader` should be revised so it does not rely on a single `BorderLayout.WEST` title plus `BorderLayout.EAST` action row. The implementation can use a small two-row panel, toolbar-like layout, or another Swing layout that wraps or stacks cleanly at narrow widths.

The header should show:

- title: `GitHub Issues`
- status: total open issues, hidden forks, hidden non-owned repos, and excluded repos
- sort control
- refresh action
- open issue action
- open repo action
- repo inclusion action

### Repo Inclusion State

Add a per-project persistent model for included and excluded repo keys. A repo key should be stable across reloads, for example `provider/owner/repoName/rootPath` or another key that avoids collisions when the same remote appears in multiple attached roots.

The inclusion UI should list detected GitHub targets with checkboxes. Applying changes should reload the tree.

### GitHub Milestone Loading

Add provider/API support for listing repository milestones. The grouping layer should receive the milestone records for each included repo. Every GitHub milestone should render even when it has zero open issues.

Milestone metadata beyond title and open issue count can stay out of the first implementation unless the API response already makes it cheap and stable.

### Parent/Sub-Issue Loading

Add provider/API support for GitHub parent/sub-issue relationships. This spec intentionally uses GitHub's relationship data only; it does not parse issue bodies, task lists, or references.

If the exact API route or version requires additional verification, implementation planning must verify it before coding against it.

### Issue Tree Grouping

Replace or extend `IssueMilestoneGrouping` with a richer pure grouping model. A likely name is `IssueTreeGrouping`.

Inputs:

- repo target
- GitHub milestone records
- open issues
- parent/sub-issue relationships
- selected sort option

Output:

- milestone groups for every GitHub milestone
- generated `No Milestone` group only when needed
- parent issue groups containing sub-issue rows
- standalone issue rows for issues without loaded children or parents

### Tree Items And Rendering

Extend `RepoIssueTreeItem` with enough structure to distinguish:

- repository rows
- milestone rows, including `0 open`
- parent issue rows with children
- sub-issue rows
- standalone issue rows
- informational and error rows

Parent issue rows should open the parent issue on selection while still supporting normal disclosure-arrow expansion. Sub-issues should open their own rendered preview when selected.

## Data Flow

1. Tool-window creation detects GitHub repo targets from Workspace Git roots.
2. Per-project inclusion state filters the detected targets.
3. Refresh cancels stale preview loads and loads data for included repos.
4. For each included repo, Anchor checks issue tracking access.
5. For each trackable repo, Anchor loads GitHub milestones, open issues, and parent/sub-issue relationship data.
6. The grouping model builds the tree hierarchy.
7. The tree renders repos, milestones, parent issue groups, sub-issues, standalone issues, messages, and failures.
8. Selecting repo or milestone rows updates selected target and button state.
9. Selecting parent issue, sub-issue, or standalone issue rows opens the existing rendered editor preview.
10. Header status summarizes open issues, excluded repos, hidden forks, hidden non-owned repos, and failures.

## Error Handling

- Header overlap is treated as a layout bug. The fix should reflow controls; it should not rely on text truncation as the primary solution.
- If inclusion state references a repo that is no longer detected, Anchor should ignore it for the current tree and preserve the stored preference.
- If all detected repos are excluded, the tree should show an explicit empty state with a repo inclusion action.
- If milestone loading fails for a repo, the repo node should show an explicit failure child and skip issue grouping for that repo.
- If sub-issue loading fails, the repo should still show milestone and issue rows plus a visible message that parent/sub-issue nesting is unavailable for that repo.
- If GitHub returns a relationship involving an issue outside the loaded open issue set, Anchor should not fabricate missing issue rows.
- Fork and ownership hiding remains separate from user exclusion.

## Testing And Proof Oracles

Automated proof candidates:

- Unit-test repo inclusion state:
  - newly detected repos default included
  - excluded repos stay excluded across reload
  - stale excluded repo keys do not break target resolution
- Unit-test milestone grouping:
  - every GitHub milestone appears
  - empty milestones render with `0 open`
  - `No Milestone` appears only when unassigned open issues exist
  - milestone ordering is stable
- Unit-test parent/sub-issue grouping:
  - parent issue rows contain sub-issue children
  - sub-issues are not duplicated as standalone rows
  - standalone issues still appear under their milestone
  - missing relationship targets do not fabricate nodes
- Unit-test header-independent model behavior where possible.
- Run `.\gradlew.bat test`.
- Run `.\gradlew.bat buildPlugin`.

Manual proof candidates:

- Open the current Workspace project in IntelliJ IDEA.
- Narrow the Anchor tool window enough to reproduce the original header crowding and confirm controls no longer overlap.
- Open the repo inclusion UI, deselect a repo, refresh, and confirm the repo is excluded.
- Confirm a GitHub milestone with zero open issues appears as a dropdown with `0 open`.
- Confirm a parent issue expands to show sub-issues.
- Confirm selecting parent, child, and standalone issues opens the rendered Anchor editor preview.

## Open Questions

- The implementation plan must verify the exact GitHub parent/sub-issue API route and version before coding.
- The implementation plan must decide whether repo inclusion state belongs in an IntelliJ project service or existing plugin settings based on current persistence patterns.
- The implementation plan must choose the exact Swing layout for the responsive header after testing it at the narrow tool-window width shown in the screenshot.

## Decision Ledger

| Decision | Source | Answer | Impact | Deferred? | Risk owner |
|---|---|---|---|---|---|
| Workflow mode | User native answer | Auto Mode. | Allows one bounded brainstorm route with validator-backed proof and route closeout. | No | Codex |
| Visual companion | User direct instruction | No visual companion. | Keeps the brainstorm text-only and avoids local companion UI. | No | User |
| Scope | User prompt | Fix top tool-window visual overlap and brainstorm/spec repo inclusion, empty milestones, and parent/sub-issue nesting. | Defines one focused M1 navigator refresh spec. | No | User |
| Superpowers workflow issue | User direct instruction | Created `tannerpolley/superpowers-project#105` for the native-input bypass. | Tracks the workflow instruction conflict outside this repo. | No | Codex |
| Milestone source | User native answer | Use GitHub milestone records. | Requires API-backed milestone loading and allows empty milestone dropdowns. | No | Implementation plan |
| Repo inclusion persistence | User native answer | Persist per IntelliJ project. | Keeps Workspace repo choices durable without global plugin surprise. | No | Implementation plan |
| Sub-issue source | User native answer | Use GitHub sub-issues API. | Avoids fuzzy body parsing and makes relationship failures explicit. | No | Implementation plan |
| Tree hierarchy | User native answer | `Repo > Milestone > Parent > Subissue`. | Keeps milestones as the primary grouping while nesting issue relationships underneath. | No | Implementation plan |
| Approach | User native answer | Design 1, focused navigator refresh. | Avoids a broad project-board redesign and keeps scope implementable. | No | User |
| Architecture | User native answer | Approved. | Confirms separation between header layout, repo inclusion, issue grouping, and editor preview. | No | User |
| Components | User native answer | Approved. | Confirms component boundaries for header, inclusion state, milestone loading, sub-issue loading, grouping, and renderer. | No | User |
| Data flow | User native answer | Approved; parent group rows open the parent issue on selection. | Avoids duplicate parent rows and keeps parent issues first-class. | No | User |
| Error handling | User native answer | Approved; explicit failures and no fuzzy inference. | Aligns with loud failure over fabricated data. | No | User |
| Testing | User native answer | Approved. | Sets automated and manual proof expectations for planning. | No | User |
| GitHub sub-issue endpoint | Repo evidence and API uncertainty | Verify exact endpoint and version in implementation planning. | Prevents implementing against an assumed API shape. | Yes | Implementation plan |
| Persistence location | Repo evidence | Decide exact project-service/settings storage during planning. | Keeps brainstorm from over-specifying implementation details before examining persistence APIs. | Yes | Implementation plan |
| Header layout implementation | Screenshot evidence | Pick the concrete Swing layout during implementation after reproducing the narrow width. | Keeps design outcome strict while allowing implementation to choose the smallest robust layout. | Yes | Implementation plan |
