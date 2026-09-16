---
title: Native Issue Dependencies and Reliable Refresh
milestone: M1 - Issue Workflow Hardening
status: approved-design
owner: codex
date: 2026-09-15
---

# Native Issue Dependencies and Reliable Refresh

## Problem

Anchor renders milestones and parent/sub-issue relationships, but it does not show native GitHub
blocked-by relationships. Its active Swing navigator also replaces the current tree during every
refresh, loses selection and expansion state, and caches issue comments without expiry. Updates made
by coding agents outside the IDE can therefore be missing or disruptive when they eventually appear.

## Goals

- Show each open issue's native blockers, including closed blockers and their current state.
- Keep GitHub relationships authoritative; do not infer readiness from the open-issue list.
- Refresh on initial load, explicit request, visibility, IDE activation, and a bounded visible timer.
- Preserve the last successful tree, expanded nodes, and selected issue across refreshes.
- Refresh the selected issue body and comments without opening another editor tab.
- Display last-successful and failed/stale refresh states clearly.

## Non-Goals

- Creating, editing, or deleting GitHub dependencies from Anchor.
- Computing authorization, readiness, worker ownership, or automatic execution.
- Adding a webhook, GitHub App, reconciler, GraphQL client, or new dependency.
- Changing the unused Compose tool-window path or pull-request behavior.

## Design

`GitHubApiClient` reads the existing REST issue endpoint and the native `blocked_by` endpoint.
`GitHubProvider` maps each blocker into the existing `Issue` model and associates it with the blocked
issue through `IssueDependency`. Closed blockers remain in the returned relationship list.

`RepoIssuesTreePanel` remains the single owner of navigator refresh. A refresh keeps the current tree
visible, fetches a complete replacement in the background, and swaps it in only on success. Stable
milestone titles and issue numbers restore expansion and selection. A failed replacement retains the
last successful data and marks it stale.

Dependency rows are informational children of their blocked issue. They show state, repository,
number, and title, but are not treated as sub-issues or executable work.

`IssueEditorPreviewOpener` keeps editor navigation separate from revalidation. Selecting an issue opens
the preview; a navigator refresh updates the payload at the same virtual-file path without navigating.
Mutable comment responses bypass the generic API cache. Content-addressed Markdown caching remains.

Automatic refresh uses the existing preference, Swing timer, component visibility, and IntelliJ
application activation event. It polls every five minutes only while visible and throttles focus/show
events to one attempt per minute. Manual refresh is always available.

## Acceptance Criteria

- An issue with open and closed blockers shows every blocker with the correct state.
- Reopening a blocker changes its displayed state after refresh; the relationship is not removed.
- Refresh preserves expanded nodes and the selected issue.
- A failed refresh leaves previous data visible and reports it as stale.
- Returning to the IDE refreshes a visible Anchor window when automatic refresh is enabled.
- Hidden Anchor windows do not perform periodic refreshes.
- An externally edited issue or new comment updates in the existing preview tab.

## Proof

- Focused parsing, dependency-display, and tree-state tests.
- Full Gradle test suite, plugin verifier, and plugin build.
- Installed-plugin exercise with two blockers, close/reopen transitions, an external comment, and a
  failed refresh.

## Decision Ledger

| Decision | Source | Answer | Impact | Deferred? | Risk owner |
|---|---|---|---|---|---|
| Authority | User-approved recommendation | GitHub native relationships are authoritative. | Anchor reads and displays rather than inferring readiness. | No | User |
| UI owner | Registered tool-window source | Change the active Swing navigator only. | Avoids duplicate work in the unused Compose path. | No | Implementer |
| Refresh policy | User-approved plan | Refresh only while visible, on activation, or manually. | Keeps data current without hidden polling. | No | Implementer |
| Comment cache | Repository evidence | Do not cache mutable issue comments indefinitely. | External comments appear on revalidation. | No | Implementer |
| Batching | Ponytail scope | Start with existing REST patterns. | Avoids GraphQL and new infrastructure. | Yes | Maintainer |
