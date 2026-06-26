# Anchor Project Context

## Durable Intent

Anchor is a JetBrains plugin extension that keeps GitHub repository work inside the IDE through a native Anchor tool window, IDE-theme-aware UI, JetBrains GitHub integration, and JCEF-backed detail rendering.

The Superpowers Project layer turns this fork into a durable implementation system. It records roadmap intent, tracker rules, artifact paths, and execution expectations so agents can move from product ideas to specs, plans, issue mirrors, implementation branches, and verification without losing project context.

## Artifact Model

Canonical artifacts live under `docs/superpowers`:

- Project context: `docs/superpowers/PROJECT_CONTEXT.md`
- Milestone indexes: `docs/superpowers/milestones/`
- Loose specs: `docs/superpowers/specs/`
- Implementation plans: `docs/superpowers/plans/`
- GitHub issue mirrors: `docs/superpowers/issues/`

The artifact lifecycle is `spec -> plan -> issue`.

Specs capture product direction, design decisions, architecture notes, and PRD-style requirements. Plans turn approved specs or issue needs into executable implementation design. Issue mirrors are the official implementation records and must include tracker metadata, milestone ownership, acceptance criteria, proof oracle, AFK/HITL classification, branch, and worktree execution fields.

Milestone pages are index views. They may list related specs, plans, and issues, but canonical copies stay in the flat roots above.

## Roadmap And Milestones

- [M1 - Issue Workflow Hardening](milestones/M1-issue-workflow-hardening.md): make the multi-repository issue browser dependable, sortable, and comfortable for repeated IDE use.
- [M2 - Pull Request Workflow Parity](milestones/M2-pull-request-workflow-parity.md): bring PR browsing, detail rendering, comments, review state, commits, and branch actions to the same quality bar as issues.
- [M3 - Provider And Enterprise Readiness](milestones/M3-provider-and-enterprise-readiness.md): harden provider boundaries, GitHub Enterprise behavior, authentication, and future provider paths.
- [M4 - Marketplace Quality And Release Operations](milestones/M4-marketplace-quality-and-release-operations.md): keep plugin packaging, compatibility, screenshots, changelog, and release verification trustworthy.

## GitHub Tracker Config

- Canonical tracker repository: `tannerpolley/anchor`
- Canonical remote name: `origin`
- Source upstream repository: `alph-a07/anchor`; use it as an external reference, not as the tracker target.
- Issue mirror path: `docs/superpowers/issues/<issue-number>-<slug>.md`
- Source spec path: `docs/superpowers/specs/<slug>.md`
- Source plan path: `docs/superpowers/plans/<slug>.md`

Recommended GitHub issue labels:

- Type: `type:bug`, `type:feature`, `type:task`, `type:docs`, `type:refactor`
- Area: `area:auth`, `area:issues`, `area:pull-requests`, `area:markdown`, `area:settings`, `area:theme`, `area:release`
- Priority: `priority:p0`, `priority:p1`, `priority:p2`
- Status: `status:ready`, `status:in-progress`, `status:blocked`, `status:needs-review`

Recommended GitHub milestone titles:

- `M1 - Issue Workflow Hardening`
- `M2 - Pull Request Workflow Parity`
- `M3 - Provider And Enterprise Readiness`
- `M4 - Marketplace Quality And Release Operations`

Native GitHub issue types must be inspected through GraphQL before assigning them. Keep compatibility labels such as `type:bug`, `type:feature`, and `type:task` even when native issue types are enabled.

AFK/HITL policy:

- AFK work: local code, tests, docs, static validation, issue mirror updates, and plans that do not require external account action.
- HITL work: GitHub Project board creation, Marketplace publishing, credential changes, scope decisions, destructive Git actions, and manual IDE or account verification.

Goal execution criteria:

- Issue work starts from a native `/goal` or goal-tool activation.
- Each issue mirror must name the target branch, worktree expectations, acceptance criteria, and proof oracle before implementation starts.
- Branch names should use `codex/<issue-slug>` unless a user or issue mirror specifies another branch.

## Execution Model

Agents should read this context before creating specs, writing plans, splitting issues, or implementing issue work.

Implementation work should stay small enough to verify with targeted Gradle checks. Typical proof commands are:

- `.\gradlew.bat test`
- `.\gradlew.bat verifyPlugin`
- `.\gradlew.bat buildPlugin`

Use the cheapest proof that covers the changed behavior. Escalate to `verifyPlugin` or `buildPlugin` when plugin metadata, compatibility, packaging, or JetBrains platform integration changes.

## Extension Skills

Use these skills by canonical name:

- `$superpowers-project:setup-project`
- `$superpowers-project:brainstorm-spec`
- `$superpowers-project:write-plan`
- `$superpowers-project:create-issues`
- `$superpowers-project:implement-plan`
- `$superpowers-project:resolve-issue`
- `$superpowers-project:orchestrate-issues`
- `$superpowers-project:merge-changes`
- `$superpowers-project:align-project`
- `$superpowers-project:audit-project`

## Current Open Questions

- Which recommended labels and milestone titles should be created in the fork tracker?
- Should this fork get an optional GitHub Project board for milestone/status views?
- Which recently completed issue-browser changes should be backfilled into issue mirrors, if any?
