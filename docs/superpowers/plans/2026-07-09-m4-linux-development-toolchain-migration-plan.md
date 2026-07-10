# Linux Development Toolchain Migration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace Anchor's Windows-only development tooling with one tested Debian-family Linux and Bash workflow while preserving cross-platform plugin runtime behavior.

**Architecture:** Keep artifact validation in five focused executable Bash scripts and exercise them through one isolated Bash regression runner. Enforce LF through repository policy, make Ubuntu CI and release automation run the same shell and Gradle commands documented for local development, and remove the replaced PowerShell and batch entry points.

**Tech Stack:** Bash 5, GNU coreutils, GNU grep/sed/awk, jq, ShellCheck, Gradle Wrapper, Kotlin/JVM 21, GitHub Actions

---

## Source and Route Evidence

- **Source Spec:** `docs/superpowers/specs/2026-07-09-linux-development-toolchain-design.md`
- **Milestone:** `docs/superpowers/milestones/M4-marketplace-quality-and-release-operations.md`
- **Workflow Mode Ledger:** `.superpowers/runs/20260709-linux-development-toolchain/workflow-mode-ledger.json`
- **Auto Mode Authorization:** `.superpowers/runs/20260709-linux-development-toolchain/auto-mode-authorization.json`
- **Execution Route:** `$superpowers-project:implement-plan` in the current thread on `codex/linux-development-toolchain-migration`
- **Issue Route:** No issue mirror. The user selected one direct implementation route.
- **Publish Route:** Local commits only. Do not push, publish, create a release, or create a pull request.

## Outcome Proof

**Intent:** Give Anchor contributors one Debian-family Linux command path for artifact validation, testing, packaging, and release preparation.
**Current Behavior:** Five repository validators require PowerShell, `gradlew.bat` remains tracked, reusable project artifacts prescribe Windows commands, and the repository has no committed LF policy or Linux build CI.
**Expected Outcome:** Executable Bash validators replace PowerShell, documentation and automation use Linux commands, forbidden Windows development entry points are absent, CI proves the Linux path, and runtime OS support remains unchanged.
**Target Output:** Six executable Bash files, `.gitattributes`, `.editorconfig`, one Linux CI workflow, hardened existing workflows, migrated documentation, and deletion of five `.ps1` files plus `gradlew.bat`.
**Owner:** The Anchor repository maintainer owns the Linux toolchain; each validator owns the artifact contract named by its filename.
**Interface:** Contributors run `./scripts/<validator>.sh` with GNU-style long options and run plugin checks through `./gradlew`.
**Cutover:** Documentation, CI, and release automation switch to Bash in the same change that deletes the PowerShell and batch files.
**Replaced Path:** Delete `scripts/*.ps1` and `gradlew.bat`; do not leave wrappers, aliases, redirects, or compatibility copies.
**Evidence:** Validator positive/negative receipts, `bash -n`, ShellCheck, zero-result Windows-tooling scans, Gradle test/verification/package receipts, executable Git modes, and cleanup-hook output.
**Acceptance Proof:** All commands in the Proof Oracle pass, tracked Windows development files equal zero, and remaining Windows references are limited to runtime portability fixtures, the cross-platform bug template, URLs/prose, and generated `gradlew` compatibility code.
**Stop Criteria:** Stop if a translated validator disagrees with an existing valid artifact, any required tool is unavailable, Gradle verification fails, workflow semantics require an unapproved external action, or unrelated user content would need replacement.
**Avoid:** Product code changes, runtime OS restrictions, generated `gradlew` edits, package auto-installation, task-runner layers, IDE sandbox launch, plugin reload, GitHub publication, and push operations.
**Risk:** Bash regex and Markdown table parsing can drift from the PowerShell behavior. Characterisation fixtures and canonical artifact validation own that risk; GitHub remains the final interpreter of workflow YAML.

## Implementation Boundaries

