# Native Issue Dependencies and Reliable Refresh Plan

**Goal:** Show GitHub native blockers and make issue refresh converge without discarding usable navigator state.

**Architecture:** The existing REST provider reads blockers into the existing issue model. The active Swing navigator owns one transactional refresh path, and the existing preview store receives detail revalidation without editor navigation.

**Tech Stack:** Kotlin, IntelliJ Platform OpenAPI, Swing, OkHttp, Gson, kotlin.test/JUnit5.

## Source

- Design: `docs/superpowers/specs/2026-09-15-native-issue-dependency-refresh-design.md`
- Issue: https://github.com/tannerpolley/anchor/issues/7
- Mirror: `docs/superpowers/issues/7-show-native-issue-blockers-and-refresh-anchor-reliably.md`
- Milestone: `M1 - Issue Workflow Hardening`
- Branch: `codex/native-dependency-refresh`

## Outcome Proof

- **Intent:** Keep agent-managed GitHub dependency and issue updates visible and current inside Anchor.
- **Current Behavior:** Anchor shows sub-issues but not native blockers, clears the tree during refresh, loses navigation state, and can reuse cached comments indefinitely.
- **Expected Outcome:** Blockers remain visible with current state; successful refresh preserves navigation; failed refresh retains stale data; the selected preview revalidates without another tab.
- **Target Output:** A compatible Anchor plugin ZIP whose issue tree and editor preview converge after external GitHub changes.
- **Owner:** `RepoIssuesTreePanel` owns navigator refresh; `IssueEditorPreviewOpener` owns preview revalidation; `GitHubProvider` owns GitHub payload mapping.
- **Interface:** Users view blocker rows below issues and receive automatic or manual refresh through the existing Anchor tool window.
- **Cutover:** Replace destructive tree reload and indefinite comment caching in the active Swing path.
- **Replaced Path:** Loading-placeholder replacement on every refresh and cached mutable comment responses.
- **Evidence:** Focused dependency parsing/display and tree-state tests, full tests, plugin verification, plugin build, and installed-plugin interaction proof.
- **Acceptance Proof:** All automated gates pass and the installed plugin demonstrates dependency state changes, stale-data retention, preserved navigation, and one refreshed preview tab.
- **Stop Criteria:** Stop before delivery for failing automated gates, lost last-successful data, duplicate preview tabs, or missing installed-plugin proof.
- **Avoid:** Dependency mutation, readiness inference, webhooks, a GitHub App, GraphQL batching, new libraries, Compose-path work, and pull-request scope.
- **Risk:** Per-issue REST reads can become slow on unusually large open-issue sets; retain the simple path until measurement justifies batching.

## Implementation Boundaries

- **Files To Create:** `RepoIssueTreeState.kt`, focused tests, this plan, the source spec, and issue mirror 7.
- **Files To Modify:** GitHub issue API/provider files, Swing issue tree item/panel/renderer files, `IssueEditorPreviewOpener.kt`, the auto-refresh setting label, and M1 index.
- **Files To Avoid:** `RemoteVcsToolWindowContent.kt`, `ToolWindowState.kt`, pull-request files, plugin dependencies, authentication storage, and GitHub write endpoints.
- **Source Of Truth:** GitHub native relationships and the approved source spec.
- **Read Path:** `GitHubApiClient` reads open issues, sub-issues, blockers, direct issue detail, and comments; `GitHubProvider` maps them.
- **Write Path:** Local Kotlin/tests/docs plus preview-store and Swing tree state; no GitHub dependency writes.
- **Integration Points:** GitHub REST, Swing tree model, IntelliJ application activation, persistent auto-refresh setting, and Anchor virtual-file preview store.
- **Migration Or Cutover:** Add blocker reads/rendering, replace refresh semantics, then enable bounded lifecycle triggers and preview revalidation.
- **Replaced Path Handling:** Remove comment-cache use and stop clearing valid tree/selection state before replacement data exists.
- **Acceptance Proof Gate:** Full tests, `verifyPlugin`, `buildPlugin`, `git diff --check`, artifact validators, and HITL installed-plugin exercise.

## Test Complete And Metrics

