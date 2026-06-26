param(
    [Parameter(Mandatory = $true)]
    [string]$PlanPath
)

$ErrorActionPreference = "Stop"
$errors = New-Object System.Collections.Generic.List[string]

if (-not (Test-Path -LiteralPath $PlanPath)) {
    $errors.Add("Plan file does not exist: $PlanPath")
} else {
    $content = Get-Content -Raw -LiteralPath $PlanPath
    $requiredOutcomeFields = @(
        "Intent",
        "Current Behavior",
        "Expected Outcome",
        "Target Output",
        "Owner",
        "Interface",
        "Cutover",
        "Replaced Path",
        "Evidence",
        "Acceptance Proof",
        "Stop Criteria",
        "Avoid",
        "Risk"
    )
    $requiredBoundaryFields = @(
        "Files To Create",
        "Files To Modify",
        "Files To Avoid",
        "Source Of Truth",
        "Read Path",
        "Write Path",
        "Integration Points",
        "Migration Or Cutover",
        "Replaced Path Handling",
        "Acceptance Proof Gate"
    )

    if ($content -notmatch "(?m)^## Outcome Proof\s*$") {
        $errors.Add("Missing section: ## Outcome Proof")
    }
    if ($content -notmatch "(?m)^## Implementation Boundaries\s*$") {
        $errors.Add("Missing section: ## Implementation Boundaries")
    }

    foreach ($field in $requiredOutcomeFields) {
        if ($content -notmatch "(?m)^- \*\*$([regex]::Escape($field)):\*\* .+") {
            $errors.Add("Missing Outcome Proof field: $field")
        }
    }

    foreach ($field in $requiredBoundaryFields) {
        if ($content -notmatch "(?m)^- \*\*$([regex]::Escape($field)):\*\* .+") {
            $errors.Add("Missing Implementation Boundaries field: $field")
        }
    }
}

if ($errors.Count -gt 0) {
    Write-Error (($errors | ForEach-Object { "- $_" }) -join [Environment]::NewLine)
    exit 1
}

Write-Host "Outcome proof valid: $PlanPath"
