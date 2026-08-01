param([string]$BaseUrl = "http://localhost:8080")
$ErrorActionPreference = "Stop"

$staticOutput=@(& "$PSScriptRoot\verify-static.ps1")
if($staticOutput.Count-lt 2){throw "Static verification did not return both reports"}
$staticReports=@($staticOutput|ForEach-Object{$_|ConvertFrom-Json})
if(@($staticReports|Where-Object status-ne"PASS").Count-ne 0){throw "Static verification failed"}

$matrix=& "$PSScriptRoot\verify-event-matrix.ps1" -BaseUrl $BaseUrl|ConvertFrom-Json
if($matrix.status-ne"PASS"-or$matrix.verified-ne 15){throw "Event matrix failed"}

$e2e=& "$PSScriptRoot\verify-e2e.ps1" -BaseUrl $BaseUrl|ConvertFrom-Json
if($e2e.status-ne"PASS"-or$e2e.humanDecision-ne"APPROVE"){throw "End-to-end verification failed"}

$health=Invoke-RestMethod "$BaseUrl/actuator/health"
if($health.status-ne"UP"){throw "Health endpoint is not UP"}
$page=Invoke-WebRequest "$BaseUrl/" -UseBasicParsing
$securityHeaders=@("Content-Security-Policy","X-Content-Type-Options","X-Frame-Options","Referrer-Policy","Permissions-Policy")
foreach($header in $securityHeaders){if(-not$page.Headers[$header]){throw "Missing security header: $header"}}
foreach($path in @("/actuator/metrics","/actuator/info","/actuator/env")){
 try{Invoke-WebRequest "$BaseUrl$path" -UseBasicParsing|Out-Null;throw "Management endpoint exposed: $path"}
 catch{if($_.Exception.Message-eq"Management endpoint exposed: $path"){throw};if([int]$_.Exception.Response.StatusCode-ne 404){throw "Unexpected status for $path"}}
}

[ordered]@{
 status="PASS"
 staticReports=$staticReports.Count
 screens=$matrix.screens
 events=$matrix.events
 eventMatrix=$matrix.verified
 designRun=$e2e.designRun
 implementationRun=$e2e.implementationRun
 evidence=$e2e.evidence
 evaluationScore=$e2e.evaluationScore
 securityHeaders=$securityHeaders.Count
 health=$health.status
}|ConvertTo-Json -Compress
