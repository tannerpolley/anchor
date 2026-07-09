---
title: Linux Development Toolchain Migration
milestone: M4 - Marketplace Quality and Release Operations
status: approved-design
owner: codex
date: 2026-07-09
---

# Linux Development Toolchain Migration

## Project Context Evidence

- The repository contains five PowerShell-only validators under `scripts/` and no Bash equivalents.
- The repository tracks both the Unix Gradle launcher, `gradlew`, and the Windows launcher, `gradlew.bat`.
- `docs/superpowers/PROJECT_CONTEXT.md`, two existing specs, two existing plans, and four issue mirrors prescribe PowerShell or `gradlew.bat` commands.
- Both GitHub Actions workflows already run on Ubuntu, but they do not enforce strict Bash mode or run the repository validators.
- The release workflow builds and publishes without running the full test and plugin-verification gates first.
- The transferred working tree has LF line endings for nearly all tracked text files, but the repository has no `.gitattributes` or `.editorconfig` policy to preserve that state.
- `build.gradle.kts` targets JVM 21 while `CONTRIBUTING.md` still lists JDK 17 as sufficient.
- Runtime tests contain Windows-style path fixtures that exercise cross-platform plugin behavior. Those fixtures are not development-tooling dependencies.

## Problem

Anchor's development workflow still depends on Windows launchers, PowerShell scripts, and Windows command examples after the repository moved to Zorin OS. Contributors can build with `./gradlew`, but they cannot run the repository's Superpowers artifact validators without PowerShell. The repository also lacks a committed line-ending policy, so another checkout can recreate the full-file CRLF/LF churn visible after the transfer.

The cutover needs one Linux-native command path for local development, CI, release validation, and project documentation. Anchor itself should remain usable on all JetBrains-supported operating systems.

## Goals

- Replace the five PowerShell validators with executable Bash scripts.
- Remove `gradlew.bat` and all repository-owned PowerShell scripts.
- Convert repository-owned build and validation instructions to `./gradlew` and Bash commands.
- Preserve cross-platform plugin runtime behavior and runtime path fixtures.
- Enforce LF text files, UTF-8, final newlines, and stable indentation settings.
- Add automated positive and negative tests for the translated validators.
- Run the Linux validator and Gradle proof paths in GitHub Actions before merge and release.
- Update development requirements to JDK 21 and document the Linux command-line dependencies.
- Fail on missing inputs, missing dependencies, malformed data, invalid artifacts, and incomplete release metadata.

## Non-Goals

- Restricting Anchor's runtime support to Linux.
- Removing Windows path handling from plugin code or portability tests.
- Editing the generated compatibility branches inside `gradlew`.
- Adding Make, Just, or another task runner.
- Changing plugin features, UI behavior, provider logic, or settings.
- Installing operating-system packages automatically.
- Launching a sandbox IDE or reloading the plugin, because this migration does not change plugin code or UI.

## Approaches Considered

### Design 1: Complete Linux Toolchain Replacement

Translate each validator to strict Bash, remove the Windows scripts and launcher, update all repository-owned commands, commit line-ending policy, add validator regression tests, and make Linux CI run the same commands documented for local use.

This approach completes the cutover and adds proof for the regex-heavy validator translations. It introduces one explicit command-line dependency, `jq`, for reliable JSON validation.

### Design 2: Mechanical Conversion

Translate and rename the scripts, remove `gradlew.bat`, and update documentation without adding validator tests or CI gates.

This produces a smaller change. It leaves no automated evidence that the Bash validators accept valid artifacts and reject invalid ones, so future edits can silently weaken the project workflow gates.

### Design 3: Linux Task Runner

Add Make or Just targets that wrap validation, testing, packaging, and release checks after translating the scripts.

This gives contributors short commands. It also adds a new dependency and hides the Gradle and validator interfaces behind another layer without solving a current coordination problem.

## Recommended Approach

Use Design 1. Keep the command path direct: executable scripts for artifact validation and `./gradlew` for plugin work. Add regression coverage and CI gates without introducing a task runner.

## Architecture

The Linux toolchain has four boundaries.

