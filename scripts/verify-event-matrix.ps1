param([string]$BaseUrl = "http://localhost:8080")
$ErrorActionPreference = "Stop"
function PostJson([string]$Path,$Body) {
  $headers=@{"X-Request-ID"=[guid]::NewGuid().ToString();"X-Idempotency-Key"=[guid]::NewGuid().ToString();"X-Actor"="event-matrix"}
  $bytes=[Text.Encoding]::UTF8.GetBytes(($Body|ConvertTo-Json -Depth 6))
  Invoke-RestMethod "$BaseUrl$Path" -Method Post -ContentType "application/json; charset=utf-8" -Headers $headers -Body $bytes
}
function ExpectRejected([scriptblock]$Action,[int]$Status) {
  try { & $Action; throw "Expected HTTP $Status rejection" }
  catch {
    if($_.Exception.Message -eq "Expected HTTP $Status rejection"){throw}
    if([int]$_.Exception.Response.StatusCode -ne $Status){throw}
  }
}

$screens=Invoke-RestMethod "$BaseUrl/api/v1/screens"
$events=@($screens|ForEach-Object{$screen=$_;@($screen.events)|ForEach-Object{[pscustomobject]@{screenId=$screen.id;eventId=$_.id}}})
if($screens.Count-ne 5-or $events.Count-ne 15){throw "Registry cardinality mismatch"}

ExpectRejected { PostJson "/api/v1/design-runs" @{screenId="SCR-CONSULT-LIST";eventId="EVT-43"} } 400
PostJson "/api/v1/design-snapshots/reopen" @{screenId="SCR-CONSULT-REG";eventId="EVT-11"}|Out-Null
ExpectRejected { PostJson "/api/v1/implementation-runs" @{screenId="SCR-CONSULT-REG";eventId="EVT-11"} } 409

$verified=0
foreach($item in $events){
  $owned=Invoke-RestMethod "$BaseUrl/api/v1/screens/$($item.screenId)/events"
  if($owned.id-notcontains $item.eventId){throw "Ownership lookup failed for $($item.eventId)"}
  $design=PostJson "/api/v1/design-runs" $item
  $designStatus=Invoke-RestMethod "$BaseUrl/api/v1/runs/$($design.runId)"
  if($designStatus.loopType-ne"DESIGN"-or$designStatus.screenId-ne$item.screenId-or$designStatus.eventId-ne$item.eventId){throw "Design context drift for $($item.eventId)"}
  PostJson "/api/v1/runs/$($design.runId)/cancel" @{}|Out-Null
  $approval=PostJson "/api/v1/design-snapshots/approve" $item
  if($approval.state-ne"IMPLEMENTATION_READY"){throw "Approval failed for $($item.eventId)"}
  $implementation=PostJson "/api/v1/implementation-runs" $item
  $implementationStatus=Invoke-RestMethod "$BaseUrl/api/v1/runs/$($implementation.runId)"
  if($implementationStatus.loopType-ne"IMPLEMENT"-or$implementationStatus.screenId-ne$item.screenId-or$implementationStatus.eventId-ne$item.eventId){throw "Implementation context drift for $($item.eventId)"}
  PostJson "/api/v1/runs/$($implementation.runId)/cancel" @{}|Out-Null
  $verified++
}

[ordered]@{status="PASS";screens=$screens.Count;events=$events.Count;verified=$verified;invalidOwnership="REJECTED";approvalGate="REJECTED"}|ConvertTo-Json -Compress
