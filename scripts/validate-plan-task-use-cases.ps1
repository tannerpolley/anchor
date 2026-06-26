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
    $taskMatches = [regex]::Matches($content, "(?ms)^### Task \d+: .+?(?=^### Task \d+: |\z)")

    if ($taskMatches.Count -eq 0) {
        $errors.Add("No numbered task sections found")
    }

    foreach ($match in $taskMatches) {
        $task = $match.Value
        $title = (($task -split "`r?`n")[0]).Trim()
        if ($task -notmatch "(?m)^\*\*Use Cases:\*\*\s*$") {
            $errors.Add("$title missing **Use Cases:** block")
            continue
        }

        $useCaseBlock = [regex]::Match($task, "(?ms)^\*\*Use Cases:\*\*\s*(.+?)^\*\*Files:\*\*")
        if (-not $useCaseBlock.Success) {
            $errors.Add("$title has no files boundary after use cases")
            continue
        }

        $useCases = $useCaseBlock.Groups[1].Value
        if ($useCases -notmatch "(?m)^- .+") {
            $errors.Add("$title has no concrete use-case bullets")
        }
    }
}

if ($errors.Count -gt 0) {
    Write-Error (($errors | ForEach-Object { "- $_" }) -join [Environment]::NewLine)
    exit 1
}

Write-Host "Task use cases valid: $PlanPath"
