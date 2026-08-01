import type {Screen,TraceSummary} from "./types";
export type HarnessAgent={id:string;name:string;file:string;content:string};
export type RunAgent={id:string;name:string;file:string};
export type RunStatus={runId:string;state:string;phase:string;phaseStatus?:string;loopType?:string;screenId?:string;eventId?:string;evidenceCount?:number;activeAgents?:RunAgent[]};
export type Evidence={id:string;runId:string;kind:string;artifactPath?:string;contentHash:string;summary:Record<string,unknown>;createdAt:string};
export type HumanGate={id:string;runId:string;gateType:string;decision?:string};
export type Project={id:string;name:string;targetPath:string;status:string};
export type HarnessDraft={agentId:string;content:string;version:number};
export type DesignSnapshot={screenId:string;eventId:string;designVersion:number;status:string};
export type ImplementationQueueItem={screenId:string;eventId:string;status:string};
export type MagazineJob={id:string;groupId?:string;status:string;stage:string;progress:number;format:string;privacyStatus:string;qualityScore?:number;riskScore?:number;outputPath?:string;createdAt:string};
export type MagazineVideo={id:string;videoId:string;title:string;channelTitle:string;viewCount:number;likeCount:number;commentCount:number;hotScore:number};
export type MagazineGroup={id:string;groupTitle:string;topicKeyword:string;itemCount:number;createdAt:string};
export type MagazineCollection={mode:string;collectedCount:number;savedCount:number;groupId:string;groupTitle:string};
export type MagazinePlanEntry={rankNo:number;sourceTitle:string;narration:string;sketchPrompt:string;sourceAttribution:{channelTitle:string;videoId:string}};
export type MagazinePlan={title:string;intro:string;entries:MagazinePlanEntry[];outro:string;estimatedDurationSec:number;quality:{score:number};risk:{score:number;level:string}};
export type MagazineGeneration={jobId:string;stage:string;qualityScore:number;riskScore:number;artifact:MagazinePlan};
export type MagazineRender={jobId:string;stage:string;manifest:{videoPath:string;durationSec:number;resolution:string;publishable:boolean}};
export type MagazineQuality={jobId:string;stage:string;qualityScore:number;report:{passed:boolean;score:number;publishable:boolean;measured:Record<string,unknown>;checks:Record<string,boolean>}};
const json=async<T>(path:string,init?:RequestInit):Promise<T>=>{
  const command=init?.method&&init.method!=="GET";
  const response=await fetch(`/api/v1${path}`,{...init,headers:{"Content-Type":"application/json","X-Request-ID":crypto.randomUUID(),...(command?{"X-Idempotency-Key":crypto.randomUUID(),"X-Actor":"web-user"}:{}),...(init?.headers||{})}});
  if(!response.ok)throw new Error((await response.json().catch(()=>null))?.detail||`HTTP ${response.status}`);
  return response.status===204?undefined as T:response.json();
};
export const api={
  screens:()=>json<Screen[]>("/screens"),
  projects:()=>json<Project[]>("/projects"),
  trace:(screenId:string,eventId:string)=>json<TraceSummary>(`/screens/${encodeURIComponent(screenId)}/events/${encodeURIComponent(eventId)}/trace`),
  snapshots:(screenId:string,eventId:string)=>json<DesignSnapshot[]>(`/design-snapshots?screenId=${encodeURIComponent(screenId)}&eventId=${encodeURIComponent(eventId)}`),
  implementationQueue:(screenId:string)=>json<ImplementationQueueItem[]>(`/implementation-queue?screenId=${encodeURIComponent(screenId)}`),
  approve:(screenId:string,eventId:string)=>json<{state:string;designVersion:string}>("/design-snapshots/approve",{method:"POST",body:JSON.stringify({screenId,eventId})}),
  reopen:(screenId:string,eventId:string)=>json<{state:string}>("/design-snapshots/reopen",{method:"POST",body:JSON.stringify({screenId,eventId})}),
  run:(loopType:string,screenId:string,eventId:string)=>json<RunStatus>("/runs",{method:"POST",body:JSON.stringify({loopType,screenId,eventId})}),
  harness:(loopType:string)=>json<HarnessAgent[]>(`/harnesses/${loopType}`),
  harnessDrafts:(loopType:string)=>json<HarnessDraft[]>(`/harnesses/${loopType}/drafts`),
  saveHarnessDraft:(loopType:string,agentId:string,content:string,version:number)=>json<{version:number}>(`/harnesses/${loopType}/drafts/${agentId}`,{method:"PUT",headers:{"If-Match":String(version)},body:JSON.stringify({content,actor:"web-user"})}),
  validateHarness:(loopType:string,agentId:string,content:string)=>json<{valid:boolean;errors:string[]}>(`/harnesses/${loopType}/${agentId}/validate`,{method:"POST",body:JSON.stringify({content})}),
  publishHarness:(loopType:string,files:Record<string,string>)=>json<{status:string;version:number}>(`/harnesses/${loopType}/publish`,{method:"POST",body:JSON.stringify({files})}),
  runStatus:(runId:string)=>json<RunStatus>(`/runs/${runId}`),
  advanceRun:(runId:string,evidencePass:boolean,summary:string)=>json<RunStatus>(`/runs/${runId}/advance`,{method:"POST",body:JSON.stringify({evidencePass,summary})}),
  retryRun:(runId:string)=>json<RunStatus>(`/runs/${runId}/retry`,{method:"POST"}),
  evidence:(runId:string)=>json<Evidence[]>(`/evidence?runId=${encodeURIComponent(runId)}`),
  gates:(runId:string)=>json<HumanGate[]>(`/human-gates?runId=${encodeURIComponent(runId)}`),
  decideGate:(gateId:string,decision:"APPROVE"|"REJECT")=>json<{state:string;decision:string}>(`/human-gates/${gateId}/decide`,{method:"POST",body:JSON.stringify({decision,actor:"ui-human"})}),
  cancelRun:(runId:string)=>json<{runId:string;state:string}>(`/runs/${runId}/cancel`,{method:"POST"}),
  magazineJobs:()=>json<MagazineJob[]>("/youtube-magazine/jobs"),
  magazineVideos:()=>json<MagazineVideo[]>("/youtube-magazine/videos"),
  magazineGroups:()=>json<MagazineGroup[]>("/youtube-magazine/groups"),
  collectMagazineVideos:()=>json<MagazineCollection>("/youtube-magazine/collect",{method:"POST",body:JSON.stringify({regionCode:"KR",categoryId:"24",maxResults:18})}),
  createMagazineJob:(format:"SHORTS"|"LONGFORM"="SHORTS",groupId?:string)=>json<MagazineJob>("/youtube-magazine/jobs",{method:"POST",body:JSON.stringify({format,groupId})}),
  generateMagazinePlan:(id:string)=>json<MagazineGeneration>(`/youtube-magazine/jobs/${id}/generate`,{method:"POST"}),
  renderMagazinePreview:(id:string)=>json<MagazineRender>(`/youtube-magazine/jobs/${id}/render-preview`,{method:"POST"}),
  checkMagazineQuality:(id:string)=>json<MagazineQuality>(`/youtube-magazine/jobs/${id}/quality-check`,{method:"POST"}),
  approveMagazineJob:(id:string)=>json<MagazineJob>(`/youtube-magazine/jobs/${id}/approve`,{method:"POST"}),
  prepareMagazineUpload:(id:string)=>json<MagazineJob>(`/youtube-magazine/jobs/${id}/upload`,{method:"POST"})
};
