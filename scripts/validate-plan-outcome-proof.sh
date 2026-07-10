#!/usr/bin/env bash
set -Eeuo pipefail

usage() {
    printf 'Usage: %s --plan-path <file>\n' "${0##*/}" >&2
}

plan_path=
while (($#)); do
    case "$1" in
        --plan-path)
            if [[ -z ${2-} || ${2-} == --* ]]; then
                printf 'Missing value for --plan-path\n' >&2
                usage
                exit 2
            fi
            plan_path=$2
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

if [[ -z $plan_path ]]; then
    printf 'Missing required option: --plan-path\n' >&2
    usage
    exit 2
fi
if [[ ! -f $plan_path ]]; then
    printf 'Plan file does not exist: %s\n' "$plan_path" >&2
    exit 1
fi

errors=()
content=$(<"$plan_path")
if ! grep -Eq '^## Outcome Proof[[:space:]]*$' <<<"$content"; then
    errors+=("Missing section: ## Outcome Proof")
fi
if ! grep -Eq '^## Implementation Boundaries[[:space:]]*$' <<<"$content"; then
    errors+=("Missing section: ## Implementation Boundaries")
fi

outcome_fields=(
    "Intent"
    "Current Behavior"
    "Expected Outcome"
    "Target Output"
    "Owner"
    "Interface"
    "Cutover"
    "Replaced Path"
    "Evidence"
    "Acceptance Proof"
    "Stop Criteria"
    "Avoid"
    "Risk"
)
boundary_fields=(
    "Files To Create"
    "Files To Modify"
    "Files To Avoid"
    "Source Of Truth"
    "Read Path"
    "Write Path"
    "Integration Points"
    "Migration Or Cutover"
    "Replaced Path Handling"
    "Acceptance Proof Gate"
)

for field in "${outcome_fields[@]}"; do
    if ! grep -Eq "^[[:space:]]*(-[[:space:]]+)?\\*\\*${field}:\\*\\*[[:space:]]+[^[:space:]].*$" <<<"$content"; then
        errors+=("Missing Outcome Proof field: $field")
    fi
done
for field in "${boundary_fields[@]}"; do
    if ! grep -Eq "^[[:space:]]*(-[[:space:]]+)?\\*\\*${field}:\\*\\*[[:space:]]+[^[:space:]].*$" <<<"$content"; then
        errors+=("Missing Implementation Boundaries field: $field")
    fi
done

if ((${#errors[@]})); then
    printf -- '- %s\n' "${errors[@]}" >&2
    exit 1
fi

printf 'Outcome proof valid: %s\n' "$plan_path"
