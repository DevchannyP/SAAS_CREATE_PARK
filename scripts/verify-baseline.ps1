$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$mockup = Join-Path $root "ai_development_saas_loop_threads_mockup_v6_ssot_soft_orbit.html"
$manifest = Join-Path $root "contracts/event-manifest.json"
$expected = "117234D672D24C8E7E0093A0A035445A5B56E41A21DDB662C89D617BF6DCE184"
$actual = (Get-FileHash -Algorithm SHA256 -LiteralPath $mockup).Hash
if ($actual -ne $expected) { throw "FATAL_EVENT_DRIFT: baseline hash $actual" }
$data = Get-Content -Raw -Encoding UTF8 -LiteralPath $manifest | ConvertFrom-Json
if ($data.screens.Count -ne 5) { throw "FATAL_EVENT_DRIFT: screen count" }
$events = @($data.screens | ForEach-Object { $_.events })
if ($events.Count -ne 15) { throw "FATAL_EVENT_DRIFT: event count" }
if (($events.id | Sort-Object -Unique).Count -ne 15) { throw "FATAL_EVENT_DRIFT: duplicate event" }
Write-Output (@{ baseline=$actual; screens=5; events=15; status="PASS" } | ConvertTo-Json -Compress)
