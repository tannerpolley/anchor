# Remote VCS Plugin — UI/UX Design Specification
### Premium, Info-Dense, JetBrains-Native

> **Guiding principle:** Every pixel earns its place. No decorative noise, no wasted space — but every element that's there is crafted to feel intentional, scannable, and alive.

---

## Table of Contents

1. [Design Philosophy](#1-design-philosophy)
2. [Design Token System](#2-design-token-system)
3. [Typography Scale](#3-typography-scale)
4. [Iconography](#4-iconography)
5. [Component Library](#5-component-library)
6. [Tool Window Shell](#6-tool-window-shell)
7. [Issues Module — Full Spec](#7-issues-module--full-spec)
8. [Pull Requests Module — Full Spec](#8-pull-requests-module--full-spec)
9. [Branches Module — Full Spec](#9-branches-module--full-spec)
10. [Detail Panels — Full Spec](#10-detail-panels--full-spec)
11. [Comments Component — Full Spec](#11-comments-component--full-spec)
12. [Auth & Onboarding Flow](#12-auth--onboarding-flow)
13. [Settings Panel](#13-settings-panel)
14. [Dialogs](#14-dialogs)
15. [State System (Loading / Empty / Error)](#15-state-system-loading--empty--error)
16. [Interactions & Motion](#16-interactions--motion)
17. [Observed Issues & Fixes](#17-observed-issues--fixes)

---

## 1. Design Philosophy

### The Problem With the Current State

Looking at the current screenshots, the plugin is functional but falls into the "raw Swing table dump" trap common to early JetBrains plugins. Specific issues:

- **Header** shows raw `GitRemoteInfo(provider=github, owner=finsible, repoName=android-client, remoteName=origin, remoteUrl=https://git...` — this is a debug toString(), not UI
- **State column** truncates to `O...` and `C...` — the dots indicate the text is clipped. State should be a badge, not a truncated string
- **PR status column** is `...` — completely unreadable; data isn't fitting the column
- **Branches list** is just a flat name + SHA dump with no grouping, no linked context, no PR/issue chips
- **No detail panel** — there's no split view; clicking an item likely does nothing visible
- **Settings panel** is the cleanest of the four, but the layout has too much empty air and no visual section anchoring

### Design Goals

| Goal | Meaning in practice |
|---|---|
| **JetBrains-native** | Uses platform fonts, colors, spacing tokens — not custom theming that fights the IDE |
| **Info-dense without clutter** | Lots of data visible at once, but organized via hierarchy — not noise |
| **Premium feel** | Crisp, intentional, polished micro-interactions. Feels like the GitHub or Linear app, not a form |
| **Theme-complete** | Dark, Light, High Contrast, Darcula all look great natively — zero hardcoded colors |
| **Scannable in 1 second** | A developer glancing at the panel instantly knows: what's open, what's theirs, what needs attention |

### Visual Personality

The plugin lives in a dark professional IDE used by developers who appreciate craft. The visual language should be:

- **Compact and precise** — not airy or spacious
- **Accent-forward** — use accent color (platform blue) strategically for active states, links, and primary actions
- **Badge-centric** — status, labels, and context are communicated via small inline badges, not text columns
- **Depth via separation** — headers, panels, and toolbars have subtle separator lines; no drop shadows

---

## 2. Design Token System

All values mapped to IntelliJ Platform `UIManager` / `JBColor` / `JBUI` equivalents. **Zero hardcoded hex values.**

### Color Tokens

| Token Name | JetBrains API | Purpose |
|---|---|---|
| `bg.primary` | `UIUtil.getPanelBackground()` | Main panel background |
| `bg.secondary` | `JBUI.CurrentTheme.ToolWindow.background()` | Tool window background |
| `bg.elevated` | `UIUtil.getTableBackground()` | Slightly elevated surfaces (header bar, filter bar) |
| `bg.hover` | `UIUtil.getTableSelectionBackground(false)` | Row hover state (unfocused selection color) |
| `bg.selected` | `UIUtil.getTableSelectionBackground(true)` | Row selected state (focused) |
| `bg.input` | `UIUtil.getTextFieldBackground()` | Input fields |
| `text.primary` | `UIUtil.getLabelForeground()` | Main readable text |
| `text.secondary` | `UIUtil.getContextHelpForeground()` | Supporting / metadata text |
| `text.disabled` | `UIUtil.getLabelDisabledForeground()` | Disabled / placeholder text |
| `text.link` | `JBUI.CurrentTheme.Link.linkColor()` | Clickable links, issue/PR numbers |
| `text.selected` | `UIUtil.getTableSelectionForeground(true)` | Text on selected rows |
| `accent.primary` | `JBUI.CurrentTheme.Button.focusedBorderColor()` | Active tab indicator, primary buttons |
| `accent.github` | `JBColor(0x2DA44E, 0x3FB950)` | GitHub green — open issues/PRs |
| `accent.closed` | `JBColor(0x8957E5, 0x8957E5)` | GitHub purple — closed issues |
| `accent.merged` | `JBColor(0x8957E5, 0x8957E5)` | Merged PR (same purple as GitHub) |
| `accent.draft` | `JBColor(0x656D76, 0x848D97)` | Draft PR / closed state |
| `accent.warning` | `JBUI.CurrentTheme.NotificationInfo.borderColor()` | Warnings, WIP indicators |
| `border.primary` | `JBUI.CurrentTheme.CustomFrameDecorations.separatorForeground()` | Dividers, panel borders |
| `border.focused` | `JBUI.CurrentTheme.Button.focusedBorderColor()` | Focused input borders |

### Badge Color Tokens

Badges (labels, state chips) have their own palette:

| Badge Type | Background | Text | Border |
|---|---|---|---|
| Open (issue/PR) | `accent.github` @ 15% alpha | `accent.github` | `accent.github` @ 40% alpha |
| Closed (issue) | `accent.closed` @ 15% alpha | `accent.closed` | `accent.closed` @ 40% alpha |
| Merged (PR) | `accent.merged` @ 15% alpha | `accent.merged` | `accent.merged` @ 40% alpha |
| Draft (PR) | `accent.draft` @ 15% alpha | `accent.draft` | `accent.draft` @ 40% alpha |
| Label chip | varies per label color, alpha-softened | label color | label color @ 50% alpha |
| Branch pill | `bg.elevated` | `text.secondary` | `border.primary` |
| Review approved | `#2DA44E` @ 15% alpha | `#2DA44E` | none |
| Review changes | `#E3B341` @ 15% alpha | `#E3B341` | none |
| Review pending | `bg.elevated` | `text.disabled` | none |

### Spacing Scale (all via `JBUI.scale()`)

| Token | Scaled value | Use |
|---|---|---|
| `space.1` | 2px | Icon-to-text gap, badge internal padding |
| `space.2` | 4px | Tight item spacing |
| `space.3` | 6px | Badge padding horizontal |
| `space.4` | 8px | Standard inner padding |
| `space.5` | 12px | Section gap, list row vertical padding |
| `space.6` | 16px | Panel padding, dialog section gap |
| `space.7` | 20px | Large section separation |
| `space.8` | 24px | Header height padding |

---

## 3. Typography Scale

All via `JBUI.Fonts` — never hardcoded font names or sizes.

| Role | API | Use |
|---|---|---|
| `type.ui.default` | `JBUI.Fonts.label()` | Table cell text, filter labels |
| `type.ui.small` | `JBUI.Fonts.smallFont()` | Timestamps, SHA, secondary metadata |
| `type.ui.bold` | `JBUI.Fonts.label().asBold()` | Column headers, selected row title |
| `type.ui.medium` | `JBUI.Fonts.label(13f)` | Detail panel title, dialog headings |
| `type.code` | `JBUI.Fonts.monospace()` | SHA hashes, branch names |
| `type.header` | `JBUI.Fonts.label(11f)` | Column headers (uppercase, letter-spaced via HTML renderer) |

---

## 4. Iconography

### Platform Icons Used

Prefer `AllIcons` — do not bundle equivalents:

| Use | Icon |
|---|---|
| Issues tab | `AllIcons.General.TodoDefault` or custom |
| PR tab | `AllIcons.Vcs.Merge` |
| Branches tab | `AllIcons.Vcs.Branch` |
| Refresh | `AllIcons.Actions.Refresh` |
| Filter | `AllIcons.General.Filter` |
| New issue / PR | `AllIcons.General.Add` |
| Edit | `AllIcons.Actions.Edit` |
| Delete | `AllIcons.Actions.GC` |
| Close issue | `AllIcons.Actions.Cancel` |
| Merge | `AllIcons.Vcs.Merge` |
| Copy link | `AllIcons.Actions.Copy` |
| Open in browser | `AllIcons.Ide.External_link_arrow` |
| Comment | `AllIcons.Actions.InlayRenameInComments` |
| Assignee avatar | `AllIcons.General.User` (fallback) |
| GitHub provider | Custom SVG — GitHub mark, light + dark variants |
| Settings | `AllIcons.General.Settings` |
| Back navigation | `AllIcons.Actions.Back` |
| Notification bell | `AllIcons.Ide.Notification.NoEvents` / `...Gear` |
| Approve / Check | `AllIcons.Actions.Commit` |
| Diff / Files changed | `AllIcons.Actions.Diff` |

### Custom SVG Icons

Bundle these as `/icons/` with `-dark` variants (JetBrains naming convention):

- `github_mark.svg` / `github_mark_dark.svg` — 16×16 GitHub Invertocat logo
- `gitlab_mark.svg` / `gitlab_mark_dark.svg` — stub for future
- `issue_open.svg` — filled circle with dot (green tint)
- `issue_closed.svg` — filled circle with checkmark (purple tint)
- `pr_open.svg` — branch arrow open (green tint)
- `pr_merged.svg` — branch arrow merged (purple tint)
- `pr_draft.svg` — branch arrow dashed (gray tint)
- `pr_closed.svg` — branch arrow closed (red tint)

All SVGs: 16×16, no embedded colors (use `currentColor` or stroke-only for theme adaptation).

---

## 5. Component Library

Reusable primitives shared across all modules.

### 5.1 — State Badge

The core display unit for issue/PR status. Replaces the current truncated "O..." / "C..." text.

```
┌─────────────────┐
│ ● Open          │  ← 16px icon + text, pill shape
└─────────────────┘
```

**Spec:**
- Shape: Pill (border-radius = height / 2)
- Height: `JBUI.scale(18)`
- Horizontal padding: `space.3` (6px)
- Icon: 10×10 SVG state icon, left-aligned
- Gap between icon and text: `space.1` (2px)
- Font: `type.ui.small`, medium weight
- Background + border: from badge color tokens above
- States: `OPEN`, `CLOSED`, `MERGED`, `DRAFT`, `CLOSED_NOT_MERGED`

**Do not truncate.** State badge text is always "Open", "Closed", "Merged", "Draft" — short enough to always fit.

### 5.2 — Label Chip

Inline colored chip for GitHub labels.

**Spec:**
- Shape: Pill, height `JBUI.scale(16)`
- Font: `type.ui.small`
- Color: derived from GitHub label hex color — softened to 20% alpha background, full-opacity text
- Max width: `JBUI.scale(80)`, ellipsis if label name is long
- Tooltip: full label name on hover if truncated
- Multiple chips wrap in a horizontal flow, max 3 visible then `+N more` chip

### 5.3 — Branch Pill

Inline chip for branch names — monospace font, icon prefix.

```
  ⑂ main          ← branch icon + monospace name
```

**Spec:**
- Shape: Rounded rect (radius 3px), height `JBUI.scale(18)`
- Font: `type.code` at small size
- Icon: `AllIcons.Vcs.Branch` at 12×12
- Background: `bg.elevated`
- Border: `border.primary`
- On click: navigates to branch in Branches tab

### 5.4 — Avatar

Circular user avatar with fallback initials.

**Spec:**
- Size variants: 16px (inline list rows), 24px (detail panel header), 32px (comment header)
- Shape: Perfect circle, clipped
- Fallback: Single uppercase initial of username, rendered on `bg.elevated` background
- Loaded: GitHub avatar URL fetched and cached; shown via async image loader
- Border: none by default; 2px `border.focused` ring when it's the authenticated user

### 5.5 — SHA Chip

Monospace short SHA display.

**Spec:**
- Shows first 7 chars (standard Git short SHA)
- Font: `type.code` + `type.ui.small`
- Color: `text.secondary`
- Background: `bg.elevated`
- Border-radius: 3px
- On click: copies full SHA to clipboard; shows a brief "Copied!" tooltip balloon

### 5.6 — Inline Action Toolbar

Appears on row hover — replaces the need for right-click only discoverability.

```
  [row content...............................] [🌐] [⎘] [✕]
```

**Spec:**
- Absolutely positioned (overlay), trailing end of row, vertically centered
- Background: gradient fade from transparent to `bg.hover` — so it doesn't abruptly cover content
- Icons: 14×14, `text.secondary` color, `text.primary` on hover
- Tooltip on each icon (Open in Browser, Copy Link, Close)
- Appears on row hover after 150ms delay
- Fades in: 100ms opacity transition

### 5.7 — Separator

Horizontal divider used between sections.

**Spec:**
- 1px height, `border.primary` color
- Full width minus panel horizontal padding
- `JSeparator` or manual painted line

### 5.8 — Empty State

Centered content when a list has no items.

```
        [icon]
   No open issues

   Issues you create will appear here.
   [+ Create Issue]
```

**Spec:**
- Icon: 32×32 relevant platform icon, `text.disabled` color
- Title: `type.ui.medium`, `text.secondary`
- Subtitle: `type.ui.default`, `text.disabled`
- CTA button: only shown where creation is available; `JButton` with `ActionButtonLook`
- Centered vertically + horizontally in available panel space

---

## 6. Tool Window Shell

### 6.1 — Header Bar (Critical Fix)

**Current:** Raw `GitRemoteInfo(provider=github, owner=finsible, repoName=android-client, remoteName=origin, remoteUrl=https://git...`

**Specified:**

```
┌──────────────────────────────────────────────────────────────────────┐
│  [●] finsible / android-client           [↻]  [🔔]  [⋮]            │
│      origin · main                                                    │
└──────────────────────────────────────────────────────────────────────┘
```

**Layout:**
- Left: Provider status dot (green = connected, gray = not connected) + `owner / repoName` in `type.ui.default` bold + `remoteName · currentBranch` in `type.ui.small` `text.secondary` on second line — OR single line on narrow widths with separator `·`
- Right: Refresh action button (`AllIcons.Actions.Refresh`), Notifications bell, Overflow menu (⋮)
- Height: `JBUI.scale(44)` to accommodate two lines
- Background: `bg.elevated`
- Bottom border: 1px `border.primary` separator
- Provider dot: 8px circle, color from connection state; tooltip shows full remote URL on hover

**Connection states for the dot:**
- Green `accent.github` — authenticated and connected
- Yellow `accent.warning` — connected but rate limited
- Gray `accent.draft` — not authenticated / disconnected
- Animated pulse on the dot when a background fetch is in progress (CSS-like pulse via Swing Timer)

**On click of `finsible / android-client` label:** Opens a repo picker dropdown if the user has multiple repos configured. Otherwise no action.

### 6.2 — Navigation Tabs

**Current:** Basic `JBTabPane` or manual button row with icon + text labels.

**Specified:**

```
  Issues (12)   Pull Requests (3)   Branches
  ▔▔▔▔▔▔▔▔▔▔
```

- Style: Flat tab row, no raised tab styling
- Active indicator: 2px bottom border line in `accent.primary` (platform accent color) — same pattern as IntelliJ's own editor tabs in New UI mode
- Count badge: Inline number in parentheses, `text.secondary` color. Shows count of open items. Hidden when count is 0 or unknown
- Font: `type.ui.default` — not bold on inactive, slightly bolder on active
- Tab height: `JBUI.scale(30)`
- No icons in tabs — text-only; icons are used in the header and rows
- Separator: 1px `border.primary` below the entire tab row

### 6.3 — Toolbar / Filter Bar

Appears below the tabs, above the list. Contextually adapts per tab.

**Layout:**

```
  [🔍 Search...........................] [Labels ▾] [State ▾] [Assignee ▾]
```

- Left: Search field, full-width but capped; placeholder text contextual ("Search issues...", "Search pull requests...")
- Right: Filter dropdowns — only the filters relevant to the current tab
- Height: `JBUI.scale(32)`
- Background: same as header, `bg.elevated`
- Bottom: 1px separator
- Filter dropdowns: `ComboBoxAction` or `ActionButton` with popup; selected filter shown with a filled indicator dot

**Issues filters:** State (Open / Closed / All), Label (multi-select), Assignee, Milestone
**PR filters:** State (Open / Closed / Merged / All), Author, Base branch
**Branches filters:** Prefix/search only (no dropdowns needed)

### 6.4 — Content Area Layout

**Split panel model** — list left, detail right. Industry-standard pattern (same as IntelliJ's own Git log).

```
┌───────────────────────┬──────────────────────────────────────┐
│  LIST PANEL           │  DETAIL PANEL                        │
│  (min 280px)          │  (fills remaining space)             │
│                       │                                      │
│  [list items...]      │  [selected item detail]              │
│                       │                                      │
└───────────────────────┴──────────────────────────────────────┘
```

- Split via `JBSplitter` (the standard IntelliJ splitter component) — user-draggable
- Default split ratio: 40% list / 60% detail
- Minimum list panel width: `JBUI.scale(280)`
- When tool window width < `JBUI.scale(520)`: collapse to single-panel mode — list fills full width; clicking a row replaces the view with detail + back button
- Detail panel shows placeholder "Select an item" empty state when nothing selected

---

## 7. Issues Module — Full Spec

### 7.1 — Issues List Panel

**Column structure (redesigned):**

| Column | Width | Content | Notes |
|---|---|---|---|
| Status | 56px fixed | `StateBadge` component | "Open" or "Closed" — never truncated |
| # | 40px fixed | Issue number | `text.link` color, monospace |
| Title | flex (fills) | Issue title text | Truncate with ellipsis only here |
| Labels | 120px | Up to 2 `LabelChip` components + `+N` | Hidden if no labels |
| Author | 80px | Avatar (16px) + username | `text.secondary` |
| Comments | 36px | Comment icon + count | Hidden when 0 |
| Updated | 72px | Relative time | "2d ago", "3w ago" — `type.ui.small` |

**Row spec:**
- Row height: `JBUI.scale(36)` — tall enough to breathe, short enough to be dense
- Row hover: `bg.hover` background, inline action toolbar fades in (Open in Browser, Copy Link, Quick Close)
- Row selected: `bg.selected` background, `text.selected` for primary text
- Row alternating background: subtle — only if it aids scanning; prefer hover-only if the theme doesn't need it
- Clicking any row: loads issue detail in the detail panel (right side)

**Column header:**
- Row height: `JBUI.scale(26)`
- Background: `bg.elevated`
- Font: `type.header` — small caps style via HTML renderer or font attributes, `text.secondary` color
- Sortable columns (# and Updated): show sort arrow icon on hover and when active
- Bottom separator: 1px `border.primary`

**Behavior notes:**
- List is virtualized — render only visible rows (standard JTable handles this)
- Pagination: auto-load-more when user scrolls within 5 rows of the bottom; brief spinner row at bottom while loading
- No horizontal scroll — all columns must fit in available width; Title column absorbs the flex space

### 7.2 — Issues List — Observed Fix

**Before:** State column shows `● O...` and `● C...` — dots are clipped text, not intentional badges

**After:** Full `StateBadge` component with "Open" / "Closed" text — always fully visible because the column is 56px fixed

---

## 8. Pull Requests Module — Full Spec

### 8.1 — PR List Panel

**Column structure (redesigned):**

| Column | Width | Content | Notes |
|---|---|---|---|
| Status | 68px fixed | `StateBadge` | "Open", "Draft", "Merged", "Closed" |
| # | 40px fixed | PR number | `text.link`, monospace |
| Title | flex | PR title | Truncate here |
| Branch | 140px | `BranchPill` (head branch) | Monospace, truncated pill |
| Author | 80px | Avatar + username | `text.secondary` |
| Checks | 28px | CI check aggregate dot | Green/red/yellow/gray |
| Updated | 72px | Relative time | `type.ui.small` |

**State badge values for PRs:**
- Open: green badge, "Open"
- Draft: gray badge, "Draft"
- Merged: purple badge, "Merged"
- Closed (unmerged): red-toned badge, "Closed"

**Checks dot:**
- 10px circle in the Checks column
- Green: all checks passed
- Red: at least one check failed
- Yellow: checks in progress
- Gray: no checks configured
- Tooltip: "3/4 checks passed" on hover

**Observed fix:** Current screenshot shows `St...` truncated in the status column header and `● ...` in status cells — completely unreadable. The redesigned layout uses a fixed 68px column for the full badge, no truncation.

---

## 9. Branches Module — Full Spec

### 9.1 — Branches List Panel (Major Redesign)

**Current:** Flat list of `branchName | SHA` — no hierarchy, no context

**Redesigned — grouped + contextual:**

```
  ✦ DEFAULT                                                    
  ────────────────────────────────────────────────────────────
  main                         8f3a1b2c    2d ago    alph-a07

  ↗ WITH OPEN PULL REQUESTS                                    
  ────────────────────────────────────────────────────────────
  enhancement/new-transaction-ux  7a852f66  3w ago  alph-a07  [→ PR #99]
  epic/may-2026-overhaul-01       0de80248  1w ago  alph-a07  [→ PR #102]

  ⑂ ALL BRANCHES  (47)                                         
  ────────────────────────────────────────────────────────────
  bugfix/fetch-txns-post-auth     178ea360  2m ago  alph-a07
  bugfix/objectbox-crashes        9553806a  2m ago  alph-a07
  ...
```

**Group headers:**
- Font: `type.header` — small caps, `text.disabled`
- No background change — just text label with trailing separator line
- Collapsible with triangle toggle; state persisted in `PersistentStateComponent`

**Column structure:**

| Column | Width | Content |
|---|---|---|
| Branch name | flex | Icon + `type.code` name, truncated |
| SHA | 72px | `ShaChip` component |
| Updated | 72px | Relative time |
| Author | 80px | Avatar + name |
| Linked PR/Issue | 100px | `→ PR #N` chip or `⬡ Issue #N` chip, if detected |

**Branch name parsing:**
- Pattern `{number}-{slug}` → show linked issue chip auto-detected
- Pattern `copilot/sub-pr-{N}` → infer likely linked PR by number
- Pattern `bugfix/` → `bugfix` label chip
- Pattern `feature/` → `feature` label chip
- Pattern `enhancement/` → `enhancement` label chip
- Pattern `epic/` → `epic` label chip (yellow accent)

**Row context menu:**
- Checkout (delegates to Git plugin's `GitBrancher`)
- Create PR from this Branch
- View Linked Issue (if detected)
- Delete Remote Branch (with confirmation; warns if PR exists)
- Copy Branch Name

**SHA column fix:** Current screenshot shows `SHA` as a full-width column with raw long hex. Redesigned: `ShaChip` shows only first 7 chars, always monospace, copy on click.

---

## 10. Detail Panels — Full Spec

This is the most critical missing piece in the current UI. The detail panel appears to the right of the list in split view, or replaces the list in narrow/single-panel mode.

### 10.1 — Issue Detail Panel

```
┌──────────────────────────────────────────────────────────────────────┐
│  ← Back   #99 · [● Open]   [Edit]   [Close Issue]  [↗ Open in GH]  │
├──────────────────────────────────────────────────────────────────────┤
│  Revamp New Transaction for faster UX                                │
│  type.ui.medium, bold, `text.primary`, wrapping                      │
├──────────────────────────────────────────────────────────────────────┤
│  DETAILS                                                             │
│  ─────────────────────────────────────────────────────────          │
│  Assignees   [avatar] alph-a07                                       │
│  Labels      [enhancement] [ux]                                      │
│  Milestone   v2.0                                                    │
│  Linked PRs  → PR #101                                               │
│  Branch      ⑂ enhancement/new-transaction-ux                        │
├──────────────────────────────────────────────────────────────────────┤
│  DESCRIPTION                                                         │
│  ─────────────────────────────────────────────────────────          │
│  [rendered Markdown content]                                         │
│  [Edit Description]  ← small text button                            │
├──────────────────────────────────────────────────────────────────────┤
│  COMMENTS  (0)                                                       │
│  ─────────────────────────────────────────────────────────          │
│  [CommentList component]                                             │
└──────────────────────────────────────────────────────────────────────┘
```

**Header bar:**
- Height: `JBUI.scale(40)`
- Background: `bg.elevated`
- Bottom separator
- Back button: only shown in single-panel narrow mode
- Issue number: `type.code`, `text.secondary`
- State badge: full `StateBadge` component
- Action buttons right-aligned: Edit (pencil icon), Close/Reopen (contextual), Open in GitHub (external link icon)

**Title:**
- Font: `type.ui.medium` with increased size (~14px scaled)
- Padding: `space.6` horizontal, `space.5` vertical
- Inline editable: clicking the title once selects it; double-click or Edit button enters edit mode — title becomes a `JBTextField` in-place

**Details sidebar section:**
- Section header: "DETAILS" in `type.header` style
- Each row: label in `text.secondary` at `JBUI.scale(90)` width, value right of it
- Labels row: inline `LabelChip` components, horizontal wrap
- Linked PRs: `→ PR #N` clickable chips in `text.link` — clicking navigates to that PR in the PR detail panel
- Branch: `BranchPill` clickable — clicking navigates to branch in Branches tab

**Description section:**
- Markdown rendered via `MarkdownJPanel` (use IntelliJ's built-in markdown renderer if available, otherwise HTMLEditorKit with GitHub-flavored Markdown HTML conversion)
- Edit mode: Markdown source in a `JBTextArea` with monospace font; Save/Cancel buttons below
- Empty state: italic "No description provided." in `text.disabled`

### 10.2 — PR Detail Panel

Same structural layout as Issue, with additions:

```
┌──────────────────────────────────────────────────────────────────────┐
│  ← Back   #101 · [● Open]  [Edit]  [Merge ▾]  [Close PR]  [↗]      │
├──────────────────────────────────────────────────────────────────────┤
│  Revamp New Transaction for faster UX                                │
├──────────────────────────────────────────────────────────────────────┤
│  DETAILS                                                             │
│  ─────────────────────────────────────────────────────────          │
│  Base ← Head   ⑂ main  ← ⑂ enhancement/new-transaction-ux           │
│  Assignees     [avatar] alph-a07                                     │
│  Reviewers     [avatar] reviewer1 ✓  [avatar] reviewer2 ⏳          │
│  Labels        [enhancement]                                         │
│  Closes        ⬡ Issue #99                                           │
│  Checks        ● 4/4 passed                                          │
├──────────────────────────────────────────────────────────────────────┤
│  MERGE                                                               │
│  ─────────────────────────────────────────────────────────          │
│  [○ Create merge commit  ○ Squash and merge  ○ Rebase and merge]    │
│  [        Merge Pull Request        ]   ← primary action button     │
├──────────────────────────────────────────────────────────────────────┤
│  DESCRIPTION + COMMENTS                                              │
└──────────────────────────────────────────────────────────────────────┘
```

**Merge section:**
- Only shown when PR is open (not merged, not closed, not draft)
- Strategy selector: radio buttons styled as a segmented button group
- Merge button: disabled (grayed, tooltip "Awaiting required reviews") if reviews or checks block merging
- Merge button: destructive-action style — shows a confirmation balloon on click before executing

**Reviewer row:**
- Each reviewer shown as avatar + username + review state icon:
    - ✓ green: approved
    - ✕ red: changes requested
    - ⏳ gray: review pending/requested

**Checks row:**
- Expandable: click the row to expand an inline list of individual check names + their status icons
- Collapsed default: shows aggregate count "3/4 checks passed"

### 10.3 — Branch Detail Panel

Simpler than Issues/PRs:

```
┌──────────────────────────────────────────────────────────────────────┐
│  ← Back   ⑂ enhancement/new-transaction-ux                [Checkout]│
├──────────────────────────────────────────────────────────────────────┤
│  Last commit: "Implement bottom sheet for date picker"               │
│  7a852f66 · 3 weeks ago · alph-a07                                   │
├──────────────────────────────────────────────────────────────────────┤
│  LINKED                                                              │
│  Open PR:    → PR #101 Revamp New Transaction...                     │
│  Issue:      ⬡ Issue #99 Revamp New Transaction...                   │
├──────────────────────────────────────────────────────────────────────┤
│  AHEAD / BEHIND main                                                 │
│  ▶ 3 commits ahead, 0 behind                                         │
│  ─────────────────────────────────────────────────────────          │
│  [recent commits list, last 5]                                       │
│  + Create PR from this branch                                        │
└──────────────────────────────────────────────────────────────────────┘
```

---

## 11. Comments Component — Full Spec

Embedded in both Issue and PR detail panels, below the description.

### Comment Item Layout

```
┌──────────────────────────────────────────────────────────────────────┐
│  [avatar 24px]  alph-a07         2 weeks ago      [Edit] [Delete]   │
│                 ─────────────────────────────────────────────────   │
│                 [rendered comment body]                               │
└──────────────────────────────────────────────────────────────────────┘
```

- Edit/Delete icons: only shown on authenticated user's own comments; appear on comment hover
- Author name: `type.ui.default` bold, `text.primary`
- Timestamp: `type.ui.small`, `text.secondary`
- Body: Markdown rendered
- Separator between each comment: 8px gap, subtle separator line

### Add Comment Box

```
┌──────────────────────────────────────────────────────────────────────┐
│  [avatar 24px]  ┌────────────────────────────────────────────────┐  │
│                 │ Leave a comment...                              │  │
│                 │                                                 │  │
│                 └────────────────────────────────────────────────┘  │
│                 [Preview]                         [Post Comment]     │
└──────────────────────────────────────────────────────────────────────┘
```

- Text area: multi-line `JBTextArea`, auto-expands up to 6 lines, then scrolls
- Preview toggle: switches between edit and rendered-Markdown preview
- Post button: disabled when empty; primary action style
- Shown at the bottom of the comment list, always visible (not behind a "+" button)

### Edit Mode (Inline)

When editing an existing comment:
- The rendered body swaps to the same text area component, pre-filled
- Save / Cancel replace the Edit/Delete icons inline
- No dialog — fully inline replacement

---

## 12. Auth & Onboarding Flow

Shown when the tool window loads and the user is not authenticated.

### Unauthenticated State (Tool Window)

```
┌──────────────────────────────────────────────────────────────────────┐
│  Remote VCS                                                          │
│  ─────────────────────────────────────────────────────────          │
│                                                                      │
│               [GitHub Logo 32px]                                     │
│                                                                      │
│           Connect to GitHub                                          │
│           Manage issues, PRs, and branches                           │
│           directly from your IDE.                                    │
│                                                                      │
│           [    Sign in with GitHub    ]   ← primary button          │
│                                                                      │
│           Or use a Personal Access Token ↗  ← small text link       │
│                                                                      │
└──────────────────────────────────────────────────────────────────────┘
```

### OAuth Device Flow — In Progress State

After clicking "Sign in with GitHub":

```
┌──────────────────────────────────────────────────────────────────────┐
│  ← Cancel                                                            │
│                                                                      │
│     Open this URL in your browser:                                   │
│     github.com/login/device                                          │
│                                                                      │
│     Then enter this code:                                            │
│     ┌─────────────────────────────┐                                  │
│     │   ABCD - 1234               │  ← monospace, large, selectable  │
│     └─────────────────────────────┘                                  │
│     [Copy Code]   [Open Browser]                                     │
│                                                                      │
│     ⏳ Waiting for authorization...   ← animated spinner             │
│                                                                      │
│     Code expires in 14:32            ← countdown timer              │
└──────────────────────────────────────────────────────────────────────┘
```

- User code displayed in a large, selectable, monospace font — easy to type or copy
- "Open Browser" opens the device verification URL in the system browser automatically (the user just has to approve, not type the URL)
- Countdown timer for code expiry
- On success: brief checkmark animation → panel refreshes to main view

### PAT Flow (settings-accessible alternative)

Inline in the tool window or redirected to Settings:
- PAT field (password-masked), "Validate & Save" button
- Shows success state ("Token stored · Connected as alph-a07 [avatar]") inline below the field

---

## 13. Settings Panel

**Current state is decent** — `Other Settings → Remote VCS Manager` is correctly positioned. Needs refinement:

### Redesigned Layout

```
Remote VCS Manager
─────────────────────────────────────────────────────────────────────

  CURRENT REPOSITORY
  ──────────────────────────────────────────────────────────────────
  [GitHub logo 16px]  finsible / android-client
  Remote: origin  ·  Branch: main  ·  https://github.com/finsible/android-client.git

  AUTHENTICATION
  ──────────────────────────────────────────────────────────────────
  Connected as:  [avatar 20px] alph-a07  [Disconnect]

  — or use a Personal Access Token —

  Personal Access Token   [•••••••••••••••••••••••••] [Validate & Save]
                          Token stored ✓

  PREFERENCES
  ──────────────────────────────────────────────────────────────────
  ☑ Auto-refresh on project open
  ☑ Show notification for assigned issues
  ☑ Show notification for review requests

  Default issue filter:    [All open issues  ▾]
  Fetch limit:             [50            ] issues per query
  Refresh interval:        [5 min         ▾]

  ADVANCED
  ──────────────────────────────────────────────────────────────────
  GitHub API base URL:     [https://api.github.com         ]
                           ↑ Override for GitHub Enterprise

  QUICK ACTIONS
  ──────────────────────────────────────────────────────────────────
  [Manage Repositories]   [Manage Tokens]   [Clear Cache]
```

**Changes from current:**
- "Current Repository" is now clearly formatted, not a raw data dump
- "Connected as" shows avatar + username — the user can see who they're authenticated as
- Section headers use the same `type.header` pattern as the rest of the plugin
- Advanced section houses the GitHub Enterprise API URL override
- "Clear Cache" quick action added — useful for debugging
- Visual section anchoring via `type.header` + separator instead of bare whitespace

---

## 14. Dialogs

### 14.1 — Create Issue Dialog

```
┌─────────────────────────────────────────────────────────┐
│  New Issue                                          [✕]  │
├─────────────────────────────────────────────────────────┤
│  Title                                                   │
│  [........................................................] │
│                                                          │
│  Description              [Write] [Preview]             │
│  [........................................................] │
│  [........................................................] │
│  [........................................................] │
│  Markdown supported                                      │
│                                                          │
│  Labels                   Assignees        Milestone     │
│  [+ Add labels]           [+ Add assignees][+ Milestone] │
│                                                          │
├─────────────────────────────────────────────────────────┤
│                              [Cancel]  [Create Issue]   │
└─────────────────────────────────────────────────────────┘
```

- Width: `JBUI.scale(560)`
- Uses `DialogWrapper` — standard IntelliJ dialog base class; gets free OK/Cancel button bar, ESC handling, modal behavior
- Title field auto-focuses on open
- Labels/Assignees/Milestone: each opens a searchable popup picker on click
- Create button: disabled until Title is non-empty
- Keyboard: Ctrl+Enter submits

### 14.2 — Create Branch Dialog

```
┌────────────────────────────────────────────────┐
│  Create Branch                             [✕]  │
├────────────────────────────────────────────────┤
│  Branch name                                   │
│  [feature/99-revamp-new-transaction-ux      ]  │
│  ✓ Valid branch name                           │
│                                                │
│  Base branch                                   │
│  [main ▾]                                      │
│                                                │
│  Link to issue  (optional)                     │
│  [⬡ #99 Revamp New Transaction for faster UX] │
│                                                │
├────────────────────────────────────────────────┤
│                    [Cancel]  [Create Branch]   │
└────────────────────────────────────────────────┘
```

- Pre-fills branch name from issue context when opened from issue detail
- Branch name validation: live, inline below the field — valid character check, no spaces check, duplicate name check
- Base branch dropdown: lists remote branches, filterable

### 14.3 — Merge PR Confirmation Balloon

Not a full dialog — a `JBPopup` confirmation balloon anchored to the Merge button:

```
  ┌─────────────────────────────────────────┐
  │  Merge pull request #101?               │
  │  Squash and merge · 3 commits           │
  │                                         │
  │  [Cancel]         [Confirm Merge]       │
  └─────────────────────────────────────────┘
  ↑ anchored below merge button
```

---

## 15. State System (Loading / Empty / Error)

### Loading State

Used during initial data fetch or explicit refresh:

- **List panel:** Skeleton rows — 5-8 placeholder rows with animated shimmer gradient over gray placeholder bars where text would be. Height matches normal rows. No spinner in center.
- **Detail panel:** Skeleton layout — placeholder bars where title, badges, metadata would be.
- **Shimmer animation:** Horizontal sweep of lighter tone over `bg.elevated` placeholder bars, ~1.5s cycle, Swing Timer-driven or `AnimatedIcon` equivalent.
- **Toolbar:** Refresh button shows a spinning `AllIcons.Actions.Refresh` icon during load; returns to static when done.

### Empty States

Per-context empty state messages:

| Context | Icon | Title | Subtitle | CTA |
|---|---|---|---|---|
| No open issues | `AllIcons.General.TodoDefault` | "No open issues" | "Issues you open will appear here." | "+ New Issue" |
| No closed issues | `AllIcons.General.TodoDefault` | "No closed issues" | "Closed issues will appear here." | — |
| No open PRs | `AllIcons.Vcs.Merge` | "No open pull requests" | "Branches ready for review will appear here." | — |
| No branches | `AllIcons.Vcs.Branch` | "No branches found" | "Create a branch to get started." | — |
| No comments | `AllIcons.Actions.InlayRenameInComments` | "No comments yet" | "Be the first to comment." | — |
| Search returned nothing | `AllIcons.General.Search` | "No results for \"{query}\"" | "Try a different search term." | "Clear search" |

### Error States

Inline error banner below the filter bar, above the list:

```
  ┌────────────────────────────────────────────────────────────────┐
  │  ⚠  Failed to load issues · GitHub API returned 403           │
  │     Rate limit exceeded. Resets in 4 minutes.     [Retry]     │
  └────────────────────────────────────────────────────────────────┘
```

- Background: `accent.warning` @ 10% alpha
- Border-left: 3px `accent.warning` solid
- Icon: `AllIcons.General.Warning`
- Message: plain text, specific to the error
- Retry button: right-aligned; re-triggers the failed fetch
- Dismissable: `[✕]` in top-right corner of the banner
- Auth error (401): special case — shows "Reconnect" button instead of "Retry"; clicking opens auth flow

---

## 16. Interactions & Motion

### Principles

- **Functional motion only** — no decorative animations. Every animation communicates state change.
- **Short durations** — developers are impatient; 100–200ms is the sweet spot. Nothing over 300ms.
- **Swing-compatible** — achieved via Swing `Timer`, `AnimatedIcon`, or `Animator` utility

### Motion Catalog

| Trigger | Animation | Duration |
|---|---|---|
| Tab switch | Instant — no animation. Speed is more important here. | 0ms |
| Row hover | Background color fade in/out | 80ms |
| Inline action toolbar appear | Opacity fade in | 100ms |
| Detail panel content load (skeleton → real) | Opacity crossfade | 150ms |
| Refresh button click | Icon starts spinning | Immediate, lasts until data returns |
| OAuth success | Brief green checkmark replaces spinner | 200ms |
| Comment post (optimistic) | New comment slides in from bottom | 150ms |
| Badge state change (close issue) | Badge color + text updates in-place | 100ms |
| Error banner appear | Slide down from above list | 150ms |
| Error banner dismiss | Fade out | 100ms |
| Merge button confirm balloon | Fade + scale from anchor point | 120ms |

### Keyboard Navigation

| Key | Action |
|---|---|
| `↑` / `↓` | Navigate list rows |
| `Enter` | Open detail for selected row |
| `Esc` | Close detail (single-panel) / deselect row |
| `⌘R` / `Ctrl+R` | Refresh current panel |
| `/` | Focus search field |
| `N` | New issue / new PR (context dependent) |
| `⌘Enter` / `Ctrl+Enter` | Submit forms (create issue, post comment) |
| `Tab` | Navigate between fields in dialogs |

---

## 17. Observed Issues & Fixes

A direct mapping from the current screenshots to what needs to change:

### Issue 1 — Raw `GitRemoteInfo(...)` header string
**Screenshot:** Image 1, 2, 3 — header shows raw Kotlin `toString()` output  
**Fix:** Parse and display as `owner / repoName` with `remoteName · branch` subtitle. Section 6.1 covers this fully.

### Issue 2 — State column truncating to `O...` and `C...`
**Screenshot:** Image 1 — `● O...`, `● C...` in state column  
**Fix:** Replace with `StateBadge` component in a fixed 56px column. Never truncates. Section 5.1 and 7.1.

### Issue 3 — PR status column showing `● ...` (completely unreadable)
**Screenshot:** Image 2 — `St...` header, `● ...` in every row  
**Fix:** Same `StateBadge` in fixed 68px column with full "Open", "Draft", "Merged" text. Section 8.1.

### Issue 4 — Branches list is raw flat data
**Screenshot:** Image 3 — 50+ branches in a flat list with just name + SHA  
**Fix:** Grouped lis[src](../src)t with default / with-open-PRs / all grouping; contextual chips; shortened SHA chip; linked issue/PR detection. Section 9.1.

### Issue 5 — No detail panel
**Inferred:** Clicking a row in any of the three lists appears to do nothing visible  
**Fix:** Split panel with `JBSplitter`; detail panels fully specced in Section 10.

### Issue 6 — Settings panel has large empty space
**Screenshot:** Image 4 — "Remote VCS Manager" label followed by a huge whitespace gap  
**Fix:** Section content starts directly under the section heading with no gap. Section 13.

### Issue 7 — Settings "Current Repository" section is absent in the visible area
**Screenshot:** Image 4 — the `Current Repository` block appears far below the heading  
**Fix:** Tighten vertical spacing throughout the settings panel; sections flow directly after their header with `space.4` gap max.

### Issue 8 — No visual hierarchy in tables
**All screenshots:** All rows are identical visual weight; no distinction between primary info (title) and secondary info (timestamp, author)  
**Fix:** Title in `text.primary` full weight; author, timestamps, SHA in `type.ui.small` `text.secondary`. Weight contrast creates natural scanning hierarchy.

### Issue 9 — No empty state / no skeleton loader visible
**Inferred:** When loading, the panel is likely either blank or frozen  
**Fix:** Full skeleton loading system with shimmer; contextual empty states. Section 15.

---

*Spec version 1.0 — covers GitHub-first implementation, designed to extend to GitLab and ADO without visual redesign*