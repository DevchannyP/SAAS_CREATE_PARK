package io.forgeflow.run;
import java.time.Duration;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import io.forgeflow.worker.WorkerRuntimeService;
import io.forgeflow.workloop.WorkloopService;
import io.forgeflow.harness.HarnessRegistry;

@RestController @RequestMapping("/api/v1/runs")
public class RunController {
 private final CopyOnWriteArrayList<SseEmitter> subscribers=new CopyOnWriteArrayList<>();
 private final JdbcTemplate db;
 private final WorkerRuntimeService worker;
 private final WorkloopService workloops;
 private final HarnessRegistry harnesses;
 RunController(JdbcTemplate db,WorkerRuntimeService worker,WorkloopService workloops,HarnessRegistry harnesses){this.db=db;this.worker=worker;this.workloops=workloops;this.harnesses=harnesses;}
 @GetMapping("/{runId}") Map<String,Object> status(@PathVariable String runId){
  var rows=db.queryForList("select r.id as \"runId\",r.loop_type as \"loopType\",r.screen_id as \"screenId\",r.event_id as \"eventId\",r.state,r.iteration,p.internal_phase as phase,p.status as \"phaseStatus\",(select count(*) from evidence e where e.run_id=r.id) as \"evidenceCount\" from workloop_run r left join lateral (select internal_phase,status from run_phase where run_id=r.id order by started_at desc nulls last,id desc limit 1) p on true where r.id=?::uuid",runId);
  if(rows.isEmpty())throw new IllegalArgumentException("Run not found");
  var result=new java.util.LinkedHashMap<String,Object>(rows.getFirst());
  String loop=String.valueOf(result.get("loopType")),phase="HUMAN_TEST".equals(String.valueOf(result.get("state")))?"C12_HUMAN_TEST":String.valueOf(result.get("phase"));
  result.put("activeAgents",harnesses.assigned(loop,phase).stream().map(a->Map.of("id",a.id(),"name",a.name(),"file",a.file())).toList());
  return result;
 }
 @GetMapping(value="/{runId}/events",produces=MediaType.TEXT_EVENT_STREAM_VALUE)
 SseEmitter events(@PathVariable String runId){
  var emitter=new SseEmitter(Duration.ofMinutes(10).toMillis());subscribers.add(emitter);emitter.onCompletion(()->subscribers.remove(emitter));emitter.onTimeout(()->subscribers.remove(emitter));
  try{emitter.send(SseEmitter.event().name("run-state").data(status(runId)));}catch(Exception e){emitter.completeWithError(e);}
  return emitter;
 }
 @Transactional @PostMapping("/{runId}/cancel") Map<String,String> cancel(@PathVariable String runId){
  int n=db.update("update workloop_run set state='CANCELLED',updated_at=now() where id=?::uuid and state not in ('ACCEPTED','CANCELLED')",runId);
  if(n!=1)throw new IllegalStateException("Run is terminal or missing");
  db.update("update run_phase set status='CANCELLED',ended_at=now() where run_id=?::uuid and status='RUNNING'",runId);
  publish(runId);return Map.of("runId",runId,"state","CANCELLED");
 }
 @Transactional @PostMapping("/{runId}/retry") Map<String,String> retry(@PathVariable String runId){
  var old=status(runId);String state=String.valueOf(old.get("state"));
  if(!Set.of("B_REPAIR","CANCELLED").contains(state))throw new IllegalStateException("Only repaired or cancelled runs may retry");
  var next=workloops.start(String.valueOf(old.get("loopType")),String.valueOf(old.get("screenId")),String.valueOf(old.get("eventId")));
  db.update("update workloop_run set iteration=? where id=?::uuid",((Number)old.get("iteration")).intValue()+1,next.get("runId"));
  db.update("insert into audit_log(actor,action,target_type,target_id,outcome,details) values ('system','RUN_RETRIED','workloop_run',?,'SUCCESS',jsonb_build_object('previousRunId',?))",next.get("runId"),runId);
  return next;
 }
 record Advance(boolean evidencePass,String summary){}
 private static final List<String> DESIGN=List.of("D00_SNAPSHOT_FREEZE","D01_SCOPE_EVIDENCE","D02_REQUIREMENTS","D03_ARTIFACTS","D04_API_ARCHITECTURE","D05_CROSS_CHECK","D06_INDEPENDENT_REVIEW","D07_MINIMUM_REPAIR","D08_TRACE_REGRESSION","D09_SNAPSHOT","D10_HUMAN_APPROVAL");
 private static final List<String> IMPLEMENT=List.of("C00_SNAPSHOT_VERIFY","C01_EVENT_CONTEXT","C02_REPOSITORY_MAP","C03_IMPLEMENTATION_PLAN","C04_VERTICAL_SLICE","C05_COMPILE","C06_TEST","C07_SECURITY_PERF","C08_CODE_REVIEW","C09_MINIMUM_REPAIR","C10_REGRESSION","C11_PATCH_BUNDLE","C12_HUMAN_TEST");
 @Transactional @PostMapping("/{runId}/advance") Map<String,Object> advance(@PathVariable String runId,@RequestBody Advance request){
  var current=status(runId);String phase=String.valueOf(current.get("phase"));String loop=String.valueOf(current.get("loopType"));List<String> phases="DESIGN".equals(loop)?DESIGN:IMPLEMENT;
  if(!request.evidencePass()){
   db.update("update run_phase set status='BLOCKED_EVIDENCE',ended_at=now() where run_id=?::uuid and internal_phase=? and status='RUNNING'",runId,phase);
   db.update("update workloop_run set state='B_REPAIR',iteration=iteration+1,updated_at=now() where id=?::uuid",runId);
   publish(runId);return status(runId);
  }
 db.update("insert into evidence(run_id,kind,content_hash,summary) values (?::uuid,'PHASE_RESULT',encode(digest(?::bytea,'sha256'),'hex'),jsonb_build_object('phase',?,'summary',?))",runId,runId+phase,phase,request.summary()==null?"verified":request.summary());
  if("C04_VERTICAL_SLICE".equals(phase))try{
   var result=worker.executeFake(runId);
   db.update("insert into evidence(run_id,kind,artifact_path,content_hash,summary) values (?::uuid,'WORKER_RESULT',?,encode(digest(?::bytea,'sha256'),'hex'),jsonb_build_object('exitCode',?,'adapter','fake'))",runId,result.patch().toString(),runId+"worker",result.exitCode());
  }catch(Exception e){throw new IllegalStateException("Isolated worker failed",e);}
  db.update("update run_phase set status='PASS',ended_at=now(),evidence_refs=jsonb_build_array(?) where run_id=?::uuid and internal_phase=? and status='RUNNING'",phase,runId,phase);
  int index=phases.indexOf(phase);
  if(index<0)throw new IllegalStateException("Unknown phase");
  if(index==phases.size()-1){
   String state="DESIGN".equals(loop)?"A_REVIEW":"HUMAN_TEST";
   db.update("update workloop_run set state=?,updated_at=now() where id=?::uuid",state,runId);
   if("IMPLEMENT".equals(loop))db.update("insert into human_gate(run_id,gate_type) select ?::uuid,'HUMAN_TEST' where not exists(select 1 from human_gate where run_id=?::uuid)",runId,runId);
  }else{
   String next=phases.get(index+1);db.update("insert into run_phase(run_id,internal_phase,status,started_at) values (?::uuid,?,'RUNNING',now())",runId,next);
  }
  if("C11_PATCH_BUNDLE".equals(phase))db.update("insert into patch_bundle(run_id,design_snapshot_id,design_version,screen_id,event_id,changed_files,changed_symbols,unified_diff,findings,evidence_refs,content_hash,status) select id,design_snapshot_id,(select design_version from design_snapshot where id=design_snapshot_id),screen_id,event_id,'[]','[]','', '[]',jsonb_build_array(?),encode(digest((id::text||?)::bytea,'sha256'),'hex'),'READY_FOR_HUMAN' from workloop_run where id=?::uuid",phase,phase,runId);
  publish(runId);return status(runId);
 }
 private void publish(String runId){for(var emitter:subscribers){try{emitter.send(SseEmitter.event().name("run-state").data(status(runId)));}catch(Exception e){emitter.complete();subscribers.remove(emitter);}}}
}
