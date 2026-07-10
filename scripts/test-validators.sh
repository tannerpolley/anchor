#!/usr/bin/env bash
set -Eeuo pipefail

repo_root=$(git rev-parse --show-toplevel)
tmp_dir=$(mktemp -d "$repo_root/build/validator-tests.XXXXXX")
issue_tmp=$(mktemp -d "$repo_root/docs/superpowers/issues/.validator-tests.XXXXXX")

cleanup() {
    rm -rf "$tmp_dir" "$issue_tmp"
}
trap cleanup EXIT

assert_success() {
    local name=$1
    shift
    local output
    if ! output=$("$@" 2>&1); then
        printf 'FAIL: %s\n%s\n' "$name" "$output" >&2
        return 1
    fi
    printf 'PASS: %s\n' "$name"
}

assert_failure() {
    local name=$1
    local expected=$2
    shift 2
    local output
    if output=$("$@" 2>&1); then
        printf 'FAIL: %s unexpectedly passed\n%s\n' "$name" "$output" >&2
        return 1
    fi
    if [[ $output != *"$expected"* ]]; then
        printf 'FAIL: %s did not report %q\n%s\n' "$name" "$expected" "$output" >&2
        return 1
    fi
    printf 'PASS: %s\n' "$name"
}

cat >"$tmp_dir/valid-spec.md" <<'EOF'
# Fixture Spec

## Decision Ledger

| Decision | Source | Answer | Impact | Deferred? | Risk owner |
|---|---|---|---|---|---|
| Route | User | Continue | Bounded execution | No | Maintainer |
EOF

cat >"$tmp_dir/invalid-spec.md" <<'EOF'
# Fixture Spec

## Decision Ledger

| Decision | Source | Answer | Impact | Deferred? | Risk owner |
|---|---|---|---|---|---|
| Route | User | TODO | Bounded execution | No | Maintainer |
EOF

cat >"$tmp_dir/valid-plan.md" <<'EOF'
# Fixture Implementation Plan

## Outcome Proof

**Intent:** Exercise the validator.
**Current Behavior:** No fixture validation exists.
**Expected Outcome:** The fixture passes.
**Target Output:** One validated fixture.
**Owner:** Maintainer.
**Interface:** Bash command.
**Cutover:** Replace the old fixture.
**Replaced Path:** Delete the old fixture.
**Evidence:** Validator receipt for the target artifact.
**Acceptance Proof:** The target artifact passes all structural checks.
**Stop Criteria:** Any validator failure.
**Avoid:** Weakening the fixture.
**Risk:** Incorrect field parsing.

## Implementation Boundaries

**Files To Create:** Fixture file.
**Files To Modify:** Validator test.
**Files To Avoid:** Production code.
**Source Of Truth:** Fixture contract.
**Read Path:** Fixture Markdown.
**Write Path:** Standard output.
**Integration Points:** Test runner.
**Migration Or Cutover:** Replace the old fixture.
**Replaced Path Handling:** Delete the old fixture.
**Acceptance Proof Gate:** Validator exits zero.

### Task 1: Validate Fixture

**Use Cases:**
- Acceptance proof covers the migration cutover.

**Files:**
- Test: `fixture.md`

- [ ] **Step 1: Run validation**
EOF

cp "$tmp_dir/valid-plan.md" "$tmp_dir/invalid-plan.md"
sed -i '/^\*\*Risk:\*\*/d' "$tmp_dir/invalid-plan.md"

cp "$tmp_dir/valid-plan.md" "$tmp_dir/invalid-task-plan.md"
sed -i '/^\*\*Use Cases:\*\*$/,/^\*\*Files:\*\*$/c\
**Files:**' "$tmp_dir/invalid-task-plan.md"

cat >"$issue_tmp/valid-issue.md" <<'EOF'
# Fixture Issue

**GitHub Issue:** https://github.com/example/anchor/issues/99
**GitHub Milestone:** M1
**Issue Type:** Task
**Source Plan:** docs/superpowers/plans/2026-07-09-m4-linux-development-toolchain-migration-plan.md
**Classification:** Ready
**Labels:** type:task
**Goal Command:** Implement the fixture
**Execution Mode:** Inline
**Worktree Policy:** Current branch
**Integration Policy:** Local commit
**TDD Policy:** Required
**Parallelization Plan:** None
**Reviewer Role:** Maintainer
**Script Gate Mode:** Strict

## Outcome Summary

