# Load GitHub milestones and issue relationships

**GitHub Issue:** https://github.com/tannerpolley/anchor/issues/3
**GitHub Milestone:** M1 - Issue Workflow Hardening
**Issue Type:** feature
**Source Spec:** docs/superpowers/specs/2026-06-29-tool-window-navigation-refresh-design.md
**Source Plan:** docs/superpowers/plans/2026-06-29-m1-tool-window-navigation-refresh-plan.md
**Classification:** AFK
**Labels:** status:ready, type:feature, area:issues, priority:p1
**Goal Command:** /goal Implement this issue from docs/superpowers/issues/3-load-github-milestones-and-issue-relationships.md using source plan docs/superpowers/plans/2026-06-29-m1-tool-window-navigation-refresh-plan.md.
**Execution Mode:** Ask at runtime
**Worktree Policy:** Native Codex worktree thread first
**Integration Policy:** Worker PR reviewed by main thread
**TDD Policy:** Required
**Parallelization Plan:** None
**Reviewer Role:** Main thread orchestrator
**Script Gate Mode:** Safety only

## Outcome Summary

**Outcome Source:** docs/superpowers/plans/2026-06-29-m1-tool-window-navigation-refresh-plan.md#outcome-proof
**Intent:** Anchor needs GitHub milestone records and parent/sub-issue relationships before the UI can show empty milestone dropdowns and nested issue rows.
**Target Output:** Provider-level API methods return parsed milestone and relationship models for GitHub repositories.
**Owner:** Anchor GitHub provider and API client.
**Interface:** `RemoteVcsProvider`, `GitHubProvider`, `GitHubApiClient`, `IssueMilestone`, and `IssueRelationship`.
**Cutover:** Add required provider methods and implement them for GitHub with verified GitHub API routes.
**Replaced Path:** Issue-tree data based only on open issue records and nullable issue milestone names.
**Acceptance Proof:** Provider parsing tests, verified API route notes, full Gradle tests, and cleanup.
**Stop Criteria:** Stop before UI wiring if the exact GitHub parent/sub-issue API route or schema cannot be verified.
**Avoid:** Body parsing for relationships, provider-level empty data defaults, inferred parent rows, and UI assumptions before API verification.

## Project Merge

**Merge Owner:** Main thread orchestrator
**Merge Gate:** Clean premerge proof required
**Merge Policy:** Repo default
**Worktree Cleanup Policy:** Remove owned worktree after merge
**Orchestrator Wakeup Policy:** Bounded Auto Mode heartbeat

## What To Build

Create `IssueMilestone` and `IssueRelationship` models, add required provider methods for milestones and issue relationships, verify the GitHub API routes, and implement GitHub-backed parsing and retrieval.

## Acceptance Criteria

- [ ] The GitHub milestone route is verified from official docs or a read-only API probe.
- [ ] The GitHub parent/sub-issue relationship route is verified from official docs or a read-only API probe.
- [ ] `IssueMilestone` captures id, title, open issue count, and state.
- [ ] `IssueRelationship` captures parent issue number and child issue number.
- [ ] `RemoteVcsProvider` requires milestone and relationship methods with no empty data default.
- [ ] `GitHubProvider` implements both methods using GitHub-backed data.
- [ ] Provider tests cover representative milestone and relationship payloads.
- [ ] `gradlew.bat test` passes after the change.

## Blocked by

- None

## Non-goals

- Rendering tree rows.
- Repo inclusion UI.
- Editing GitHub milestones or issue relationships.
- Pull request hierarchy support.

## Proof Oracle

- `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\validate-issue-mirror.ps1 -IssueFile docs\superpowers\issues\3-load-github-milestones-and-issue-relationships.md`
- `.\gradlew.bat --no-daemon "-Dkotlin.compiler.execution.strategy=in-process" --console=plain test --tests "com.itsjeel01.remotevcsmanager.providers.github.GitHubProviderIssueStructureTest"`
- `.\gradlew.bat --no-daemon "-Dkotlin.compiler.execution.strategy=in-process" --console=plain test`
- `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File "$env:USERPROFILE\.codex\hooks\codex-cleanup.ps1" -RepoRoot .`
