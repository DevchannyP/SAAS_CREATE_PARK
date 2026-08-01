$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$manifestPath = Join-Path $root "contracts/event-manifest.json"
$manifest = Get-Content -Raw -Encoding UTF8 $manifestPath | ConvertFrom-Json
$manifestHash = (Get-FileHash -Algorithm SHA256 $manifestPath).Hash.ToLowerInvariant()
foreach ($screen in $manifest.screens) {
  foreach ($event in $screen.events) {
    $dir = Join-Path $root ("ssot/design/{0}/{1}" -f $screen.id,$event.id)
    New-Item -ItemType Directory -Force $dir | Out-Null
    $base = [ordered]@{version=1;screenId=$screen.id;eventId=$event.id;eventName=$event.name;eventKind=$event.kind;status="DRAFT";sourceRefs=@("contracts/event-manifest.json");contentHash=""}
    $req = [ordered]@{} + $base
    $req.requirements = @([ordered]@{id=("REQ-{0}" -f $event.id.Substring(4));classification="FACT";trigger=$event.name;preconditions=@("authenticated actor");input=@();processing=@($event.name);output=@();failureCases=@("FORBIDDEN","VALIDATION_ERROR","SYSTEM_ERROR");prohibitedBehaviors=@("cross-event access");authorization="server enforced";transactionType=($(if($event.kind -eq "COMMAND"){"READ_WRITE"}else{"READ_ONLY"}));acceptanceCriteria=@("event contract preserved");verificationMethods=@("contract test");evidenceRefs=@("contracts/event-manifest.json");confidence=1.0})
    $mock = [ordered]@{} + $base; $mock.states=@("NORMAL","EMPTY","LOADING","ERROR","FORBIDDEN");$mock.controls=@();$mock.fixture=@{}
    $api = [ordered]@{} + $base; $api.operations=$(if(@("NAVIGATION")-contains $event.kind){@()}else{@([ordered]@{operationId=("API-"+$event.id.Substring(4));authorization="required";transactionType=$req.requirements[0].transactionType})})
    $data = [ordered]@{} + $base; $data.entities=@();$data.readEvents=$(if($event.kind -eq "QUERY"){@($event.id)}else{@()});$data.writeEvents=$(if($event.kind -eq "COMMAND"){@($event.id)}else{@()})
    $review = [ordered]@{} + $base;$review.findings=@();$review.hardGate="PENDING_EVIDENCE"
    $refs=[ordered]@{requirements="requirements.ref.json";mockup="mockup.ref.json";apiContract="api-contract.ref.json";dataModel="data-model.ref.json";review="review.ref.json"}
    $design=[ordered]@{}+$base;$design.eventManifestHash=$manifestHash;$design.artifactRefs=$refs;$design.designVersion=1
    $files=@{"manifest.json"=$design;"requirements.ref.json"=$req;"mockup.ref.json"=$mock;"api-contract.ref.json"=$api;"data-model.ref.json"=$data;"review.ref.json"=$review}
    $files.GetEnumerator() | ForEach-Object { $_.Value | ConvertTo-Json -Depth 12 | Set-Content -Encoding UTF8 (Join-Path $dir $_.Key) }
    $payload=($files.GetEnumerator() | Sort-Object Key | ForEach-Object { $_.Key+":"+($_.Value | ConvertTo-Json -Depth 12 -Compress) }) -join "`n"
    $bytes=[Text.Encoding]::UTF8.GetBytes($payload);$digest=[Security.Cryptography.SHA256]::Create().ComputeHash($bytes);$hash=([BitConverter]::ToString($digest)-replace '-','').ToLowerInvariant()
    $files.GetEnumerator() | ForEach-Object { $_.Value.contentHash=$hash; $_.Value | ConvertTo-Json -Depth 12 | Set-Content -Encoding UTF8 (Join-Path $dir $_.Key) }
  }
}
Write-Output "Generated event SSOT for 5 screens / 15 events"
