#!/usr/bin/env bash
set -Eeuo pipefail

usage() {
    printf 'Usage: %s --repo-root <directory> --mode-ledger-path <file>\n' "${0##*/}" >&2
}

require_value() {
    local option=$1
    local value=${2-}
    if [[ -z $value || $value == --* ]]; then
        printf 'Missing value for %s\n' "$option" >&2
        usage
        exit 2
    fi
}

repo_root=
mode_ledger_path=
while (($#)); do
    case "$1" in
        --repo-root)
            require_value "$1" "${2-}"
            repo_root=$2
            shift 2
            ;;
        --mode-ledger-path)
            require_value "$1" "${2-}"
            mode_ledger_path=$2
            shift 2
            ;;
        --help)
            usage
            exit 0
            ;;
        *)
            printf 'Unknown option: %s\n' "$1" >&2
            usage
            exit 2
            ;;
    esac
done

if [[ -z $repo_root ]]; then
    printf 'Missing required option: --repo-root\n' >&2
    usage
    exit 2
fi
if [[ -z $mode_ledger_path ]]; then
    printf 'Missing required option: --mode-ledger-path\n' >&2
    usage
    exit 2
fi
if [[ ! -d $repo_root ]]; then
    printf 'Repository root does not exist: %s\n' "$repo_root" >&2
    exit 1
fi
if [[ ! -f $mode_ledger_path ]]; then
    printf 'Mode ledger does not exist: %s\n' "$mode_ledger_path" >&2
    exit 1
fi
if ! command -v jq >/dev/null 2>&1; then
    printf 'Required command is missing: jq\n' >&2
    exit 1
fi

repo_root=$(realpath -e "$repo_root")
mode_ledger_path=$(realpath -e "$mode_ledger_path")
case "$mode_ledger_path" in
    "$repo_root"/*) ;;
    *)
        printf 'Mode ledger must live under the repository root\n' >&2
        exit 1
        ;;
esac

if ! jq -e . "$mode_ledger_path" >/dev/null 2>&1; then
    printf 'Mode ledger is not valid JSON: %s\n' "$mode_ledger_path" >&2
    exit 1
fi

errors=()
required_fields=(
    "question_id"
    "source"
    "selected_mode"
    "repo_root"
    "plugin_manifest_version"
    "plugin_contract_hash"
    "started_at"
    "autonomy_scope"
    "mutation_scope"
    "candidate_scope"
    "route_policy"
    "proof_policy"
    "stop_conditions"
    "downstream_ledger_paths"
)

for field in "${required_fields[@]}"; do
    if ! jq -e --arg field "$field" '
        has($field)
        and .[$field] != null
        and .[$field] != ""
        and .[$field] != []
        and .[$field] != {}
    ' "$mode_ledger_path" >/dev/null; then
        errors+=("Missing required field: $field")
    fi
done

question_id=$(jq -r '.question_id // empty' "$mode_ledger_path")
source=$(jq -r '.source // empty' "$mode_ledger_path")
selected_mode=$(jq -r '.selected_mode // empty | ascii_downcase' "$mode_ledger_path")
autonomy_scope=$(jq -r '.autonomy_scope // empty' "$mode_ledger_path")

if [[ $question_id != project_workflow_mode ]]; then
    errors+=("question_id must be project_workflow_mode")
fi
if [[ $source != request_user_input ]]; then
    errors+=("source must be request_user_input")
fi
case "$selected_mode" in
    manual)
        if [[ $autonomy_scope != ask-every-material-decision ]]; then
            errors+=("manual mode must use ask-every-material-decision autonomy_scope")
        fi
        ;;
    auto)
        if [[ $autonomy_scope != one-route ]]; then
            errors+=("auto mode must use one-route autonomy_scope")
        fi
        if ! jq -e '.route_policy.one_route_only == true' "$mode_ledger_path" >/dev/null; then
            errors+=("auto mode requires route_policy.one_route_only true")
        fi
        if jq -e '.route_policy.continue_to_next_candidate == true' "$mode_ledger_path" >/dev/null; then
            errors+=("auto mode cannot continue to the next candidate")
        fi
        ;;
    looping)
        if [[ $autonomy_scope != bounded-loop ]]; then
            errors+=("looping mode must use bounded-loop autonomy_scope")
        fi
        ;;
    *)
        errors+=("selected_mode must be manual, auto, or looping")
        ;;
esac

required_stop_conditions=(
    "missing-proof"
    "dirty-unsafe-state"
    "failed-validation"
    "decision-outside-policy"
)
for condition in "${required_stop_conditions[@]}"; do
    if ! jq -e --arg condition "$condition" '.stop_conditions | index($condition) != null' "$mode_ledger_path" >/dev/null; then
        errors+=("stop_conditions missing $condition")
    fi
done

while IFS= read -r downstream_path; do
    if [[ -z $downstream_path ]]; then
        continue
    fi
    if [[ ! -e $repo_root/$downstream_path ]]; then
        errors+=("Downstream ledger path does not exist: $downstream_path")
    fi
done < <(jq -r '.downstream_ledger_paths[]? // empty' "$mode_ledger_path")

if ((${#errors[@]})); then
    printf -- '- %s\n' "${errors[@]}" >&2
    exit 1
fi

printf 'Workflow mode ledger valid: %s\n' "$mode_ledger_path"
