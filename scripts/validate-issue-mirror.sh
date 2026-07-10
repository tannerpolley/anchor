#!/usr/bin/env bash
set -Eeuo pipefail

usage() {
    printf 'Usage: %s --issue-file <file>\n' "${0##*/}" >&2
}

issue_file=
while (($#)); do
    case "$1" in
        --issue-file)
            if [[ -z ${2-} || ${2-} == --* ]]; then
                printf 'Missing value for --issue-file\n' >&2
                usage
                exit 2
            fi
            issue_file=$2
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

if [[ -z $issue_file ]]; then
    printf 'Missing required option: --issue-file\n' >&2
    usage
    exit 2
fi
if [[ ! -f $issue_file ]]; then
    printf 'Issue mirror does not exist: %s\n' "$issue_file" >&2
    exit 1
fi

repo_root=$(git rev-parse --show-toplevel)
resolved_issue=$(realpath -e "$issue_file")
issue_root=$(realpath -e "$repo_root/docs/superpowers/issues")
case "$resolved_issue" in
    "$issue_root"/*) ;;
    *)
        printf 'Issue mirror must live under docs/superpowers/issues\n' >&2
        exit 1
        ;;
esac

errors=()
content=$(<"$resolved_issue")
required_metadata=(
    "GitHub Issue"
    "GitHub Milestone"
    "Issue Type"
    "Source Plan"
    "Classification"
    "Labels"
    "Goal Command"
    "Execution Mode"
    "Worktree Policy"
    "Integration Policy"
    "TDD Policy"
    "Parallelization Plan"
    "Reviewer Role"
    "Script Gate Mode"
)
required_sections=(
    "Outcome Summary"
    "Project Merge"
    "What To Build"
    "Acceptance Criteria"
    "Blocked by"
    "Non-goals"
    "Proof Oracle"
)
outcome_fields=(
    "Outcome Source"
    "Intent"
    "Target Output"
    "Owner"
    "Interface"
    "Cutover"
    "Replaced Path"
    "Acceptance Proof"
    "Stop Criteria"
    "Avoid"
)
merge_fields=(
    "Merge Owner"
    "Merge Gate"
    "Merge Policy"
    "Worktree Cleanup Policy"
    "Orchestrator Wakeup Policy"
)

for field in "${required_metadata[@]}"; do
    if ! grep -Eq "^\\*\\*${field}:\\*\\*[[:space:]]+[^[:space:]].*$" <<<"$content"; then
        errors+=("Missing metadata field: $field")
    fi
done
for section in "${required_sections[@]}"; do
    if ! grep -Fqx "## $section" <<<"$content"; then
        errors+=("Missing section: $section")
    fi
done
for field in "${outcome_fields[@]}"; do
    if ! grep -Eq "^\\*\\*${field}:\\*\\*[[:space:]]+[^[:space:]].*$" <<<"$content"; then
        errors+=("Missing Outcome Summary field: $field")
    fi
done
for field in "${merge_fields[@]}"; do
    if ! grep -Eq "^\\*\\*${field}:\\*\\*[[:space:]]+[^[:space:]].*$" <<<"$content"; then
        errors+=("Missing Project Merge field: $field")
    fi
done
if ! grep -Eq '^- \[ \] .+' <<<"$content"; then
    errors+=("Acceptance Criteria must include checkbox bullets")
fi

source_plan=$(sed -n 's/^\*\*Source Plan:\*\*[[:space:]]*//p' "$resolved_issue" | sed -n '1p')
if [[ -n $source_plan && ! -f $repo_root/$source_plan ]]; then
    errors+=("Source Plan does not exist: $source_plan")
fi

if ((${#errors[@]})); then
    printf -- '- %s\n' "${errors[@]}" >&2
    exit 1
fi

printf 'Issue mirror valid: %s\n' "$issue_file"
