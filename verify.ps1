# One entry point for verifying SpongeCoach. Runs every check and prints a single verdict.
#
#   .\verify.ps1                 # everything
#   .\verify.ps1 -SkipE2e        # skip Playwright (slowest stage)
#   .\verify.ps1 -Only spec      # one stage: spec | backend | frontend | e2e
#
# Backend tests need Docker running (Quarkus Dev Services starts Postgres in a container).

param(
    [switch]$SkipE2e,
    [ValidateSet('spec', 'backend', 'frontend', 'e2e')]
    [string]$Only
)

$ErrorActionPreference = 'Continue'
$repoRoot = $PSScriptRoot
$results = [ordered]@{}

function Invoke-Stage {
    param([string]$Name, [string]$WorkingDirectory, [scriptblock]$Command)

    if ($Only -and $Only -ne $Name) { return }

    Write-Host ""
    Write-Host "=== $Name " -NoNewline
    Write-Host ("=" * [Math]::Max(0, 60 - $Name.Length))
    Push-Location $WorkingDirectory
    try {
        & $Command
        $ok = $LASTEXITCODE -eq 0
    } finally {
        Pop-Location
    }
    $script:results[$Name] = if ($ok) { 'PASS' } else { 'FAIL' }
}

# Backend runs before spec: the backend suite executes docs/spec/*.feature through Cucumber, and
# the spec stage reads that run's report to confirm every scenario was actually executed.
if (-not $Only -or $Only -eq 'backend') {
    docker info *> $null
    if ($LASTEXITCODE -ne 0) {
        Write-Host ""
        Write-Host "=== backend ================================================="
        Write-Host "Docker is not running - backend tests need it for Dev Services Postgres." -ForegroundColor Yellow
        $results['backend'] = 'SKIPPED (no Docker)'
    } else {
        Invoke-Stage 'backend' (Join-Path $repoRoot 'backend') { & ./gradlew.bat test --console=plain }
    }
}

Invoke-Stage 'spec' $repoRoot { node scripts/check-spec-coverage.mjs }

Invoke-Stage 'frontend' (Join-Path $repoRoot 'frontend') { npx jest --ci }

if (-not $SkipE2e) {
    Invoke-Stage 'e2e' (Join-Path $repoRoot 'frontend') { npx playwright test }
}

Write-Host ""
Write-Host "=== verdict ================================================="
foreach ($stage in $results.Keys) {
    $status = $results[$stage]
    $color = if ($status -eq 'PASS') { 'Green' } elseif ($status -eq 'FAIL') { 'Red' } else { 'Yellow' }
    Write-Host ("{0,-10} {1}" -f $stage, $status) -ForegroundColor $color
}

if ($results.Values -contains 'FAIL') { exit 1 }
exit 0
