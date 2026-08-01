param([string]$BaseUrl = "http://localhost:8080")
$ErrorActionPreference = "Stop"
function PostJson([string]$Path, $Body, [string]$IdempotencyKey = ([guid]::NewGuid().ToString())) {
  $bytes = [Text.Encoding]::UTF8.GetBytes(($Body | ConvertTo-Json -Depth 8))
  $headers = @{"X-Request-ID"=[guid]::NewGuid().ToString();"X-Idempotency-Key"=$IdempotencyKey;"X-Actor"="e2e-user"}
  try { Invoke-RestMethod "$BaseUrl$Path" -Method Post -ContentType "application/json; charset=utf-8" -Headers $headers -Body $bytes }
  catch {
    $detail = $_.ErrorDetails.Message
    throw "POST $Path failed: $detail"
  }
}
function PutJson([string]$Path, $Body, [int]$Version) {
  $bytes = [Text.Encoding]::UTF8.GetBytes(($Body | ConvertTo-Json -Depth 8))
  $headers = @{"X-Request-ID"=[guid]::NewGuid().ToString();"X-Idempotency-Key"=[guid]::NewGuid().ToString();"X-Actor"="e2e-user";"If-Match"=([string]$Version)}
  Invoke-RestMethod "$BaseUrl$Path" -Method Put -ContentType "application/json; charset=utf-8" -Headers $headers -Body $bytes
}
$screens = Invoke-RestMethod "$BaseUrl/api/v1/screens"
if ($screens.Count -ne 5) { throw "Expected five screens" }
$events = Invoke-RestMethod "$BaseUrl/api/v1/screens/SCR-CONSULT-LIST/events"
if ($events.Count -ne 4) { throw "Screen event registry failed" }
$trace = Invoke-RestMethod "$BaseUrl/api/v1/screens/SCR-CONSULT-LIST/events/EVT-01/trace"
if ($trace.requirements -notcontains "REQ-01") { throw "Trace contract failed" }

$artifact = PostJson "/api/v1/design-artifacts" @{screenId="SCR-CONSULT-LIST";eventId="EVT-01";artifactType="REQUIREMENTS";content="The event shall load and trace approved consultation records.";actor="e2e-user"}
$artifactEvaluation = PostJson "/api/v1/design-artifacts/$($artifact.id)/evaluate" @{}
if (-not $artifactEvaluation.passed) { throw "Design artifact evaluation failed" }
$artifactUpdated = PutJson "/api/v1/design-artifacts/$($artifact.id)" @{content="The event shall load, validate, and trace approved consultation records."} ([int]$artifact.artifactVersion)
if ($artifactUpdated.artifactVersion -ne ([int]$artifact.artifactVersion+1)) { throw "Design artifact version update failed" }
try { PutJson "/api/v1/design-artifacts/$($artifact.id)" @{content="stale writer must fail"} ([int]$artifact.artifactVersion); throw "Stale artifact version accepted" } catch { if ($_.Exception.Message -eq "Stale artifact version accepted") { throw } }

$thread = PostJson "/api/v1/threads" @{name="E2E thread";loopType="DESIGN"}
$threadUpdate = PutJson "/api/v1/threads/$($thread.id)" @{name="E2E renamed"} 1
if ($threadUpdate.version -ne 2) { throw "Thread optimistic update failed" }
try { PutJson "/api/v1/threads/$($thread.id)" @{name="stale update"} 1; throw "Stale If-Match accepted" } catch { if ($_.Exception.Message -eq "Stale If-Match accepted") { throw } }

$codeDrafts = Invoke-RestMethod "$BaseUrl/api/v1/harnesses/code/drafts"
$existingDraft = $codeDrafts | Where-Object agentId -eq "implementation"
$draftVersion = if ($existingDraft) { [int]$existingDraft.version } else { 0 }
$draft = PutJson "/api/v1/harnesses/code/drafts/implementation" @{content="# E2E implementation draft`nEvidence Before Score";actor="e2e-user"} $draftVersion
$diff = Invoke-RestMethod "$BaseUrl/api/v1/harnesses/code/diff"
if ($draft.version -ne ($draftVersion+1) -or $diff.changedAgents -notcontains "implementation") { throw "Harness draft/diff failed" }

$duplicateKey = [guid]::NewGuid().ToString()
$duplicateProbe = PostJson "/api/v1/runs" @{loopType="DESIGN";screenId="SCR-OWNER-SEARCH";eventId="EVT-32"} $duplicateKey
$duplicateReplay = PostJson "/api/v1/runs" @{loopType="DESIGN";screenId="SCR-OWNER-SEARCH";eventId="EVT-32"} $duplicateKey
if ($duplicateReplay.runId -ne $duplicateProbe.runId) { throw "Idempotent response replay created a second run" }
try { PostJson "/api/v1/threads" @{name="wrong key reuse";loopType="DESIGN"} $duplicateKey; throw "Cross-command key reuse accepted" } catch { if ($_.Exception.Message -eq "Cross-command key reuse accepted") { throw } }