**Files To Create:** `.gitattributes`, `.editorconfig`, `.github/workflows/ci.yml`, `scripts/validate-decision-ledger.sh`, `scripts/validate-issue-mirror.sh`, `scripts/validate-plan-outcome-proof.sh`, `scripts/validate-plan-task-use-cases.sh`, `scripts/validate-workflow-mode-ledger.sh`, and `scripts/test-validators.sh`.
**Files To Modify:** `.github/workflows/pr_compliance.yml`, `.github/workflows/publish-release.yml`, `README.md`, `CONTRIBUTING.md`, `docs/superpowers/PROJECT_CONTEXT.md`, two earlier specs, two earlier plans, and issue mirrors 1, 3, 4, and 5.
**Files To Avoid:** `src/main/**`, runtime path tests under `src/test/**`, `.github/ISSUE_TEMPLATE/bug_report.yml`, `gradlew`, Gradle wrapper JAR/properties, plugin metadata, changelog content, license text, and unrelated IDE files.
**Source Of Truth:** The approved source spec defines the platform boundary; the existing PowerShell validators define the validation semantics during translation.
**Read Path:** Validator options identify one input artifact; the issue and workflow validators also resolve referenced repository paths from the supplied or current repository root.
**Write Path:** Validators write messages only to standard output or standard error. The test runner writes fixtures under `mktemp -d` and removes them through `trap`.
**Integration Points:** Superpowers specs/plans/issues, Auto Mode JSON ledgers, GitHub pull-request CI, tag release automation, Gradle tests, JetBrains plugin verification, and plugin packaging.
**Migration Or Cutover:** Add and prove Bash replacements before deleting Windows files, then migrate all reusable commands and wire CI to the new scripts.
**Replaced Path Handling:** Repository scans reject tracked `.ps1`, `.bat`, `.cmd`, PowerShell invocations, and `gradlew.bat` documentation. Intentional runtime and generated-wrapper references use an explicit allowlist in the scan.
**Acceptance Proof Gate:** Run all static, validator, Gradle, workflow-review, Git-mode, scan, and cleanup commands before claiming completion.

## Test-Complete Definition and Metrics

- All six Bash scripts pass `bash -n` and ShellCheck with zero findings.
- Each validator has at least one success fixture and one failure fixture; every assertion must pass.
- All applicable canonical specs, plans, issue mirrors, and workflow ledgers pass their matching Bash validator.
- `./gradlew --no-daemon test`, `verifyPlugin`, and `buildPlugin` return status 0.
- Forbidden tracked Windows development files and commands have zero matches after allowlisted runtime/generated cases are excluded.
- Bash scripts and `gradlew` have Git mode `100755`.
- Tolerances and numerical error bounds do not apply because each gate has a deterministic pass/fail result.

## Non-Goals

- Narrow Anchor runtime support to Linux.
- Replace runtime Windows path fixtures.
- Edit the generated Unix Gradle launcher.
- Add a task runner or automatic package installer.
- Change plugin behavior or perform IDE visual verification.

## Proof Oracle

```bash
bash -n scripts/*.sh
shellcheck scripts/*.sh
./scripts/test-validators.sh
./scripts/validate-decision-ledger.sh --path docs/superpowers/specs/2026-07-09-linux-development-toolchain-design.md --kind spec
./scripts/validate-plan-outcome-proof.sh --plan-path docs/superpowers/plans/2026-07-09-m4-linux-development-toolchain-migration-plan.md
./scripts/validate-plan-task-use-cases.sh --plan-path docs/superpowers/plans/2026-07-09-m4-linux-development-toolchain-migration-plan.md
./scripts/validate-decision-ledger.sh --path docs/superpowers/plans/2026-07-09-m4-linux-development-toolchain-migration-plan.md --kind plan
./scripts/validate-issue-mirror.sh --issue-file docs/superpowers/issues/1-render-github-issues-in-editor-preview-and-group-by-milestone.md
./scripts/validate-workflow-mode-ledger.sh --repo-root . --mode-ledger-path .superpowers/runs/20260709-linux-development-toolchain/workflow-mode-ledger.json
./gradlew --no-daemon test
./gradlew --no-daemon verifyPlugin
./gradlew --no-daemon buildPlugin
bash "$HOME/.codex/hooks/codex-cleanup.sh" --repo-root .
```

