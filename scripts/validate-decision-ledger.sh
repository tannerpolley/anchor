#!/usr/bin/env bash
set -Eeuo pipefail

usage() {
    printf 'Usage: %s --path <file> [--kind <spec|plan|issue>]\n' "${0##*/}" >&2
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

trim() {
    local value=$1
    value=${value#"${value%%[![:space:]]*}"}
    value=${value%"${value##*[![:space:]]}"}
    printf '%s' "$value"
}

path=
kind=spec
while (($#)); do
    case "$1" in
        --path)
            require_value "$1" "${2-}"
            path=$2
            shift 2
            ;;
        --kind)
            require_value "$1" "${2-}"
            kind=$2
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

if [[ -z $path ]]; then
    printf 'Missing required option: --path\n' >&2
    usage
    exit 2
fi
if [[ $kind != spec && $kind != plan && $kind != issue ]]; then
    printf 'Invalid --kind value: %s\n' "$kind" >&2
    exit 2
fi
if [[ ! -f $path ]]; then
    printf 'File does not exist: %s\n' "$path" >&2
    exit 1
fi

errors=()
content=$(<"$path")
if ! grep -Eq '^## Decision Ledger[[:space:]]*$' <<<"$content"; then
    errors+=("Missing required section: ## Decision Ledger")
else
    section=$(awk '
        /^## Decision Ledger[[:space:]]*$/ { active = 1; next }
        active && /^##[[:space:]]+/ { exit }
        active { print }
    ' "$path")
    mapfile -t table_lines < <(grep -E '^[[:space:]]*\|' <<<"$section" || true)

    if ((${#table_lines[@]} < 3)); then
        errors+=("Decision ledger table or data rows are missing")
    else
        header_row=${table_lines[0]}
        header_row=${header_row#*|}
        header_row=${header_row%|*}
        IFS='|' read -r -a raw_headers <<<"$header_row"

        declare -A header_indexes=()
        for index in "${!raw_headers[@]}"; do
            header=$(trim "${raw_headers[$index]}")
            header_indexes["${header,,}"]=$index
        done

        required_headers=("decision" "source" "answer" "impact" "deferred?" "risk owner")
        for required_header in "${required_headers[@]}"; do
            if [[ ! -v "header_indexes[$required_header]" ]]; then
                errors+=("Decision ledger missing column: $required_header")
            fi
        done

        row_count=0
        for ((line_index = 2; line_index < ${#table_lines[@]}; line_index += 1)); do
            row=${table_lines[$line_index]}
            row=${row#*|}
            row=${row%|*}
            IFS='|' read -r -a cells <<<"$row"
            row_count=$((row_count + 1))

            if ((${#cells[@]} != ${#raw_headers[@]})); then
                errors+=("Decision row has wrong cell count: ${table_lines[$line_index]}")
                continue
            fi

            for required_header in "${required_headers[@]}"; do
                if [[ -v "header_indexes[$required_header]" ]]; then
                    value=$(trim "${cells[${header_indexes[$required_header]}]}")
                    if [[ -z $value ]]; then
                        errors+=("Decision row has empty $required_header: ${table_lines[$line_index]}")
                    fi
                fi
            done

            if [[ -v 'header_indexes[deferred?]' ]]; then
                deferred=$(trim "${cells[${header_indexes[deferred?]}]}")
                if [[ ${deferred,,} != yes && ${deferred,,} != no ]]; then
                    errors+=("Deferred? must be Yes or No: ${table_lines[$line_index]}")
                fi
            fi
        done

        if ((row_count == 0)); then
            errors+=("Decision ledger has no decision rows")
        fi
    fi
fi

if grep -Eiq '(^|[^[:alnum:]_])(TBD|TODO|FIXME)([^[:alnum:]_]|$)' <<<"$content"; then
    errors+=("Placeholder token found")
fi

if ((${#errors[@]})); then
    printf -- '- %s\n' "${errors[@]}" >&2
    exit 1
fi

printf 'Decision ledger valid for %s: %s\n' "$kind" "$path"