1. Bash validators own one artifact contract each. They parse GNU-style long options, validate inputs, collect semantic errors, print all validation failures to standard error, and return a non-zero status.
2. A Bash regression runner owns temporary fixtures and exit-status assertions for the validators. It removes its temporary directory through an exit trap.
3. GitHub Actions owns continuous proof. Pull-request and branch CI run shell checks, validator tests, Gradle tests, plugin verification, and plugin packaging on Ubuntu. Release automation runs the same gates before publishing.
4. Repository policy files own checkout consistency. `.gitattributes` enforces LF for text and protects binary files. `.editorconfig` aligns editors with UTF-8, final newlines, and file-type indentation.

The plugin runtime stays outside these boundaries.

## Components

### Decision Ledger Validator

`scripts/validate-decision-ledger.sh` accepts `--path <file>` and optional `--kind <spec|plan|issue>`. It verifies the Decision Ledger section, required table columns, at least one data row, valid `Yes` or `No` deferred values, and absence of placeholder tokens.

### Issue Mirror Validator

`scripts/validate-issue-mirror.sh` accepts `--issue-file <file>`. It verifies that the resolved file lives under `docs/superpowers/issues`, checks required metadata and sections, confirms acceptance checkboxes, and resolves the referenced source plan from the repository root.

### Plan Outcome Validator

`scripts/validate-plan-outcome-proof.sh` accepts `--plan-path <file>`. It verifies the Outcome Proof and Implementation Boundaries sections and their required fields.

### Plan Task Use-Case Validator

`scripts/validate-plan-task-use-cases.sh` accepts `--plan-path <file>`. It finds numbered task sections and checks that each task has a concrete Use Cases block before its Files block.

### Workflow Mode Ledger Validator

`scripts/validate-workflow-mode-ledger.sh` accepts `--repo-root <directory>` and `--mode-ledger-path <file>`. It uses `jq -e` to reject malformed JSON, verify required fields and fixed values, resolve source spec and plan paths, and check required stop conditions.

### Validator Regression Runner

`scripts/test-validators.sh` runs `bash -n` over the Bash scripts, creates isolated valid and invalid fixtures, and asserts each validator's successful and failing paths. It prints the failed command and captured output when an assertion fails.

### Repository Text Policy

`.gitattributes` marks text as LF and lists binary formats that Git must not normalize. `.editorconfig` sets UTF-8, LF, final newlines, trimmed trailing whitespace, four-space Kotlin and shell indentation, and two-space YAML indentation.

### Linux CI

`.github/workflows/ci.yml` runs on pushes and pull requests. It checks out the repository, validates the Gradle wrapper, installs JDK 21, configures Gradle, checks required Linux tools, runs ShellCheck and the validator regression runner, then runs Gradle tests, plugin verification, and plugin packaging.

The PR compliance and release workflows use explicit Bash shells, strict mode, quoted variables, narrow permissions, and bounded job timeouts. The release workflow checks the tag's changelog entry and completes the proof gates before Marketplace publication or GitHub release creation.

## Data Flow

1. A contributor checks out the repository with LF line endings and executable Bash scripts.
2. The contributor creates or edits a Superpowers spec, plan, issue mirror, or workflow ledger.
3. The matching Bash validator parses command-line options and resolves the requested artifact.
4. The validator checks structure and semantics, then returns success or prints the collected errors and returns failure.
5. `scripts/test-validators.sh` exercises the validator contracts against temporary fixtures.
6. GitHub Actions runs the validator suite and Gradle proof commands on the same Linux shell path.
7. A tag-triggered release verifies its changelog entry, tests, plugin compatibility, and distributable package before publishing.

## Error Handling

- Each script starts with `set -Eeuo pipefail`.
- Missing option values, unknown options, and missing required options print usage and return status 2.
- Missing files, directories, or external tools return status 1 with the failing path or dependency name.
- Validators collect independent artifact errors so one run reports all relevant defects.
- The JSON validator treats malformed JSON as a validation failure and does not attempt text-based JSON parsing.
- Release metadata extraction fails when the tag has no exact changelog heading.
- CI steps do not suppress validator, ShellCheck, test, verification, or packaging failures.
- The migration does not add compatibility wrappers, aliases, or fallback execution paths.

## Documentation Cutover

Update active instructions and reusable historical workflow artifacts. Convert PowerShell code fences to Bash, replace backslash path separators with forward slashes, replace `gradlew.bat` with `./gradlew`, and replace PowerShell cleanup commands with the user-level Bash cleanup command where the artifact documents cleanup proof.

