# Fix issue tool-window header overlap and repo inclusion controls

**GitHub Issue:** https://github.com/tannerpolley/anchor/issues/2
**GitHub Milestone:** M1 - Issue Workflow Hardening
**Issue Type:** feature
**Source Spec:** docs/superpowers/specs/2026-06-29-tool-window-navigation-refresh-design.md
**Source Plan:** docs/superpowers/plans/2026-06-29-m1-tool-window-navigation-refresh-plan.md
**Classification:** AFK
**Labels:** status:ready, type:feature, area:issues, priority:p1
**Goal Command:** /goal Implement this issue from docs/superpowers/issues/2-fix-issue-tool-window-header-overlap-and-repo-inclusion-controls.md using source plan docs/superpowers/plans/2026-06-29-m1-tool-window-navigation-refresh-plan.md.
**Execution Mode:** Ask at runtime
**Worktree Policy:** Native Codex worktree thread first
**Integration Policy:** Worker PR reviewed by main thread
**TDD Policy:** Required
**Parallelization Plan:** None
**Reviewer Role:** Main thread orchestrator
**Script Gate Mode:** Safety only

## Outcome Summary

**Outcome Source:** docs/superpowers/plans/2026-06-29-m1-tool-window-navigation-refresh-plan.md#outcome-proof
**Intent:** The Anchor issue navigator header must not overlap at the screenshot width, and users must be able to include or exclude detected repositories per IntelliJ project.
**Target Output:** A Workspace-visible Anchor tool window with a stable header and a repo inclusion control whose selections persist for the current project.
**Owner:** Anchor issue tool-window layout and per-project repo inclusion state.
**Interface:** Tool-window header controls, repository inclusion dialog, and filtered repository target list.
**Cutover:** Replace the overlapping one-row header layout and route detected GitHub targets through project inclusion state before loading issues.
**Replaced Path:** Existing `RepoIssuesTreePanel.createHeader` one-row layout and always-included target list from `RemoteVcsIssuesPanel`.
**Acceptance Proof:** Focused inclusion tests, full Gradle tests, cleanup, and Workspace screenshot evidence at the original narrow width.
**Stop Criteria:** Stop before merge if IntelliJ project persistence ownership is unclear, if header controls still overlap at the screenshot width, or if excluded repositories continue loading after apply and refresh.
**Avoid:** Global repo preferences, hidden repo filtering rules, extra tool-window detail panes, broad pull request navigation changes, and inferred repository inclusion.

## Project Merge

**Merge Owner:** Main thread orchestrator
**Merge Gate:** Clean premerge proof required
**Merge Policy:** Repo default
**Worktree Cleanup Policy:** Remove owned worktree after merge
**Orchestrator Wakeup Policy:** Bounded Auto Mode heartbeat

## What To Build

Add a responsive non-overlapping header layout, a compact repo inclusion control, project-scoped inclusion persistence, and filtering before issue panels load repository data.

## Acceptance Criteria

- [ ] The header does not overlap title, status, sort controls, or buttons at the screenshot width.
- [ ] A repo inclusion control is visible in the Anchor issue tool-window header.
- [ ] The inclusion dialog lists detected GitHub repositories with selectable states.
- [ ] Newly detected repositories are included until the user deselects them.
- [ ] Deselecting a repository and refreshing removes it from the issue tree.
- [ ] The excluded repository state persists for the IntelliJ project across tool-window recreation.
- [ ] `gradlew.bat test` passes after the change.

## Blocked by

- None

## Non-goals

- GitHub milestone API loading.
- Parent/sub-issue relationship loading.
- Pull request repo inclusion controls.
- Editing GitHub repository metadata.

## Proof Oracle

- `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\validate-issue-mirror.ps1 -IssueFile docs\superpowers\issues\2-fix-issue-tool-window-header-overlap-and-repo-inclusion-controls.md`
- `.\gradlew.bat --no-daemon "-Dkotlin.compiler.execution.strategy=in-process" --console=plain test`
- `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File "$env:USERPROFILE\.codex\hooks\codex-cleanup.ps1" -RepoRoot .`
- Workspace screenshot showing no header overlap at the original narrow width.