- Zero failing Gradle tests.
- Plugin Verifier reports compatible with IntelliJ IDEA 2025.3.6.
- Plugin ZIP builds successfully.
- Focused tests retain closed blockers, qualify cross-repository references, and restore tree state.
- Installed proof confirms visible-only refresh, close/reopen convergence, stale-data retention, and one preview tab.

### Task 1: Read Native Dependencies

**Use Cases:**
- Load every native blocker for an open issue, including closed blockers.
- Refresh a selected issue directly after it leaves the open-issue list.
- Always fetch mutable comments from GitHub.

**Files:**
- Modify: `GitHubApiClient.kt`, `GitHubProvider.kt`, `GitHubIssueStructureParser.kt`, `IssueRelationship.kt`.
- Modify: `GitHubProviderIssueStructureTest.kt`.

Add paginated `blocked_by` and direct issue requests. Map blocker payloads through the existing `Issue` parser into `IssueDependency`, and remove comment-cache reads/writes.

### Task 2: Render Dependency Rows

**Use Cases:**
- Show blocker state and identity beneath parent, sub-issue, and standalone issue rows.
- Disambiguate blockers from another repository.
- Keep dependency rows informational rather than executable or readiness-bearing.

**Files:**
- Modify: `RepoIssueTreeItem.kt`, `RepoIssuesTreeRenderer.kt`, `RepoIssuesTreePanel.kt`.
- Create: `IssueDependencyDisplayTest.kt`.

Render state, repository-qualified issue number, title, and URL with IDE theme attributes.

### Task 3: Replace Destructive Refresh

**Use Cases:**
- Keep the current tree visible while replacement data loads.
- Restore expanded milestones/issues and the selected issue after success.
- Retain previous data and mark it stale after failure.

**Files:**
- Modify: `RepoIssuesTreePanel.kt`.
- Create: `RepoIssueTreeState.kt`, `RepoIssueTreeStateTest.kt`.

Capture stable keys, swap only complete successful results, restore state, and expose updated/refreshing/failed status.

### Task 4: Revalidate While Visible

**Use Cases:**
- Refresh when Anchor becomes visible or the IDE regains focus.
- Poll every five minutes only while visible and enabled.
- Update the selected preview without opening another editor tab.

**Files:**
- Modify: `RepoIssuesTreePanel.kt`, `IssueEditorPreviewOpener.kt`, `RemoteVcsSettingsPanel.kt`.

Reuse the existing setting, Swing timer, component visibility, application activation event, preview store, and request generation guards.

## Proof Oracle

```bash
./scripts/validate-plan-outcome-proof.sh --plan-path docs/superpowers/plans/2026-09-15-m1-native-issue-dependency-refresh-plan.md
./scripts/validate-decision-ledger.sh --path docs/superpowers/plans/2026-09-15-m1-native-issue-dependency-refresh-plan.md --kind plan
./scripts/validate-plan-task-use-cases.sh --plan-path docs/superpowers/plans/2026-09-15-m1-native-issue-dependency-refresh-plan.md
./scripts/validate-issue-mirror.sh --issue-file docs/superpowers/issues/7-show-native-issue-blockers-and-refresh-anchor-reliably.md
./gradlew --no-daemon test
./gradlew --no-daemon verifyPlugin
./gradlew --no-daemon buildPlugin
git diff --check
```

## Decision Ledger

| Decision | Source | Answer | Impact | Deferred? | Risk owner |
|---|---|---|---|---|---|
| Authority | User-approved plan | GitHub native relationships remain authoritative. | Anchor only reads and displays dependency state. | No | User |
| Provider seam | Repository evidence | Keep blocker reads on `GitHubProvider`, already used directly by the active panel. | Avoids a speculative cross-provider interface. | No | Implementer |
| Refresh owner | Repository evidence | Keep one transactional path in `RepoIssuesTreePanel`. | Prevents duplicated lifecycle and stale-state rules. | No | Implementer |
| Polling | Approved plan | Five minutes while visible, one-minute lifecycle throttle. | Bounds API traffic without new configuration. | No | Implementer |
| Large repositories | Ponytail scope | Keep sequential REST reads until measured latency or rate usage fails. | Avoids premature GraphQL batching. | Yes | Maintainer |
| Completion | Project guidance | Automated gates plus installed-plugin proof. | Separates code compatibility from user-visible behavior. | No | User |
