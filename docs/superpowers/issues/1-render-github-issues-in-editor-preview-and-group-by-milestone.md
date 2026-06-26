# Render GitHub issues in editor preview and group by milestone

**GitHub Issue:** https://github.com/tannerpolley/anchor/issues/1
**GitHub Milestone:** M1 - Issue Workflow Hardening
**Issue Type:** feature
**Source Spec:** docs/superpowers/specs/2026-06-26-issue-editor-preview-design.md
**Source Plan:** docs/superpowers/plans/2026-06-26-m1-issue-editor-preview-plan.md
**Classification:** AFK
**Labels:** status:ready, type:feature, area:issues, area:markdown, priority:p1
**Goal Command:** /goal Implement GitHub issue #1 from docs/superpowers/issues/1-render-github-issues-in-editor-preview-and-group-by-milestone.md using source plan docs/superpowers/plans/2026-06-26-m1-issue-editor-preview-plan.md.
**Execution Mode:** Ask at runtime
**Worktree Policy:** Native Codex worktree thread first
**Integration Policy:** Worker PR reviewed by main thread
**TDD Policy:** Required
**Parallelization Plan:** None
**Reviewer Role:** Main thread orchestrator
**Script Gate Mode:** Safety only

## Outcome Summary

**Outcome Source:** docs/superpowers/plans/2026-06-26-m1-issue-editor-preview-plan.md#outcome-proof
**Intent:** Users click issues in the Anchor tool window and read rendered GitHub Markdown in the main editor preview tab instead of inside a plugin split pane.
**Target Output:** A sandbox IDE shows an Anchor issue preview editor tab after selecting an issue, and selecting another issue reuses the preview tab.
**Owner:** Anchor UI/editor integration.
**Interface:** User selects issue nodes in the Anchor tool window; the IntelliJ editor area displays the issue preview.
**Cutover:** Remove the tool-window issue detail pane once the editor preview path owns issue display.
**Replaced Path:** SwingIssueDetailRenderer as a tool-window detail surface and the JSplitPane path in RepoIssuesTreePanel.
**Acceptance Proof:** Unit tests, gradlew.bat test, gradlew.bat verifyPlugin, and HITL sandbox IDE preview-tab verification.
**Stop Criteria:** Stop before merge if the configured IntelliJ Platform build cannot compile a preview-tab editor open path, if JCEF editor ownership cannot be disposed safely, if automated tests fail, or if HITL sandbox preview proof is missing.
**Avoid:** Disk temp files, Markdown-plugin dependency, duplicate issue detail surfaces, raw HTML shown as editor text, silent substitutions when GitHub rendering fails, and broad Compose tool-window refactors.

## Project Merge

**Merge Owner:** Main thread orchestrator
**Merge Gate:** Clean premerge proof required
**Merge Policy:** Repo default
**Worktree Cleanup Policy:** Remove owned worktree after merge
**Orchestrator Wakeup Policy:** Bounded Auto Mode heartbeat

## What To Build

Move rendered GitHub issue detail display from the Anchor tool window into a reusable main-editor preview tab, and group issues under each repository by milestone.

## Acceptance Criteria

- [ ] Selecting an issue opens a read-only Anchor issue preview in the main editor area.
- [ ] Selecting a different issue reuses one temporary preview tab.
- [ ] The preview shows issue metadata, labels, rendered body, and comments.
- [ ] Issues are grouped under each repository by milestone title, with No Milestone for unassigned issues.
- [ ] Repository nodes open by default; milestone nodes are collapsed except the first active milestone.
- [ ] The old plugin-pane issue detail surface is removed.
- [ ] Detail load, render, and JCEF creation failures produce explicit preview error content.
- [ ] gradlew.bat test and gradlew.bat verifyPlugin pass.

## Blocked by

- None

## Non-goals

- Editing GitHub issue bodies from the editor preview.
- Pull request editor previews.
- Full milestone due-date or description summaries.
- Creating additional GitHub tracker labels, milestones, or issues beyond this slice.
- Reworking the old Compose tool-window path.

## Proof Oracle

- pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\validate-plan-outcome-proof.ps1 -PlanPath docs\superpowers\plans\2026-06-26-m1-issue-editor-preview-plan.md
- pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\validate-decision-ledger.ps1 -Path docs\superpowers\plans\2026-06-26-m1-issue-editor-preview-plan.md -Kind plan
- pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\validate-plan-task-use-cases.ps1 -PlanPath docs\superpowers\plans\2026-06-26-m1-issue-editor-preview-plan.md
- .\gradlew.bat test
- .\gradlew.bat verifyPlugin
- HITL sandbox IDE preview-tab verification before merge.
