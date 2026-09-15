# Show native issue blockers and refresh Anchor reliably

**GitHub Issue:** https://github.com/tannerpolley/anchor/issues/7
**GitHub Milestone:** M1 - Issue Workflow Hardening
**Issue Type:** unassigned
**Source Spec:** docs/superpowers/specs/2026-09-15-native-issue-dependency-refresh-design.md
**Source Plan:** docs/superpowers/plans/2026-09-15-m1-native-issue-dependency-refresh-plan.md
**Classification:** AFK implementation; HITL installed-plugin proof
**Labels:** status:ready, type:feature, area:issues, priority:p1
**Goal Command:** /goal Implement GitHub issue #7 from its source plan and prove native blockers plus reliable refresh.
**Execution Mode:** Direct implementation in the saved attached checkout
**Worktree Policy:** Use the saved attached checkout; no additional worktree
**Integration Policy:** Focused local commit; no push, pull request, or merge without separate authority
**TDD Policy:** Focused tests required for dependency mapping/display and tree-state restoration
**Parallelization Plan:** None; the active refresh path and provider mapping form one bounded change
**Reviewer Role:** Main thread final diff review
**Script Gate Mode:** Safety and artifact validity

## Outcome Summary

**Outcome Source:** docs/superpowers/plans/2026-09-15-m1-native-issue-dependency-refresh-plan.md#outcome-proof
**Intent:** Keep agent-managed GitHub dependency and issue updates visible and current inside Anchor.
**Target Output:** A compatible Anchor plugin ZIP showing native blockers and preserving usable navigator/preview state through refresh.
**Owner:** Anchor issue provider, Swing navigator, and editor-preview integration.
**Interface:** Users view blocker rows and receive bounded automatic or manual refresh in the existing Anchor tool window.
**Cutover:** Replace destructive refresh and cached mutable comments in the active Swing path.
**Replaced Path:** Loading-placeholder replacement before data exists and indefinite issue-comment cache reads.
**Acceptance Proof:** Automated tests, plugin verification/build, artifact validators, and installed-plugin interaction proof.
**Stop Criteria:** Stop before delivery for failed gates, data loss on refresh failure, duplicate preview tabs, or missing HITL proof.
**Avoid:** Dependency mutation, readiness inference, webhooks, GraphQL batching, new libraries, Compose-path work, and pull-request scope.

## Project Merge

**Merge Owner:** User or separately authorized delivery task
**Merge Gate:** Clean diff, passing automated gates, and installed-plugin proof
**Merge Policy:** Repository default through a reviewed pull request
**Worktree Cleanup Policy:** Preserve the saved attached checkout
**Orchestrator Wakeup Policy:** None; this is a direct implementation task

## What To Build

Read and render native GitHub blockers, replace destructive issue refresh with transactional state-preserving refresh, and revalidate the selected issue preview while Anchor is visible.

## Acceptance Criteria

- [ ] Open and closed blockers remain visible with their current state.
- [ ] Closing or reopening a blocker converges after refresh without deleting the relationship.
- [ ] Refresh preserves expansion and selection.
- [ ] Failed refresh retains prior data and clearly marks it stale.
- [ ] Hidden Anchor windows do not poll periodically.
- [ ] External issue edits and comments update in the existing preview tab.
- [ ] Full tests, plugin verification, plugin build, and artifact validators pass.
- [ ] Installed-plugin interaction proof passes before merge.

## Blocked by

- None

## Non-goals

- Editing dependency relationships from Anchor.
- Inferring readiness, authorization, worker ownership, or automatic execution.
- Adding webhooks, a GitHub App, GraphQL batching, or new dependencies.
- Changing pull-request behavior or the unused Compose tool-window path.

## Proof Oracle

- `./scripts/validate-plan-outcome-proof.sh --plan-path docs/superpowers/plans/2026-09-15-m1-native-issue-dependency-refresh-plan.md`
- `./scripts/validate-decision-ledger.sh --path docs/superpowers/plans/2026-09-15-m1-native-issue-dependency-refresh-plan.md --kind plan`
- `./scripts/validate-plan-task-use-cases.sh --plan-path docs/superpowers/plans/2026-09-15-m1-native-issue-dependency-refresh-plan.md`
- `./scripts/validate-issue-mirror.sh --issue-file docs/superpowers/issues/7-show-native-issue-blockers-and-refresh-anchor-reliably.md`
- `./gradlew --no-daemon test`
- `./gradlew --no-daemon verifyPlugin`
- `./gradlew --no-daemon buildPlugin`
- `git diff --check`
- HITL installed-plugin exercise with two blockers, close/reopen transitions, an external comment, IDE reactivation, and a failed refresh.

## Decision Ledger

| Decision | Source | Answer | Impact | Deferred? | Risk owner |
|---|---|---|---|---|---|
| Scope | User-approved plan | Read and display dependencies plus reliable refresh. | Keeps implementation bounded to issue visibility. | No | User |
| Authority | GitHub native state | Do not infer or mutate readiness. | Anchor remains a projection rather than a reconciler. | No | User |
| Delivery | Root authority boundary | Local commit only until publication is separately requested. | Preserves push and PR authority. | No | User |
| Completion | Project guidance | Require automated gates and installed-plugin proof. | Prevents build-only completion claims. | No | User |