**Outcome Source:** Source plan
**Intent:** Validate an issue mirror.
**Target Output:** Valid fixture.
**Owner:** Maintainer.
**Interface:** Bash command.
**Cutover:** Replace invalid fixture.
**Replaced Path:** Invalid fixture.
**Acceptance Proof:** Validator exits zero.
**Stop Criteria:** Validation failure.
**Avoid:** Missing metadata.

## Project Merge

**Merge Owner:** Maintainer
**Merge Gate:** Validation passes
**Merge Policy:** Local merge
**Worktree Cleanup Policy:** Remove task fixtures
**Orchestrator Wakeup Policy:** After validation

## What To Build

Validate this fixture.

## Acceptance Criteria

- [ ] Required metadata and sections pass.

## Blocked by

- Nothing.

## Non-goals

- Production behavior.

## Proof Oracle

- Run the issue validator.
EOF

cp "$issue_tmp/valid-issue.md" "$issue_tmp/invalid-issue.md"
sed -i '/^\*\*Goal Command:\*\*/d' "$issue_tmp/invalid-issue.md"

cat >"$tmp_dir/malformed-ledger.json" <<'EOF'
{"question_id":
EOF

cat >"$tmp_dir/invalid-ledger.json" <<'EOF'
{
  "question_id": "wrong_question",
  "source": "request_user_input",
  "selected_mode": "auto",
  "repo_root": ".",
  "plugin_manifest_version": "1",
  "plugin_contract_hash": "fixture",
  "started_at": "2026-07-10T00:00:00Z",
  "autonomy_scope": "one-route",
  "mutation_scope": ["current-repo"],
  "candidate_scope": ["fixture"],
  "route_policy": {
    "one_route_only": true,
    "continue_to_next_candidate": false
  },
  "proof_policy": {"required": true},
  "stop_conditions": [
    "missing-proof",
    "dirty-unsafe-state",
    "failed-validation",
    "decision-outside-policy"
  ],
  "downstream_ledger_paths": ["fixture.json"]
}
EOF

assert_success \
    "valid decision ledger" \
    "$repo_root/scripts/validate-decision-ledger.sh" \
    --path "$tmp_dir/valid-spec.md" \
    --kind spec
assert_failure \
    "decision ledger placeholder" \
    "Placeholder token found" \
    "$repo_root/scripts/validate-decision-ledger.sh" \
    --path "$tmp_dir/invalid-spec.md" \
    --kind spec
assert_success \
    "valid plan outcome proof" \
    "$repo_root/scripts/validate-plan-outcome-proof.sh" \
    --plan-path "$tmp_dir/valid-plan.md"
assert_failure \
    "missing plan outcome field" \
    "Missing Outcome Proof field: Risk" \
    "$repo_root/scripts/validate-plan-outcome-proof.sh" \
    --plan-path "$tmp_dir/invalid-plan.md"
assert_success \
    "valid plan task use cases" \
    "$repo_root/scripts/validate-plan-task-use-cases.sh" \
    --plan-path "$tmp_dir/valid-plan.md"
assert_failure \
    "missing plan task use cases" \
    "missing **Use Cases:** block" \
    "$repo_root/scripts/validate-plan-task-use-cases.sh" \
    --plan-path "$tmp_dir/invalid-task-plan.md"
assert_success \
    "valid issue mirror" \
    "$repo_root/scripts/validate-issue-mirror.sh" \
    --issue-file "$issue_tmp/valid-issue.md"
assert_failure \
    "missing issue metadata" \
    "Missing metadata field: Goal Command" \
    "$repo_root/scripts/validate-issue-mirror.sh" \
    --issue-file "$issue_tmp/invalid-issue.md"
assert_success \
    "valid workflow mode ledger" \
    "$repo_root/scripts/validate-workflow-mode-ledger.sh" \
    --repo-root "$repo_root" \
    --mode-ledger-path "$repo_root/.superpowers/runs/20260709-linux-development-toolchain/workflow-mode-ledger.json"
assert_failure \
    "malformed workflow mode ledger" \
    "Mode ledger is not valid JSON" \
    "$repo_root/scripts/validate-workflow-mode-ledger.sh" \
    --repo-root "$repo_root" \
    --mode-ledger-path "$tmp_dir/malformed-ledger.json"
assert_failure \
    "invalid workflow mode question" \
    "question_id must be project_workflow_mode" \
    "$repo_root/scripts/validate-workflow-mode-ledger.sh" \
    --repo-root "$repo_root" \
    --mode-ledger-path "$tmp_dir/invalid-ledger.json"

printf 'All validator tests passed.\n'
