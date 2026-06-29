# Group issue tree by milestones, parent issues, and sub-issues

**GitHub Issue:** https://github.com/tannerpolley/anchor/issues/4
**GitHub Milestone:** M1 - Issue Workflow Hardening
**Issue Type:** feature
**Source Spec:** docs/superpowers/specs/2026-06-29-tool-window-navigation-refresh-design.md
**Source Plan:** docs/superpowers/plans/2026-06-29-m1-tool-window-navigation-refresh-plan.md
**Classification:** AFK
**Labels:** status:ready, type:feature, area:issues, priority:p1
**Goal Command:** /goal Implement this issue from docs/superpowers/issues/4-group-issue-tree-by-milestones-parent-issues-and-sub-issues.md using source plan docs/superpowers/plans/2026-06-29-m1-tool-window-navigation-refresh-plan.md.
**Execution Mode:** Ask at runtime
**Worktree Policy:** Native Codex worktree thread first
**Integration Policy:** Worker PR reviewed by main thread
**TDD Policy:** Required
**Parallelization Plan:** None
**Reviewer Role:** Main thread orchestrator
**Script Gate Mode:** Safety only

## Outcome Summary

**Outcome Source:** docs/superpowers/plans/2026-06-29-m1-tool-window-navigation-refresh-plan.md#outcome-proof
**Intent:** The issue tree needs a pure grouping model that includes every GitHub milestone and nests sub-issues under parent issue rows.
**Target Output:** Tested grouping data for `Repo > Milestone > Parent Issue > Subissue` rendering.
**Owner:** Anchor issue tree grouping model.
**Interface:** `IssueTreeGrouping` consumed by `RepoIssuesTreePanel` and `RepoIssuesTreeRenderer`.
**Cutover:** Replace issue-derived milestone grouping with a model built from milestone records, open issues, and issue relationships.
**Replaced Path:** `IssueMilestoneGrouping.group(issues)` as the only tree grouping source.
**Acceptance Proof:** Grouping tests for empty milestones, `No Milestone`, parent rows, sub-issue nesting, de-duplication, full Gradle tests, and cleanup.
**Stop Criteria:** Stop before UI rendering if the grouping model duplicates child issues, hides standalone issues, or creates `No Milestone` when no unassigned open issues exist.
**Avoid:** UI-specific grouping logic, body parsing, duplicate issue rows, and milestone groups derived only from currently open issues.

## Project Merge

**Merge Owner:** Main thread orchestrator
**Merge Gate:** Clean premerge proof required
**Merge Policy:** Repo default
**Worktree Cleanup Policy:** Remove owned worktree after merge
**Orchestrator Wakeup Policy:** Bounded Auto Mode heartbeat

## What To Build

Create a tested `IssueTreeGrouping` model that groups open issues into milestone groups using GitHub milestone records first, nests child issues under parent rows, and keeps standalone issue rows visible.

## Acceptance Criteria

- [ ] Milestones with zero open issues appear as groups with `0 open`.
- [ ] `No Milestone` appears only when unassigned open issues exist.
- [ ] Parent issue rows include their sub-issue children.
- [ ] Sub-issues do not also appear as standalone rows in the same milestone group.
- [ ] Standalone issues remain visible and sorted.
- [ ] Production callers no longer depend on issue-derived-only grouping.
- [ ] `gradlew.bat test` passes after the change.

## Blocked by

- https://github.com/tannerpolley/anchor/issues/3

## Non-goals

- GitHub API route verification.
- Header layout changes.
- Repo inclusion persistence.
- JTree rendering changes beyond model contracts.

## Proof Oracle

- `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\validate-issue-mirror.ps1 -IssueFile docs\superpowers\issues\4-group-issue-tree-by-milestones-parent-issues-and-sub-issues.md`
- `.\gradlew.bat --no-daemon "-Dkotlin.compiler.execution.strategy=in-process" --console=plain test --tests "com.itsjeel01.remotevcsmanager.ui.IssueTreeGroupingTest"`
- `.\gradlew.bat --no-daemon "-Dkotlin.compiler.execution.strategy=in-process" --console=plain test`
- `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File "$env:USERPROFILE\.codex\hooks\codex-cleanup.ps1" -RepoRoot .`