### Task 1: Commit Linux Text and Editor Policy

**Use Cases:**
- A Linux checkout keeps shell, Kotlin, Markdown, YAML, XML, and Gradle text files on LF.
- Git does not normalize wrapper JARs, images, archives, or other binary assets.
- Editors use UTF-8, final newlines, and repository-appropriate indentation.
- The existing transferred LF conversion becomes intentional without changing unrelated user content.

**Files:**
- Create: `.gitattributes`
- Create: `.editorconfig`

- [ ] **Step 1: Prove policy is missing**

```bash
test -f .gitattributes && test -f .editorconfig
```

Expected: status 1 because neither policy file exists.

- [ ] **Step 2: Add exact normalization policy**

Use `* text=auto eol=lf`; mark `*.jar`, `*.zip`, raster images, and font files as binary. Set `root = true`, UTF-8, LF, final newline, trailing-whitespace trimming, four spaces by default, two spaces for YAML/JSON, and tabs only for Makefiles if one appears later.

- [ ] **Step 3: Verify attributes and editor settings**

```bash
git check-attr text eol -- gradlew build.gradle.kts README.md
git check-attr binary -- gradle/wrapper/gradle-wrapper.jar
rg -n 'end_of_line = lf|charset = utf-8|insert_final_newline = true' .editorconfig
```

Expected: text files report `eol: lf`, the wrapper JAR reports `binary: set`, and all editor settings match.

- [ ] **Step 4: Commit**

```bash
git add .gitattributes .editorconfig
git commit -m "chore: enforce Linux text conventions"
```

### Task 2: Port Markdown Artifact Validators with Characterisation Tests

**Use Cases:**
- A valid spec or plan decision ledger passes with the required columns and concrete rows.
- Missing ledger columns, empty rows, invalid deferred values, and placeholder tokens fail with collected errors.
- Valid issue mirrors, plan outcome proofs, and plan task use-case blocks pass.
- Missing issue metadata, missing plan fields, and task sections without use cases fail loudly.
- Missing options, unknown options, and nonexistent files return non-zero without defaulting to another path.

**Files:**
- Create: `scripts/test-validators.sh`
- Create: `scripts/validate-decision-ledger.sh`
- Create: `scripts/validate-issue-mirror.sh`
- Create: `scripts/validate-plan-outcome-proof.sh`
- Create: `scripts/validate-plan-task-use-cases.sh`
- Reference: `scripts/validate-decision-ledger.ps1`
- Reference: `scripts/validate-issue-mirror.ps1`
- Reference: `scripts/validate-plan-outcome-proof.ps1`
- Reference: `scripts/validate-plan-task-use-cases.ps1`

- [ ] **Step 1: Write the failing Bash regression runner**

Create a runner with `set -Eeuo pipefail`, an owned `mktemp -d`, `trap 'rm -rf "$tmp_dir"' EXIT`, and these helpers:

```bash
assert_success() {
  local name=$1
  shift
  if ! output=$("$@" 2>&1); then
    printf 'FAIL: %s\n%s\n' "$name" "$output" >&2
    return 1
  fi
}

assert_failure() {
  local name=$1
  shift
  if output=$("$@" 2>&1); then
    printf 'FAIL: %s unexpectedly passed\n%s\n' "$name" "$output" >&2
    return 1
  fi
}
```

Add minimal valid and invalid Markdown fixtures for all four contracts and call the future scripts with `--path`, `--kind`, `--issue-file`, and `--plan-path`.

- [ ] **Step 2: Run tests and verify the expected failure**

```bash
bash scripts/test-validators.sh
```

Expected: failure naming the first missing `.sh` validator.

- [ ] **Step 3: Implement the four validators**

Each script must:

```bash
#!/usr/bin/env bash
set -Eeuo pipefail
```

