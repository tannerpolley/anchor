# Remote VCS Manager — JetBrains Plugin
### Full Development Plan · GitHub-first · Scalable to GitLab, ADO, etc.

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Tech Stack & Tooling](#2-tech-stack--tooling)
3. [Architecture Design](#3-architecture-design)
4. [Phase 1 — Project Scaffolding](#4-phase-1--project-scaffolding)
5. [Phase 2 — Auth Layer](#5-phase-2--auth-layer)
6. [Phase 3 — Provider Abstraction Layer](#6-phase-3--provider-abstraction-layer)
7. [Phase 4 — Issues Module](#7-phase-4--issues-module)
8. [Phase 5 — Pull Requests Module](#8-phase-5--pull-requests-module)
9. [Phase 6 — Branches Module](#9-phase-6--branches-module)
10. [Phase 7 — Comments Module](#10-phase-7--comments-module)
11. [Phase 8 — UI/UX System](#11-phase-8--uiux-system)
12. [Phase 9 — Cross-Cutting Concerns](#12-phase-9--cross-cutting-concerns)
13. [Phase 10 — Testing Iterations](#13-phase-10--testing-iterations)
14. [Phase 11 — Deployment Pipeline](#14-phase-11--deployment-pipeline)
15. [Requirements Traceability Matrix](#15-requirements-traceability-matrix)
16. [Risks & Mitigations](#16-risks--mitigations)

---

## 1. Project Overview

### Plugin Identity

| Field | Value |
|---|---|
| Plugin Name | RemoteVCS (working title) |
| Primary IDE | Android Studio (IntelliJ-based) |
| Target IDEs | All IntelliJ Platform IDEs |
| Primary Provider | GitHub |
| Scalable to | GitLab, Azure DevOps, Bitbucket |
| Distribution | JetBrains Marketplace (free) |
| Language | Kotlin |

### Core Capability Summary

A GUI-driven remote VCS management panel embedded natively inside any JetBrains IDE. Replaces the need for `gh` CLI, browser tab switching, or third-party desktop apps. Users can manage their entire GitHub workflow — issues, PRs, branches, and comments — without leaving the IDE.

### Requirements Recap

| ID | Requirement | Priority |
|---|---|---|
| R1 | Seamless OAuth / PAT authentication | Must Have |
| R2 | Native JetBrains look and feel, theme-aware | Must Have |
| R3 | New UI Islands support | Must Have |
| R4 | Two-way issue management | Must Have |
| R5 | Two-way PR management | Must Have |
| R6 | Branch management linked to issues/PRs | Must Have |
| R7 | Comment support on issues and PRs | Should Have |
| R8 | Modern, user-friendly UI | Must Have |
| R9 | Scalable to multiple VCS providers | Should Have |
| R10 | Works across all JetBrains IDEs | Must Have |

---

## 2. Tech Stack & Tooling

### Build & Plugin Framework

- **Gradle** with Kotlin DSL (`build.gradle.kts`)
- **IntelliJ Platform Gradle Plugin v2** (`org.jetbrains.intellij.platform`) — the current-gen replacement for the legacy plugin
- **`plugin.xml`** for extension point declarations, IDE compatibility ranges
- **Kotlin 1.9+** — full coroutine support, stable APIs

### UI Framework

- **IntelliJ Platform UI (Swing-based)** for legacy compatibility
- **Jewel** (JetBrains' new Compose for Desktop-based UI library) — for Islands-compatible, theme-aware modern UI panels
- Decision checkpoint: Jewel is relatively new; fallback to Swing + FlatLaf-compatible approach if Jewel stability is a concern at time of implementation

### Networking

- **Ktor Client** (Kotlin-native HTTP client) — for GitHub REST/GraphQL API calls
- **Kotlinx Serialization** — JSON parsing
- **GitHub REST API v3** (initial) + **GraphQL API v4** where query efficiency matters (e.g., linked issues ↔ PRs)

### Storage & State

- **IntelliJ `CredentialAttributes` + `PasswordSafe`** — secure credential storage (OS keychain backed)
- **IntelliJ `PersistentStateComponent`** — plugin settings persistence across IDE restarts
- **Kotlin `StateFlow` / coroutine scopes** — reactive in-memory state

### Testing

- **JUnit 5** — unit tests
- **MockK** — Kotlin-friendly mocking
- **IntelliJ Platform Test Framework** — UI and integration tests in a headless IDE instance
- **WireMock** — mock GitHub API responses for integration tests

---

## 3. Architecture Design

### High-Level Layering

```
┌──────────────────────────────────────────────────┐
│                  UI Layer (Jewel / Swing)          │
│   Tool Windows · Panels · Dialogs · Popups        │
├──────────────────────────────────────────────────┤
│              ViewModel / Presenter Layer           │
│   State holders · UI logic · Coroutine scopes     │
├──────────────────────────────────────────────────┤
│             Domain / Use Case Layer                │
│   IssueService · PRService · BranchService        │
├──────────────────────────────────────────────────┤
│          Provider Abstraction Layer (PAL)          │
│   VcsProvider interface · GitHub impl             │
│   GitLab stub · ADO stub                          │
├──────────────────────────────────────────────────┤
│               Infrastructure Layer                 │
│   Ktor HTTP Client · Auth Manager · Cache         │
└──────────────────────────────────────────────────┘
```

### Extension Points Used

| Extension Point | Purpose |
|---|---|
| `toolWindow` | Main plugin panel registration |
| `applicationService` | Singleton auth manager, settings service |
| `projectService` | Per-project state (active repo, open items) |
| `notificationGroup` | IDE-native notifications |
| `statusBarWidget` | Optional status bar integration (connection status) |
| `vcs.changes.changesViewToolWindowManager` | Hook into existing VCS panel for branch awareness |

### Provider Interface Contract (PAL)

The Provider Abstraction Layer is the scalability backbone. Every feature module talks only to the `VcsProvider` interface, never to a GitHub-specific class directly. GitHub is the first concrete implementation; others are stubs that can be filled in later.

Key interface groups:
- `AuthProvider` — connect, disconnect, validate token, refresh
- `IssueProvider` — list, get, create, update, close, search
- `PullRequestProvider` — list, get, create, update, merge, review
- `BranchProvider` — list, create, delete, link to issue/PR
- `CommentProvider` — list, create, edit, delete (issues + PRs)
- `RepositoryProvider` — get current repo context, list user repos

---

## 4. Phase 1 — Project Scaffolding

### Goals

Stand up a working plugin skeleton that installs, shows a tool window, and does nothing yet — but does it correctly.

### Steps

1. Initialize Gradle project with IntelliJ Platform Gradle Plugin v2
2. Configure `plugin.xml`:
    - Set `<id>`, `<name>`, `<vendor>`, `<description>`
    - Set `<idea-version since-build>` to cover Android Studio + recent IDEA builds
    - Declare `<depends>com.intellij.modules.platform</depends>` (platform-level, not IDE-specific)
    - Avoid depending on `com.intellij.modules.java` unless strictly needed — keeps it IDE-agnostic
3. Register a `toolWindow` extension point pointing to an empty panel factory
4. Configure `build.gradle.kts`:
    - Set `intellijPlatform.type` to `IC` (IntelliJ Community) for builds; Android Studio tested separately
    - Add Kotlin coroutines, Ktor, kotlinx-serialization dependencies
    - Configure `runIde` task for local dev testing
5. Set up version catalog (`libs.versions.toml`) for dependency management
6. Set up `.github/workflows/build.yml` for CI — build + verify on every push

### Deliverable

Plugin loads in a sandboxed IDE instance via `./gradlew runIde`. Tool window appears with a placeholder UI.

### Testing — Iteration 1

- Manual: Plugin loads without errors in `runIde` sandbox
- Manual: Tool window visible and togglable
- Automated: Gradle `verifyPlugin` task passes (checks `plugin.xml` correctness)
- Automated: `buildPlugin` task produces a valid ZIP

---

## 5. Phase 2 — Auth Layer

### Goals

Implement secure, seamless authentication against GitHub. Must feel native — no browser popups spawned from weird windows, credentials stored in OS keychain, and a clean connected/disconnected state communicated to the rest of the plugin.

### Auth Strategy

Two modes supported:

| Mode | Use Case | Flow |
|---|---|---|
| OAuth Device Flow | Primary — most user-friendly, no redirect URI needed | User gets a code, opens browser, approves, done |
| Personal Access Token (PAT) | Power users, CI, orgs with SSO | User pastes token into a settings field |

OAuth Device Flow is preferred because it requires no callback server running inside the IDE, which is the cleanest approach for a desktop plugin.

### Steps

1. Register a GitHub OAuth App (developer-side setup, documented for users)
2. Implement `AuthManager` as an `ApplicationService`:
    - `initiateDeviceFlow()` — calls `/login/device/code`, returns user code + verification URL
    - `pollForToken()` — polls `/login/oauth/access_token` on interval until approved or expired
    - `validateToken()` — calls `GET /user` to verify token validity on startup
    - `saveCredentials()` / `loadCredentials()` — uses `PasswordSafe` API
    - `disconnect()` — clears stored credentials, resets state
3. Implement PAT flow as a fallback in Settings panel
4. Implement `AuthState` as a sealed class: `Unauthenticated`, `Authenticating(userCode, verificationUrl)`, `Authenticated(username, avatarUrl)`, `Error(reason)`
5. Show auth UI in tool window when state is `Unauthenticated`:
    - "Connect to GitHub" button
    - Displays device code + clickable verification URL
    - Progress spinner while polling
    - Success state with avatar + username once connected
6. Token refresh handling — GitHub OAuth tokens don't expire by default; PATs can; handle 401 responses gracefully with re-auth prompt

### Security Requirements

- Tokens stored **only** in `PasswordSafe` — never in plain text files or `PropertiesComponent`
- Client secret **never** bundled in plugin JAR — use Device Flow (no secret needed client-side)
- Token only transmitted over HTTPS via Ktor (enforced by default)

### Testing — Iteration 2

- Unit: `AuthManager` state machine transitions tested with MockK
- Unit: `PasswordSafe` read/write with mocked IntelliJ service
- Integration: Device flow happy path with WireMock stubbing GitHub endpoints
- Integration: PAT validation with valid and invalid token stubs
- Manual: Full OAuth Device Flow in `runIde` against real GitHub
- Manual: Disconnect and reconnect cycle
- Manual: Plugin restarts retain authenticated state

---

## 6. Phase 3 — Provider Abstraction Layer

### Goals

Define the `VcsProvider` interface hierarchy and implement the GitHub concrete implementation. This phase produces no visible UI — it's the data backbone everything else sits on.

### Steps

1. Define all provider interfaces in a `provider` package:
    - All methods are `suspend` functions — async-first
    - All return types wrapped in a `Result<T>` or custom `VcsResult<T>` sealed class for explicit error handling
    - Interfaces annotated with `@ApiStatus.Experimental` for stubs not yet implemented
2. Implement `GitHubProvider`:
    - Wraps Ktor `HttpClient` with a pre-configured base URL, auth header injection, rate limit handling
    - Maps GitHub REST/GraphQL responses to internal domain models (no Retrofit/GitHub SDK dependency — keep it lean and controlled)
    - Implements pagination via cursor-based iteration (GitHub's `Link` header for REST, `pageInfo.endCursor` for GraphQL)
3. Implement domain model layer — internal data classes fully decoupled from API response shapes:
    - `VcsIssue`, `VcsPullRequest`, `VcsBranch`, `VcsComment`, `VcsUser`, `VcsRepository`, `VcsLabel`, `VcsMilestone`
4. Implement `RepositoryContextService` as a `ProjectService`:
    - Detects the current project's Git remote URL on project open
    - Parses owner + repo name from the remote URL
    - Exposes `currentRepo: StateFlow<VcsRepository?>` for the rest of the plugin to observe
5. Implement GitHub stub factories for GitLab and ADO — throw `NotImplementedError` with a clear message; they're placeholders for future implementation
6. Implement a `ProviderRegistry` — maps provider type enum to concrete implementation; single place to swap providers

### Testing — Iteration 3

- Unit: Domain model mapping from GitHub JSON fixtures (real API response snapshots saved as test resources)
- Unit: Pagination logic — verify correct cursor handling
- Unit: `RepositoryContextService` remote URL parsing (various URL formats: SSH, HTTPS, with/without `.git`)
- Integration: All `GitHubProvider` methods against WireMock with realistic response fixtures
- Integration: Rate limit 403 response handling — verify backoff behavior
- Integration: Network failure scenarios — verify `VcsResult.Error` propagation

---

## 7. Phase 4 — Issues Module

### Goals

Full two-way issues management. This is the primary module and gets the most polish.

### Feature Scope

| Feature | Direction | Notes |
|---|---|---|
| List issues | Read | Filter by state, label, assignee, milestone |
| View issue detail | Read | Title, body (Markdown rendered), metadata |
| Create issue | Write | Title, body, labels, assignees, milestone |
| Edit issue | Write | Update any field |
| Close / Reopen issue | Write | State toggle |
| Search issues | Read | Full-text search within repo |
| Link to branch | Read/Write | Show associated branches; create branch from issue |
| Link to PRs | Read | Show PRs that reference this issue |
| Labels management | Write | Create, apply, remove labels |

### Steps

1. Implement `IssueService` wrapping `IssueProvider` — adds caching, deduplication, coroutine scope management
2. Implement `IssueListPanel`:
    - Scrollable list with issue rows: number, title, state badge (open/closed), label chips, assignee avatar, updated timestamp
    - Filter bar: state toggle, label filter dropdown, assignee filter, search field
    - Pagination: "Load more" at bottom or infinite scroll
    - Context menu on row: Open in Browser, Copy Link, Create Branch, Close Issue
3. Implement `IssueDetailPanel`:
    - Header: issue number, title (editable inline), state badge, open/close action button
    - Body: Markdown rendered (use IntelliJ's `MarkdownUtil` or a lightweight renderer)
    - Metadata sidebar: Labels, Assignees, Milestone, Linked PRs, Linked Branches
    - Edit body: toggleable edit mode with a text area
    - Timeline placeholder (not in scope for this phase — just the structure)
4. Implement `CreateIssueDialog`:
    - Title field, body text area with Markdown preview toggle
    - Label picker (multi-select), assignee picker, milestone picker
    - Submit / Cancel
5. Wire up real-time state: on create/update/close, update the list panel immediately (optimistic update) and reconcile with server response
6. "Create Branch from Issue" action — pre-fills branch name with `{issue-number}-{slugified-title}` convention, opens branch creation dialog

### Testing — Iteration 4

- Unit: `IssueService` caching behavior — verify duplicate network calls suppressed
- Unit: Optimistic update + server reconciliation logic
- Unit: Issue filter state combinations
- Integration: Full CRUD cycle against WireMock
- Manual: Create, edit, close, reopen issue flow end-to-end
- Manual: Label creation and application
- Manual: Search with various queries
- Manual: Linked PR chips navigate to PR detail correctly
- Manual: "Create Branch" flow from issue detail

---

## 8. Phase 5 — Pull Requests Module

### Goals

Full two-way PR management with linkage to issues and branches.

### Feature Scope

| Feature | Direction | Notes |
|---|---|---|
| List PRs | Read | Filter by state, author, label, base branch |
| View PR detail | Read | Title, body, metadata, diff summary |
| Create PR | Write | Head branch, base branch, title, body, reviewers |
| Edit PR | Write | Update title, body, labels, reviewers |
| Merge PR | Write | Merge strategies: merge, squash, rebase |
| Close / Reopen PR | Write | State toggle |
| Link to issues | Read | Show issues closed by this PR |
| Link to branches | Read | Head and base branch navigation |
| Review status | Read | Show review states (approved, changes requested, pending) — not submitting reviews in scope yet |
| Draft PR support | Read/Write | Mark as draft, mark as ready |

### Steps

1. Implement `PRService` wrapping `PullRequestProvider` — mirrors the IssueService pattern
2. Implement `PRListPanel`:
    - Rows: PR number, title, state badge, draft indicator, review status indicators, author avatar, updated timestamp
    - Filter bar: state, author, base branch, label
    - Context menu: Open in Browser, Copy Link, Merge, Close
3. Implement `PRDetailPanel`:
    - Header: number, title (editable), state, draft badge, action buttons (Merge, Close, Reopen)
    - Body: Markdown rendered, editable
    - Merge section: strategy picker (merge/squash/rebase), merge button with confirmation
    - Metadata: labels, assignees, reviewers + their approval state chips, linked issues, head/base branch pills
    - Checks status: show CI check names and pass/fail/pending state (read-only)
4. Implement `CreatePRDialog`:
    - Base branch dropdown (filtered to repo branches)
    - Head branch dropdown (pre-filled if a branch from the issues module was used)
    - Title auto-fill from branch name
    - Body text area with Markdown preview toggle
    - Reviewer picker, label picker
    - Draft checkbox
5. Deep-link from Issues module: clicking a linked PR chip in IssueDetailPanel opens PRDetailPanel for that PR

### Testing — Iteration 5

- Unit: Merge action gating — verify merge button disabled when checks failing or reviews pending
- Unit: PR → Issue link resolution
- Integration: Create PR, update, merge cycle against WireMock
- Integration: Draft PR state transitions
- Manual: Full create → review status check → merge flow
- Manual: PR linked issues chips navigate correctly
- Manual: Conflict state communicated clearly in merge section

---

## 9. Phase 6 — Branches Module

### Goals

Branch awareness within the IDE, linked to issues and PRs. Not a replacement for the built-in Git plugin — this is additive, focused on remote branch management and VCS-provider context.

### Feature Scope

| Feature | Direction | Notes |
|---|---|---|
| List remote branches | Read | With last commit info, author, timestamp |
| Create branch | Write | From issue, from PR base, or from arbitrary ref |
| Delete remote branch | Write | With confirmation; warn if PR exists |
| View branch → linked issue | Read | Detect issue number in branch name |
| View branch → linked PR | Read | Show open PR for this branch |
| Checkout branch | Action | Delegates to IntelliJ's built-in Git integration |

### Steps

1. Implement `BranchService` wrapping `BranchProvider`
2. Implement `BranchListPanel`:
    - Grouped: default branch at top, then branches with open PRs, then rest
    - Row: branch name, last commit message snippet, author, relative timestamp, linked issue chip (if detected), linked PR chip (if exists)
    - Context menu: Checkout, Create PR from this Branch, Delete Remote Branch, Copy Branch Name
3. Implement `CreateBranchDialog` (shared with Issues module flow):
    - Branch name field with live validation (no spaces, valid chars)
    - Base ref selector
    - Auto-create remote on creation (push stub branch)
    - Option: link to issue (pre-filled from context if coming from issues module)
4. Integrate with IntelliJ's existing Git4Idea plugin:
    - Use `GitBrancher` API for checkout operations — don't re-implement what the platform provides
    - Listen to `GitRepository` change events to refresh branch list when local Git state changes
5. Branch name convention support: detect `{number}-{slug}` pattern and auto-resolve linked issue

### Testing — Iteration 6

- Unit: Branch name → issue number extraction with various naming patterns
- Unit: Branch list grouping logic
- Integration: Branch CRUD against WireMock
- Integration: `GitRepository` change event triggers panel refresh
- Manual: Create branch from issue, verify appears in branch list with linked issue chip
- Manual: Delete branch with open PR — confirm warning shown
- Manual: Checkout via "Checkout" action, verify IDE Git state updates

---

## 10. Phase 7 — Comments Module

### Goals

Two-way comment support on both issues and PRs. Comments appear inline in the detail panels.

### Feature Scope

| Feature | Direction | Notes |
|---|---|---|
| List comments on issue | Read | Chronological, with author + timestamp |
| List comments on PR | Read | Same |
| Post comment | Write | On issue or PR |
| Edit own comment | Write | Only comments authored by authenticated user |
| Delete own comment | Write | With confirmation |
| Markdown rendering | Read | Render comment body as Markdown |
| Reactions summary | Read | Show reaction counts (no posting reactions in scope) |

### Steps

1. Implement `CommentService` wrapping `CommentProvider`
2. Implement `CommentListComponent` — embeddable in both `IssueDetailPanel` and `PRDetailPanel`:
    - Scrollable comment list
    - Each comment: avatar, author name, relative timestamp, rendered Markdown body, edit/delete icons (own comments only), reaction count chips
    - "Add a comment" text area at the bottom with a Post button
3. Implement edit flow: clicking edit on own comment switches the rendered body to an editable text area in-place; Save / Cancel
4. Implement delete with a confirmation balloon popup
5. Handle pagination for issues/PRs with many comments (load more)
6. Real-time feel: optimistic add — comment appears immediately in list before server confirms, then reconciled

### Testing — Iteration 7

- Unit: Own-comment detection (only show edit/delete for authenticated user's comments)
- Unit: Optimistic comment insertion + rollback on failure
- Integration: Post, edit, delete cycle against WireMock
- Manual: Post a comment on a real issue in `runIde`
- Manual: Edit and delete own comment
- Manual: Verify edit/delete not shown for others' comments

---

## 11. Phase 8 — UI/UX System

### Goals

All the functional modules exist by Phase 7, but this phase ensures the entire UI is cohesive, native-feeling, performant, and accessible.

### Theme & Visual System

- **Dark/Light theme adherence**: All colors sourced from `UIManager` or `JBColor` pairs — no hardcoded hex values anywhere
- **New UI / Classic UI**: Test with both `New UI` enabled and disabled in settings; Jewel handles this natively if used
- **Islands support**: Tool window panels declared as Islands-compatible; use `@Composable` Jewel components where Islands rendering is expected
- **Typography**: Use `JBUI.Fonts` for all font references
- **Spacing**: Use `JBUI.scale()` for all pixel values to support HiDPI / different monitor densities
- **Icons**: Use `AllIcons` for platform icons; custom icons in SVG format for provider-specific icons (GitHub logo etc.), following JetBrains icon guidelines (both light and dark variants)

### Layout Structure

```
Tool Window
├── Header Bar
│   ├── Provider logo + repo name
│   ├── Connection status indicator
│   └── Settings gear icon
├── Navigation Tabs
│   ├── Issues
│   ├── Pull Requests
│   └── Branches
└── Content Area (swaps based on active tab)
    ├── List Panel (left/top)
    └── Detail Panel (right/bottom or inline expand)
```

### Navigation Model

- **Split panel**: List on left, detail on right (similar to IntelliJ's own VCS log)
- On narrow tool window: detail replaces list with back navigation
- Keyboard navigable: arrow keys in list, Enter to open detail, Escape to go back

### Steps

1. Audit all panels from Phases 2–7 for visual consistency
2. Replace any hardcoded colors/sizes
3. Implement responsive layout — tool window resize-aware (collapse to single-column below a threshold)
4. Implement loading states for all async operations — skeleton loaders or progress indicators
5. Implement empty states — helpful messages with action buttons when no data (e.g., "No open issues · Create one ↗")
6. Implement error states — inline error banners with retry actions
7. Implement keyboard shortcuts:
    - `N` — new issue / new PR (context dependent)
    - `R` — refresh current panel
    - `/` — focus search/filter bar
8. Add tooltips to all icon buttons
9. Conduct a full accessibility pass — screen reader labels on interactive elements

### Testing — Iteration 8

- Manual: Full UI walkthrough with Light theme
- Manual: Full UI walkthrough with Dark theme
- Manual: Full UI walkthrough with High Contrast theme
- Manual: Resize tool window to narrow — verify layout collapses correctly
- Manual: Keyboard-only navigation through all panels
- Manual: All loading, empty, and error states triggered deliberately
- Manual: Run on Android Studio (Jellyfish or latest stable) specifically
- Manual: Run on IntelliJ IDEA Community
- Manual: Run on IntelliJ IDEA Ultimate
- Manual: Verify HiDPI rendering on 2x display

---

## 12. Phase 9 — Cross-Cutting Concerns

### Goals

Things that don't belong to a single module but must be done right before shipping.

### 9.1 — Settings Panel

- Accessible via `Settings → Tools → RemoteVCS`
- Auth section: connected account info, disconnect button, switch account
- Default provider selector (GitHub selected by default)
- Preferences: default branch filter, issue state default filter, notification preferences
- Advanced: API base URL override (for GitHub Enterprise)

### 9.2 — Notifications

- Use `NotificationGroupManager` to register a notification group
- Notify on: PR review requested on you, issue assigned to you, comment on your issue/PR
- Notifications are non-intrusive balloons; clicking opens the relevant detail panel
- Notification polling runs on a background coroutine with a configurable interval (default: 5 min)

### 9.3 — Caching & Performance

- In-memory cache with TTL per resource type:
    - Issues: 2 min TTL (changes often)
    - PRs: 2 min TTL
    - Branches: 1 min TTL
    - Comments: 1 min TTL
    - User profile: 30 min TTL
- Manual refresh always bypasses cache
- Cache keyed by `{providerId}:{owner}:{repo}:{resourceType}:{id}`
- Don't persist cache to disk — always fresh on IDE restart

### 9.4 — Error Handling Strategy

- Network errors: inline retry with backoff
- Auth errors (401): surface re-auth dialog, do not crash
- Rate limit errors (403/429): show remaining reset time in status bar widget
- API errors (4xx/5xx): inline error banner with error message from provider
- All errors logged via IntelliJ's `Logger` (not `println` / `System.out`)

### 9.5 — Analytics & Telemetry (Optional)

- Opt-in only
- If implemented: use JetBrains' built-in statistics API (`FUSEventFields`)
- Track: feature usage counts (issues viewed, PRs created), provider type — no content, no user data

### Testing — Iteration 9

- Unit: Cache TTL expiry and invalidation logic
- Unit: Notification polling — verify no duplicate notifications
- Unit: Settings persistence — save, restart, reload cycle
- Integration: Rate limit handling — WireMock returns 403 with `X-RateLimit-Reset` header
- Manual: Settings panel — all fields save and persist across IDE restart
- Manual: Background notifications appear for assigned issues and review requests

---

## 13. Phase 10 — Testing Iterations

### Summary of All Test Tiers

#### Tier 1 — Unit Tests (per module, ongoing)

Run on every commit via CI. Fast, no IDE instance needed.

| Module | Key Coverage Areas |
|---|---|
| Auth | State machine, token storage, Device Flow polling |
| Provider | API response mapping, pagination, error mapping |
| Issues | CRUD logic, filtering, linking, cache |
| PRs | CRUD logic, merge gating, draft state |
| Branches | Name parsing, grouping, Git event integration |
| Comments | Own-comment detection, optimistic updates |
| Settings | Persistence serialization/deserialization |

Target: 80%+ line coverage on service and use-case layers.

#### Tier 2 — Integration Tests (per module, ongoing)

Run on every PR via CI. Require WireMock; no real IDE instance needed.

- All GitHub API endpoints exercised with realistic response fixtures
- Error and edge cases: empty results, large paginated results, malformed responses, network timeout
- Auth flows: complete Device Flow sequence, PAT validation, token refresh (401 handling)

#### Tier 3 — Plugin Integration Tests (per phase completion)

Use IntelliJ Platform Test Framework with headless IDE instance.

- Plugin loads without errors
- Tool window registers and renders
- Service injection resolves correctly
- `RepositoryContextService` detects project Git remote
- Settings panel saves and reads correctly

#### Tier 4 — Manual QA (per phase completion + before release)

Structured test scripts run against a real GitHub test org/repo.

**Auth QA Checklist**
- [ ] Device Flow: full authorize → connect flow
- [ ] PAT: valid token connects, invalid token shows error
- [ ] Disconnect and reconnect
- [ ] Restart IDE — remains connected
- [ ] Expired/revoked token handled gracefully

**Issues QA Checklist**
- [ ] Create issue with all fields filled
- [ ] Create issue with only required fields
- [ ] Edit issue title inline
- [ ] Edit issue body
- [ ] Apply labels, remove labels
- [ ] Close issue, reopen issue
- [ ] Search by keyword
- [ ] Filter by label, by assignee
- [ ] Linked PR chips appear and navigate
- [ ] Create branch from issue

**PR QA Checklist**
- [ ] Create PR from branch
- [ ] Edit PR title and body
- [ ] All three merge strategies (merge, squash, rebase)
- [ ] Close PR without merging
- [ ] Draft PR create and convert to ready
- [ ] Review status badges display correctly
- [ ] Linked issue chips navigate correctly

**Branch QA Checklist**
- [ ] Branch list loads with correct grouping
- [ ] Linked issue chip shows for `123-branch-name` pattern
- [ ] Create branch from scratch
- [ ] Create branch from issue
- [ ] Checkout via context menu (delegates to Git plugin)
- [ ] Delete remote branch — warning shown if PR exists

**Comments QA Checklist**
- [ ] Comments load in issue detail
- [ ] Comments load in PR detail
- [ ] Post new comment
- [ ] Edit own comment
- [ ] Delete own comment with confirmation
- [ ] Edit/delete not shown for others' comments

**Theme QA Checklist**
- [ ] Light theme — all UI elements visible, no invisible-on-white text
- [ ] Dark theme — all UI elements visible, no invisible-on-dark text
- [ ] High contrast theme — verified usable
- [ ] Darcula theme — verified usable
- [ ] New UI enabled — tool window renders correctly
- [ ] Classic UI — tool window renders correctly

**IDE Compatibility QA Checklist**
- [ ] Android Studio (latest stable)
- [ ] IntelliJ IDEA Community (latest stable)
- [ ] IntelliJ IDEA Ultimate (latest stable)
- [ ] One additional IDE (e.g., WebStorm or PyCharm)

#### Tier 5 — Pre-Release Regression

Full manual QA Tier 4 pass repeated on a clean install (no prior plugin state) immediately before Marketplace submission.

---

## 14. Phase 11 — Deployment Pipeline

### Steps

1. **GitHub Actions CI** — build, test, and `verifyPlugin` on every push to `main` and every PR
2. **Release tagging** — `v{major}.{minor}.{patch}` tags on `main` trigger release workflow
3. **Release workflow**:
    - Runs full test suite
    - Runs `buildPlugin` — produces ZIP artifact
    - Creates GitHub Release with ZIP attached
    - Publishes to JetBrains Marketplace via `publishPlugin` Gradle task using `JETBRAINS_MARKETPLACE_TOKEN` secret
4. **`CHANGELOG.md`** — maintained manually; each release section auto-attached to Marketplace upload description
5. **`plugin.xml` `<change-notes>`** — kept in sync with latest release notes
6. **IDE compatibility matrix** — `since-build` and `until-build` range updated each release after compatibility testing
7. **Versioning strategy**: `1.x` = GitHub-only; `2.x` = multi-provider support milestone

### Deployment Checklist (per release)

- [ ] All Tier 4 manual QA checks passed
- [ ] `plugin.xml` version bumped
- [ ] `CHANGELOG.md` updated
- [ ] `since-build` / `until-build` range reflects latest tested IDE builds
- [ ] `verifyPlugin` passes with no errors and zero critical warnings
- [ ] ZIP tested as a manual install on a clean Android Studio instance before Marketplace push
- [ ] Marketplace listing description, screenshots updated if UI changed significantly

---

## 15. Requirements Traceability Matrix

| Req ID | Requirement | Phases | Test Coverage |
|---|---|---|---|
| R1 | Seamless OAuth / PAT authentication | Phase 2 | Unit T2, Integration T3, Manual T4 |
| R2 | Native JetBrains look and feel | Phase 8 | Manual T4 (Theme QA) |
| R3 | New UI Islands support | Phase 8 | Manual T4 (Theme QA) |
| R4 | Two-way issue management | Phase 4 | All tiers |
| R5 | Two-way PR management | Phase 5 | All tiers |
| R6 | Branch management linked to issues/PRs | Phase 6 | All tiers |
| R7 | Comment support | Phase 7 | All tiers |
| R8 | Modern, user-friendly UI | Phase 8 | Manual T4 (full walkthrough) |
| R9 | Scalable to multiple VCS providers | Phase 3 (PAL) | Unit T2 (interface contract) |
| R10 | Works across all JetBrains IDEs | Phase 1 + Phase 8 | Manual T4 (IDE Compat QA) |

---

## 16. Risks & Mitigations

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| Jewel API instability (still evolving) | Medium | High | Fallback design with Swing + `JBColor` ready; Jewel isolated to UI layer so swap is feasible |
| GitHub API rate limits during dev/testing | High | Low | WireMock for tests; dedicated test GitHub org for manual testing |
| Android Studio build compatibility breaks between AS versions | Medium | Medium | CI matrix tests against both `IC` (IDEA) and `AI` (Android Studio) build types |
| New UI / Classic UI visual inconsistencies | Medium | Medium | Explicit manual test pass for both modes in Phase 8 |
| JetBrains Marketplace review rejection | Low | Medium | Run `verifyPlugin` and review plugin submission guidelines before first upload |
| IntelliJ Platform internal API usage flagged as unstable | Medium | Low | Prefer `@ApiStatus.Stable` APIs; `verifyPlugin` flags unstable usages in CI |

---

*Last updated: Phase planning stage — pre-implementation*
*Stack decisions subject to revision at Phase 1 implementation kickoff*