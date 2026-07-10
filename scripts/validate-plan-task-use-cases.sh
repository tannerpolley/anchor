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
task_count=0
task_title=
use_cases_seen=false
use_case_count=0
files_seen=false

finalize_task() {
    if [[ -z $task_title ]]; then
        return
    fi
    if [[ $use_cases_seen != true ]]; then
        errors+=("$task_title missing **Use Cases:** block")
    elif ((use_case_count == 0)); then
        errors+=("$task_title has no concrete use-case bullets")
    fi
}

while IFS= read -r line || [[ -n $line ]]; do
    if [[ $line =~ ^###[[:space:]]+Task[[:space:]]+[0-9]+:[[:space:]]+.+$ ]]; then
        finalize_task
        task_title=$line
        task_count=$((task_count + 1))
        use_cases_seen=false
        use_case_count=0
        files_seen=false
        continue
    fi
    if [[ -z $task_title ]]; then
        continue
    fi
    if [[ $line == "**Use Cases:**" ]]; then
        if [[ $files_seen == true ]]; then
            errors+=("$task_title has **Use Cases:** after **Files:**")
        fi
        use_cases_seen=true
        continue
    fi
    if [[ $line == "**Files:**" ]]; then
        files_seen=true
        continue
    fi
    if [[ $use_cases_seen == true && $files_seen != true && $line =~ ^-[[:space:]]+[^[:space:]].+$ ]]; then
        use_case_count=$((use_case_count + 1))
    fi
done <"$plan_path"
finalize_task

if ((task_count == 0)); then
    errors+=("No numbered task sections found")
fi
if ((${#errors[@]})); then
    printf -- '- %s\n' "${errors[@]}" >&2
    exit 1
fi

printf 'Task use cases valid: %s\n' "$plan_path"