Parse options with a `while (($#)); do case "$1" in ... esac; done` loop, require exact option values, collect semantic failures in an indexed Bash array, print each as `- <message>` to standard error, and return 1 when the array is non-empty. Preserve the field lists and section semantics from the PowerShell source. Resolve issue mirror paths with `realpath` and require the file to remain below `docs/superpowers/issues/`.

- [ ] **Step 4: Make scripts executable and run the regression suite**

```bash
chmod +x scripts/test-validators.sh scripts/validate-decision-ledger.sh scripts/validate-issue-mirror.sh scripts/validate-plan-outcome-proof.sh scripts/validate-plan-task-use-cases.sh
bash -n scripts/*.sh
./scripts/test-validators.sh
```

Expected: syntax checks and all Markdown validator success/failure assertions pass.

- [ ] **Step 5: Validate canonical artifacts**

```bash
./scripts/validate-decision-ledger.sh --path docs/superpowers/specs/2026-07-09-linux-development-toolchain-design.md --kind spec
./scripts/validate-plan-outcome-proof.sh --plan-path docs/superpowers/plans/2026-07-09-m4-linux-development-toolchain-migration-plan.md
./scripts/validate-plan-task-use-cases.sh --plan-path docs/superpowers/plans/2026-07-09-m4-linux-development-toolchain-migration-plan.md
./scripts/validate-decision-ledger.sh --path docs/superpowers/plans/2026-07-09-m4-linux-development-toolchain-migration-plan.md --kind plan
```

Expected: all four commands pass.

- [ ] **Step 6: Commit**

```bash
git add scripts/test-validators.sh scripts/validate-decision-ledger.sh scripts/validate-issue-mirror.sh scripts/validate-plan-outcome-proof.sh scripts/validate-plan-task-use-cases.sh
git commit -m "build: port artifact validators to Bash"
```

### Task 3: Port Workflow Mode Ledger Validation

**Use Cases:**
- A valid Auto Mode ledger passes when required fields, fixed values, source files, and stop conditions exist.
- Malformed JSON fails before field inspection.
- Missing `jq`, source artifacts, required fields, or stop conditions fail with exact messages.
- Source spec and plan paths cannot escape the supplied repository root.

**Files:**
- Create: `scripts/validate-workflow-mode-ledger.sh`
- Modify: `scripts/test-validators.sh`
- Reference: `scripts/validate-workflow-mode-ledger.ps1`

- [ ] **Step 1: Add failing workflow-ledger fixtures**

Add success fixtures for the approved run ledger and failure fixtures for malformed JSON, missing fields, the wrong question ID, wrong source/mode/policy values, absent source files, and missing required stop conditions.

- [ ] **Step 2: Run tests and verify the expected failure**

```bash
./scripts/test-validators.sh
```

Expected: failure because `scripts/validate-workflow-mode-ledger.sh` does not exist.

- [ ] **Step 3: Implement strict jq-backed validation**

Parse `--repo-root` and `--mode-ledger-path`, require `jq` with `command -v jq`, resolve both paths with `realpath`, reject ledgers outside the repository, run `jq -e .` before field checks, then use `jq -e` expressions for required fields, `project_workflow_mode`, `request_user_input`, Auto Mode, recorded defaults, stop-outside-policy, source paths, and stop conditions.

- [ ] **Step 4: Verify fixtures and the approved ledger**

```bash
chmod +x scripts/validate-workflow-mode-ledger.sh
bash -n scripts/validate-workflow-mode-ledger.sh
./scripts/test-validators.sh
./scripts/validate-workflow-mode-ledger.sh --repo-root . --mode-ledger-path .superpowers/runs/20260709-linux-development-toolchain/workflow-mode-ledger.json
```

Expected: all fixture assertions and the approved run ledger pass.

- [ ] **Step 5: Commit**

```bash
git add scripts/test-validators.sh scripts/validate-workflow-mode-ledger.sh
git commit -m "build: validate workflow ledgers with jq"
```

