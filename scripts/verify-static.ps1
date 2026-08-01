$ErrorActionPreference="Stop"
$root=Split-Path -Parent $PSScriptRoot
powershell -NoProfile -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot "verify-baseline.ps1")
$manifest=Get-Content -Raw -Encoding UTF8 (Join-Path $root "contracts/event-manifest.json")|ConvertFrom-Json
$frontend=Get-Content -Raw -Encoding UTF8 (Join-Path $root "frontend/src/event-manifest.json")|ConvertFrom-Json
$ids=@($manifest.screens.events.id);$frontIds=@($frontend.screens.events.id)
if((Compare-Object $ids $frontIds)){throw "FATAL_EVENT_DRIFT: frontend manifest mismatch"}
$designAgents=@(Get-ChildItem (Join-Path $root "harness/design") -Filter "*-agent.md")
$codeAgents=@(Get-ChildItem (Join-Path $root "harness/code") -Filter "*-agent.md")
if($designAgents.Count-ne 5-or$codeAgents.Count-ne 5){throw "Harness count drift"}
$ssot=@(Get-ChildItem (Join-Path $root "ssot/design") -Recurse -Filter "manifest.json")
if($ssot.Count-ne 15){throw "SSOT event count drift: $($ssot.Count)"}
$requiredTests=@("frontend/src/manifest.test.ts","backend/src/test/java/io/forgeflow/registry/EventRegistryTest.java","backend/src/test/java/io/forgeflow/workloop/LoopOrchestratorTest.java","backend/src/test/java/io/forgeflow/harness/HarnessRegistryTest.java","backend/src/test/java/io/forgeflow/evaluation/HardGateEvaluatorTest.java")
foreach($test in $requiredTests){if(-not(Test-Path (Join-Path $root $test))){throw "Required contract test missing: $test"}}
$emptyHashes=@($ssot | ForEach-Object { (Get-Content -Raw -Encoding UTF8 $_.FullName|ConvertFrom-Json).contentHash } | Where-Object {[string]::IsNullOrWhiteSpace($_)})
if($emptyHashes.Count -gt 0){throw "SSOT content hash missing"}
$forbidden=rg -n "full.?access|dangerously.?bypass|--no.?sandbox" backend worker compose.yml
if($LASTEXITCODE-eq 0){throw "Forbidden worker capability: $forbidden"}
Write-Output (@{status="PASS";screens=5;events=15;designAgents=5;codeAgents=5;eventSsot=15}|ConvertTo-Json -Compress)
