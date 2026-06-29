# Wire complete issue tree rendering and Workspace proof

**GitHub Issue:** https://github.com/tannerpolley/anchor/issues/5
**GitHub Milestone:** M1 - Issue Workflow Hardening
**Issue Type:** feature
**Source Spec:** docs/superpowers/specs/2026-06-29-tool-window-navigation-refresh-design.md
**Source Plan:** docs/superpowers/plans/2026-06-29-m1-tool-window-navigation-refresh-plan.md
**Classification:** AFK
**Labels:** status:ready, type:feature, area:issues, priority:p1
**Goal Command:** /goal Implement this issue from docs/superpowers/issues/5-wire-complete-issue-tree-rendering-and-workspace-proof.md using source plan docs/superpowers/plans/2026-06-29-m1-tool-window-navigation-refresh-plan.md.
**Execution Mode:** Ask at runtime
**Worktree Policy:** Native Codex worktree thread first
**Integration Policy:** Worker PR reviewed by main thread
**TDD Policy:** Required
**Parallelization Plan:** None
**Reviewer Role:** Main thread orchestrator
**Script Gate Mode:** Safety only

## Outcome Summary

**Outcome Source:** docs/superpowers/plans/2026-06-29-m1-tool-window-navigation-refresh-plan.md#outcome-proof
**Intent:** The Anchor issue navigator must load the new data, render complete milestone and parent/sub-issue rows, and prove the result in the current Workspace IDE.
**Target Output:** Installed Workspace plugin proof showing a non-overlapping header, repo inclusion control, empty milestone row, nested parent/sub-issue rows, and rendered editor preview selection.
**Owner:** Anchor issue tree panel, renderer, selection behavior, and Workspace verification path.
**Interface:** `RepoIssuesTreePanel`, `RepoIssuesTreeRenderer`, existing Anchor issue editor preview opener, Gradle plugin build, and current Workspace IDE.
**Cutover:** Wire provider milestone and relationship data into the tree model, then verify the installed plugin in the existing Workspace window.
**Replaced Path:** Tree rendering based only on repo/milestone/message/issue nodes without parent/sub-issue row semantics.
**Acceptance Proof:** Targeted tests where practical, full Gradle tests, `buildPlugin`, cleanup, IDE log check, and screenshot/computer-use proof in the current Workspace window.
**Stop Criteria:** Stop before merge if plugin installation opens a new project window, if the current Workspace window cannot be verified, if the screenshot still shows overlap, or if editor previews fail for parent, child, or standalone issue rows.
**Avoid:** Opening a new IntelliJ project window, restoring a tool-window detail preview, changing pull request navigation, generated build output commits, and silent UI proof gaps.

## Project Merge

**Merge Owner:** Main thread orchestrator
**Merge Gate:** Clean premerge proof required
**Merge Policy:** Repo default
**Worktree Cleanup Policy:** Remove owned worktree after merge
**Orchestrator Wakeup Policy:** Bounded Auto Mode heartbeat

## What To Build

Load milestones, issues, and relationships for each included repo; render repositories, milestones, parent issues, sub-issues, standalone issues, and messages; preserve editor-preview selection; package and prove the plugin inside the current Workspace IDE.

## Acceptance Criteria

- [ ] Included repositories load milestones, open issues, and relationship data during refresh.
- [ ] Empty milestone rows render as dropdowns with `0 open`.
- [ ] Parent issue rows open the parent issue preview when selected.
- [ ] Sub-issue rows open their own rendered issue preview when selected.
- [ ] Standalone issue rows still open the rendered issue preview.
- [ ] Header and tree rows do not overlap at the screenshot width.
- [ ] `gradlew.bat test` and `gradlew.bat buildPlugin` pass.
- [ ] The built plugin is installed into the current Workspace IDE without opening a new project window.
- [ ] Screenshot/computer-use proof shows the requested final tool-window state.

## Blocked by

- https://github.com/tannerpolley/anchor/issues/2
- https://github.com/tannerpolley/anchor/issues/4

## Non-goals

- GitHub Project board management.
- Marketplace publishing.
- Pull request tree hierarchy.
- Editing issue metadata from Anchor.

## Proof Oracle

- `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\validate-issue-mirror.ps1 -IssueFile docs\superpowers\issues\5-wire-complete-issue-tree-rendering-and-workspace-proof.md`
- `.\gradlew.bat --no-daemon "-Dkotlin.compiler.execution.strategy=in-process" --console=plain test`
- `.\gradlew.bat --no-daemon "-Dkotlin.compiler.execution.strategy=in-process" --console=plain buildPlugin`
- `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File "$env:USERPROFILE\.codex\hooks\codex-cleanup.ps1" -RepoRoot .`
- Screenshot/computer-use proof from the existing Workspace IntelliJ window.