### Task 4: Remove Windows Entrypoints and Cut Over Documentation

**Use Cases:**
- Contributors find only `./gradlew` and executable Bash validator commands.
- Superpowers specs, plans, issue mirrors, and project context remain reusable on Linux.
- Cleanup proof uses the user-level Bash cleanup hook.
- No PowerShell wrapper or batch launcher remains as an alternate path.
- Runtime portability fixtures and generated Gradle launcher compatibility code remain intact.
- Cutover proof reports zero forbidden Windows development entry points after the migration.

**Files:**
- Delete: `gradlew.bat`
- Delete: `scripts/validate-decision-ledger.ps1`
- Delete: `scripts/validate-issue-mirror.ps1`
- Delete: `scripts/validate-plan-outcome-proof.ps1`
- Delete: `scripts/validate-plan-task-use-cases.ps1`
- Delete: `scripts/validate-workflow-mode-ledger.ps1`
- Modify: `README.md`
- Modify: `CONTRIBUTING.md`
- Modify: `docs/superpowers/PROJECT_CONTEXT.md`
- Modify: `docs/superpowers/specs/2026-06-26-issue-editor-preview-design.md`
- Modify: `docs/superpowers/plans/2026-06-26-m1-issue-editor-preview-plan.md`
- Modify: `docs/superpowers/issues/1-render-github-issues-in-editor-preview-and-group-by-milestone.md`
- Delete: the obsolete multi-repository artifacts named by `docs/superpowers/plans/2026-07-10-m1-single-repository-issue-navigator-plan.md`.

- [ ] **Step 1: Capture the failing Windows-tooling inventory**