Do not rewrite URLs, product names, GitHub issue links, changelog prose, license text, generated Gradle launcher compatibility code, or runtime portability fixtures merely because they contain the word `Windows` or a Windows-style path.

## Testing and Proof Oracles

Static proof:

- `bash -n scripts/*.sh`
- `shellcheck scripts/*.sh`
- A repository scan finds no tracked `.ps1`, `.bat`, or `.cmd` files.
- A repository scan finds no repository-owned PowerShell commands or `gradlew.bat` instructions outside generated or runtime-portability exceptions.
- Git modes show `100755` for executable Bash scripts and `gradlew`.

Validator proof:

- `./scripts/test-validators.sh`
- `./scripts/validate-decision-ledger.sh --path docs/superpowers/specs/2026-07-09-linux-development-toolchain-design.md --kind spec`
- Run the issue, plan, task-use-case, and workflow-ledger validators against the repository's canonical artifacts.

Gradle proof:

- `./gradlew --no-daemon test`
- `./gradlew --no-daemon verifyPlugin`
- `./gradlew --no-daemon buildPlugin`

Workflow proof:

- Review workflow YAML for Linux runners, explicit Bash, strict mode, permissions, timeouts, and pre-publish proof order.
- Confirm the release changelog check fails when a tag has no exact version heading.

Completion proof:

- Run `bash "$HOME/.codex/hooks/codex-cleanup.sh" --repo-root .` from the repository root.
- Report intentional Windows references in runtime fixtures and generated `gradlew` compatibility code as retained cross-platform behavior.

## Open Questions

None. The user approved the toolchain-only scope, complete replacement approach, component boundaries, and validation design. Auto mode authorizes the recommended downstream planning and implementation route.

## Decision Ledger

| Decision | Source | Answer | Impact | Deferred? | Risk owner |
|---|---|---|---|---|---|
| Platform scope | User native answer | Convert repository development and build tooling to Debian-family Linux while keeping Anchor runtime support cross-platform. | Keeps plugin behavior and portability fixtures outside the migration. | No | User |
| Migration approach | User native answer | Use Design 1, Complete Linux Toolchain Replacement. | Requires script translation, deletion of Windows tooling, documentation cutover, tests, and CI gates. | No | User |
| Downstream approval | User direct instruction | Use auto mode and approve the remaining recommended workflow decisions. | Allows one spec, one plan, and inline implementation without more approval prompts. | No | User |
| Validator shell | Approved design | Use Bash with strict mode and GNU-style long options. | Creates one Linux-native interface with loud option and validation failures. | No | Codex |
| JSON dependency | Approved design | Use `jq` for workflow-ledger validation. | Avoids unreliable JSON parsing in shell and adds one documented Linux dependency. | No | Codex |
| Windows tool removal | Approved design | Delete all `.ps1` validators and `gradlew.bat` without compatibility wrappers. | Removes Windows-only development paths from the repository. | No | Codex |
| Gradle launcher | Repo evidence and approved design | Keep the generated Unix `gradlew` unchanged, including its Cygwin and MSYS compatibility branches. | Preserves Gradle wrapper integrity while using only `./gradlew` in repository workflows. | No | Codex |
| Runtime support | User native answer | Retain Windows-style runtime path fixtures and cross-platform bug-report choices. | Prevents a tooling migration from narrowing the plugin's supported operating systems. | No | User |
| Line endings | Repo evidence and approved design | Commit LF policy through `.gitattributes` and `.editorconfig`. | Prevents repeat CRLF/LF churn after the transferred working tree is normalized. | No | Codex |
| CI proof | Approved design | Add Linux CI and strengthen PR and release Bash gates. | Makes documented local commands match continuous and release validation. | No | Codex |
| Release order | Approved design | Validate changelog, shell tooling, tests, plugin compatibility, and packaging before publication. | Stops incomplete tag metadata or failed proof before external publication begins. | No | Codex |
| Task runner | Design comparison | Do not add Make or Just. | Keeps command ownership in Bash validators and Gradle. | No | Codex |
| IDE reload | Project instructions and approved scope | Do not run the plugin install/reload workflow for tooling-only changes. | Avoids an irrelevant IDE mutation while retaining Gradle packaging proof. | No | Codex |
