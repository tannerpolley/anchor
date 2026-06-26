param(
    [Parameter(Mandatory = $true)]
    [string]$RepoRoot,

    [Parameter(Mandatory = $true)]
    [string]$ModeLedgerPath
)

$ErrorActionPreference = "Stop"
$errors = New-Object System.Collections.Generic.List[string]

if (-not (Test-Path -LiteralPath $RepoRoot -PathType Container)) {
    $errors.Add("RepoRoot does not exist: $RepoRoot")
}

if (-not (Test-Path -LiteralPath $ModeLedgerPath -PathType Leaf)) {
    $errors.Add("Mode ledger does not exist: $ModeLedgerPath")
} else {
    $ledger = Get-Content -Raw -LiteralPath $ModeLedgerPath | ConvertFrom-Json
    $requiredFields = @(
        "question_id",
        "source",
        "selected_mode",
        "run_id",
        "source_spec",
        "source_plan",
        "decision_policy",
        "stop_conditions"
    )

    foreach ($field in $requiredFields) {
        if (@($ledger.PSObject.Properties.Name) -notcontains $field) {
            $errors.Add("Missing field: $field")
        }
    }

    if ($ledger.question_id -ne "project_workflow_mode") {
        $errors.Add("question_id must be project_workflow_mode")
    }
    if ($ledger.source -ne "request_user_input") {
        $errors.Add("source must be request_user_input")
    }
    if ($ledger.selected_mode -ne "auto-mode") {
        $errors.Add("selected_mode must be auto-mode")
    }
    if ($ledger.decision_policy.selected_mode -ne "recorded-defaults") {
        $errors.Add("decision_policy.selected_mode must be recorded-defaults")
    }
    if ($ledger.decision_policy.stop_outside_policy -ne $true) {
        $errors.Add("decision_policy.stop_outside_policy must be true")
    }

    $root = (Resolve-Path -LiteralPath $RepoRoot).Path
    foreach ($pathField in @("source_spec", "source_plan")) {
        if (@($ledger.PSObject.Properties.Name) -contains $pathField) {
            $fullPath = Join-Path $root ([string]$ledger.$pathField)
            if (-not (Test-Path -LiteralPath $fullPath -PathType Leaf)) {
                $errors.Add("$pathField does not exist: $($ledger.$pathField)")
            }
        }
    }

    foreach ($needed in @("missing-proof", "dirty-unsafe-state", "failed-validation", "decision-outside-policy")) {
        if (@($ledger.stop_conditions) -notcontains $needed) {
            $errors.Add("stop_conditions missing $needed")
        }
    }
}

if ($errors.Count -gt 0) {
    Write-Error (($errors | ForEach-Object { "- $_" }) -join [Environment]::NewLine)
    exit 1
}

Write-Host "Workflow mode ledger valid: $ModeLedgerPath"