```bash
git ls-files | rg '\.(ps1|bat|cmd)$'
rg -n -i 'pwsh(?:\.exe)?|powershell|gradlew\.bat|```powershell|\.\\gradlew|docs\\superpowers' README.md CONTRIBUTING.md docs .github
```

Expected: the current Windows scripts, launcher, and documentation commands appear.

- [ ] **Step 2: Delete replaced files and migrate commands**

Remove the six Windows entry points. Convert code fences to `bash`, path separators to `/`, validator options to GNU-style names, `gradlew.bat` to `./gradlew`, and cleanup commands to `bash "$HOME/.codex/hooks/codex-cleanup.sh" --repo-root .`. Update development requirements from JDK 17 to JDK 21 and list Bash, jq, ShellCheck, Git, and GNU core utilities.

- [ ] **Step 3: Run the narrow post-cutover scan**

```bash
test -z "$(git ls-files | rg '\.(ps1|bat|cmd)$' || true)"
! rg -n -i 'pwsh(?:\.exe)?|powershell|gradlew\.bat|```powershell|\.\\gradlew|docs\\superpowers' README.md CONTRIBUTING.md docs .github
```

Expected: both commands pass with zero forbidden matches.

- [ ] **Step 4: Confirm intentional runtime/generated references remain**

```bash
rg -n 'C:/workspaces|C:/missing' src/test
rg -n 'Cygwin|MSYS|MinGW' gradlew
rg -n 'Windows 11' .github/ISSUE_TEMPLATE/bug_report.yml
```

Expected: runtime portability fixtures, generated wrapper compatibility text, and cross-platform bug-report guidance remain.

- [ ] **Step 5: Revalidate migrated artifacts and commit**

```bash
./scripts/test-validators.sh
for issue in docs/superpowers/issues/[1345]-*.md; do ./scripts/validate-issue-mirror.sh --issue-file "$issue"; done
git add README.md CONTRIBUTING.md docs
git add -u gradlew.bat scripts
git commit -m "docs: cut development workflows over to Linux"
```

Expected: validators pass and Git records deletions rather than replacements.

### Task 5: Add Linux CI and Harden Bash Workflows

**Use Cases:**
- Pull requests and pushes run shell validation, unit tests, plugin verification, and packaging on Ubuntu with JDK 21.
- PR compliance handles branch, commit, and PR identifiers without unquoted expansion.
- Tag releases stop before publication when the changelog lacks the exact tagged version.
- Release publication starts only after validators, tests, plugin verification, and packaging pass.
- Workflows use narrow permissions, bounded timeouts, and strict Bash mode.

**Files:**
- Create: `.github/workflows/ci.yml`
- Modify: `.github/workflows/pr_compliance.yml`
- Modify: `.github/workflows/publish-release.yml`

- [ ] **Step 1: Record missing CI gates**

```bash
test -f .github/workflows/ci.yml
rg -n 'timeout-minutes|set -Eeuo pipefail|validate-wrapper|test|verifyPlugin|buildPlugin' .github/workflows/*.yml
```

Expected: the CI file check fails and existing workflows lack part of the required gate set.

- [ ] **Step 2: Create the Linux CI workflow**

Use `ubuntu-latest`, read-only contents permission, concurrency cancellation, a bounded job timeout, checkout, Gradle wrapper validation, Zulu JDK 21, Gradle setup, explicit `bash` shell, dependency checks for Bash/jq/ShellCheck, `shellcheck scripts/*.sh`, `./scripts/test-validators.sh`, and separate `./gradlew --no-daemon test verifyPlugin buildPlugin` proof.

- [ ] **Step 3: Harden PR compliance**

Add a job timeout and default Bash shell, start each run block with `set -Eeuo pipefail`, quote GitHub-derived values, read commit SHAs through a `while IFS= read -r` loop, and pass PR numbers as quoted arguments to `gh pr comment`.

- [ ] **Step 4: Harden release ordering**

Add a job timeout, concurrency guard, wrapper validation, explicit Bash strict mode, and a preflight step that derives the tag, requires an exact `## [version]` changelog header, and exports the tag/anchor through quoted `$GITHUB_ENV` writes. Run shell validation, validator tests, Gradle tests, plugin verification, and packaging before `publishPlugin` and GitHub release creation.

- [ ] **Step 5: Review workflow structure**

```bash
rg -n 'runs-on: ubuntu|timeout-minutes:|shell: bash|set -Eeuo pipefail|validate-wrapper|shellcheck|test|verifyPlugin|buildPlugin|publishPlugin' .github/workflows/*.yml
rg -n -B 8 -A 8 'publishPlugin|action-gh-release' .github/workflows/publish-release.yml
```

Expected: Linux, strict-shell, timeout, wrapper, validation, and proof markers exist; publication steps follow the proof steps.

- [ ] **Step 6: Commit**

```bash
git add .github/workflows/ci.yml .github/workflows/pr_compliance.yml .github/workflows/publish-release.yml
git commit -m "ci: enforce Linux validation and release gates"
```

### Task 6: Run Full Cutover Verification and Cleanup

**Use Cases:**
- The repository proves Bash validator parity, plugin correctness, package creation, and process cleanup before completion.
- A final scan reports only intentional cross-platform runtime and generated-wrapper Windows references.
- The completion claim uses fresh command output from the final tree.

**Files:**
- Verify: all changed and created files from Tasks 1 through 5

- [ ] **Step 1: Invoke required verification discipline**

Read and apply `superpowers:verification-before-completion` before running final proof or claiming success.

- [ ] **Step 2: Run shell static and regression proof**

```bash
bash -n scripts/*.sh
shellcheck scripts/*.sh
./scripts/test-validators.sh
```

Expected: status 0 and zero ShellCheck findings.

- [ ] **Step 3: Run canonical artifact validators**

Run every validator command listed under `## Proof Oracle` before the Gradle commands.

Expected: all canonical artifacts pass.

- [ ] **Step 4: Run Gradle proof**

```bash
./gradlew --no-daemon test
./gradlew --no-daemon verifyPlugin
./gradlew --no-daemon buildPlugin
```

Expected: all tasks succeed and `build/distributions/` contains the plugin ZIP.

- [ ] **Step 5: Verify modes, scans, and worktree scope**

```bash
git ls-files --stage gradlew scripts/*.sh
test -z "$(git ls-files | rg '\.(ps1|bat|cmd)$' || true)"
git diff --check
git status --short
```

Expected: launchers/scripts use `100755`, no Windows development files remain, the diff has no whitespace errors, and status contains only intended migration changes.

- [ ] **Step 6: Run repository-scoped cleanup**

```bash
bash "$HOME/.codex/hooks/codex-cleanup.sh" --repo-root .
```

Expected: no task-owned background processes or cleanup failures.

- [ ] **Step 7: Commit final proof adjustments if needed**

```bash
git add -A
git commit -m "chore: complete Linux toolchain migration"
```

Run this commit only when verification required a tracked adjustment. Do not create an empty commit.

## Decision Ledger

| Decision | Source | Answer | Impact | Deferred? | Risk owner |
|---|---|---|---|---|---|
| Platform scope | Source spec and user answer | Convert development/build tooling only; keep plugin runtime cross-platform. | Excludes product code and portability fixtures from edits. | No | User |
| Migration design | Source spec and user answer | Use complete Linux replacement with tests and CI. | Includes script translation, deletions, docs, policy, and proof gates. | No | User |
| Workflow mode | User direct instruction and validated ledger | Auto Mode for one migration route. | Allows recorded recommended defaults until route closeout. | No | User |
| Execution route | Auto Mode recorded default | Use `$superpowers-project:implement-plan` in the current thread. | Avoids issue creation and agent delegation. | No | Codex |
| Branch strategy | Auto Mode recorded default | Use `codex/linux-development-toolchain-migration`. | Keeps implementation commits off `main` after the design-spec commit. | No | Codex |
| Publish behavior | Request scope and Auto Mode policy | Keep work local; do not push, publish, release, or create a PR. | Prevents external mutation outside the migration request. | No | Codex |
| TDD policy | Planning discipline | Use characterisation fixtures before each validator implementation and failing scans before cutover edits. | Proves translation semantics and deletion gates. | No | Codex |
| Debug policy | Repo evidence | Treat validator disagreements, CI failures, and Gradle failures with systematic diagnosis before changing behavior. | Prevents weakening gates to make tests pass. | No | Codex |
| Test complete | Approved design and planning metrics | Zero shell findings, all positive/negative fixtures pass, all canonical validators pass, three Gradle proof commands pass, zero forbidden Windows tooling matches. | Defines an exact completion threshold with no tolerance band. | No | Codex |
| JSON parser | Source spec | Require jq and fail when it is missing. | Gives reliable JSON semantics without a fallback parser. | No | Codex |
| Script interface | Source spec | Use GNU-style long options only. | Removes PowerShell parameter compatibility paths. | No | Codex |
| Replaced files | Source spec | Delete five `.ps1` files and `gradlew.bat`. | Completes cutover without wrappers or redirects. | No | Codex |
| Line endings | Repo evidence and source spec | Enforce LF with `.gitattributes` and `.editorconfig`. | Makes transferred normalization durable. | No | Codex |
| Generated wrapper | Source spec | Keep `gradlew` unchanged despite generated Cygwin/MSYS text. | Preserves Gradle wrapper integrity. | No | Codex |
| Runtime fixtures | User scope answer | Keep Windows-style path fixtures and cross-platform issue template text. | Preserves product portability coverage. | No | User |
| Tooling ownership | Planning default | Anchor repository maintainer owns tooling; validator filenames own individual contracts. | Gives each failure path a clear maintenance boundary. | No | Codex |
| Cutover sequencing | Planning default | Add and prove Bash validators, then delete Windows paths, then wire CI. | Keeps a working validation path throughout implementation. | No | Codex |
| Stop criteria | Auto Mode authorization and plan outcome | Stop on missing proof, failed validation, unsafe dirty-state overlap, or decisions outside the approved scope. | Prevents autonomous scope expansion. | No | Codex |
| Completion discipline | Project workflow contract | Use `superpowers:verification-before-completion` and the repo cleanup hook. | Requires fresh proof before any success claim. | No | Codex |