$cancelProbe = PostJson "/api/v1/runs" @{loopType="DESIGN";screenId="SCR-OWNER-SEARCH";eventId="EVT-31"}
$cancelProbe = PostJson "/api/v1/runs/$($cancelProbe.runId)/cancel" @{}
if ($cancelProbe.state -ne "CANCELLED") { throw "Cancellation failed" }
$retryProbe = PostJson "/api/v1/runs/$($cancelProbe.runId)/retry" @{}
if ($retryProbe.state -ne "A_REVIEW" -or $retryProbe.runId -eq $cancelProbe.runId) { throw "Retry must create a new run" }
$retryProbe = PostJson "/api/v1/runs/$($retryProbe.runId)/advance" @{evidencePass=$false;summary="negative evidence probe"}
if ($retryProbe.state -ne "B_REPAIR" -or $retryProbe.phaseStatus -ne "BLOCKED_EVIDENCE") { throw "Evidence block failed" }

$design = PostJson "/api/v1/design-runs" @{screenId="SCR-CONSULT-LIST";eventId="EVT-01"}
foreach ($step in 1..10) {
  $design = PostJson "/api/v1/runs/$($design.runId)/advance" @{evidencePass=$true;summary="e2e design evidence $step"}
}
if ($design.phase -ne "D10_HUMAN_APPROVAL") { throw "Design phase progression failed: $($design.phase)" }
$design = PostJson "/api/v1/runs/$($design.runId)/advance" @{evidencePass=$true;summary="design review complete"}
if ($design.state -ne "A_REVIEW") { throw "Design terminal state failed" }
$snapshot = PostJson "/api/v1/design-snapshots/approve" @{screenId="SCR-CONSULT-LIST";eventId="EVT-01"}
if ($snapshot.state -ne "IMPLEMENTATION_READY") { throw "Design approval failed" }

$implement = PostJson "/api/v1/implementation-runs" @{screenId="SCR-CONSULT-LIST";eventId="EVT-01"}
foreach ($step in 1..12) {
  $implement = PostJson "/api/v1/runs/$($implement.runId)/advance" @{evidencePass=$true;summary="e2e implementation evidence $step"}
}
if ($implement.phase -ne "C12_HUMAN_TEST") { throw "Implementation phase progression failed: $($implement.phase)" }
$implement = PostJson "/api/v1/runs/$($implement.runId)/advance" @{evidencePass=$true;summary="human test ready"}
if ($implement.state -ne "HUMAN_TEST") { throw "HUMAN_TEST transition failed" }
$gates = Invoke-RestMethod "$BaseUrl/api/v1/human-gates?runId=$($implement.runId)"
if ($gates.Count -ne 1) { throw "Human gate creation failed" }
$decision = PostJson "/api/v1/human-gates/$($gates[0].id)/decide" @{decision="APPROVE";actor="e2e-human";comment="verified"}
if ($decision.state -ne "ACCEPTED") { throw "Human acceptance failed" }
$final = Invoke-RestMethod "$BaseUrl/api/v1/runs/$($implement.runId)"
if ($final.state -ne "ACCEPTED" -or [int]$final.evidenceCount -ne 14) { throw "Final persisted state failed" }
$evaluation = Invoke-RestMethod "$BaseUrl/api/v1/evaluations/$($implement.runId)"
if (-not $evaluation.passed -or [int]$evaluation.score -lt 90) { throw "Evidence-backed evaluation failed" }

$harnesses = Invoke-RestMethod "$BaseUrl/api/v1/harnesses/design"
if ($harnesses.Count -ne 3) { throw "Harness registry failed" }
$files = @{}
foreach ($item in $harnesses) { $files[$item.id] = $item.content }
$published = PostJson "/api/v1/harnesses/design/publish" @{files=$files}
if ($published.status -ne "PUBLISHED" -or $published.fileCount -ne 3) { throw "Harness atomic publish failed" }

[ordered]@{
  status = "PASS"
  screens = $screens.Count
  designRun = $design.runId
  implementationRun = $implement.runId
  evidence = $final.evidenceCount
  humanDecision = $decision.decision
  harnessVersion = $published.version
  retry = "PASS"
  negativeEvidence = "PASS"
  evaluationScore = $evaluation.score
  idempotencyReplay = "PASS"
  optimisticLock = "PASS"
  harnessDraftDiff = "PASS"
  designArtifact = "PASS"
  screenEvents = $events.Count
} | ConvertTo-Json -Compress
