package io.forgeflow.workloop;
import io.forgeflow.registry.EventRegistry;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkloopService{
 private final JdbcTemplate db; private final EventRegistry registry;
 public WorkloopService(JdbcTemplate db,EventRegistry registry){this.db=db;this.registry=registry;}
 @Transactional public Map<String,String> approve(String screen,String event){
  registry.require(screen,event);String id=UUID.randomUUID().toString();
  Integer latest=db.queryForObject("select coalesce(max(design_version),0) from design_snapshot where screen_id=? and event_id=?",Integer.class,screen,event);int version=(latest==null?0:latest)+1;
  db.update("update implementation_queue set status='STALE' where screen_id=? and event_id=? and status='IMPLEMENTATION_READY'",screen,event);
  db.update("update design_snapshot set status='SUPERSEDED' where screen_id=? and event_id=? and status='A_APPROVED'",screen,event);
  db.update("insert into design_snapshot(id,screen_id,event_id,design_version,status,content_hash,approved_at) values (?::uuid,?,?,?,'A_APPROVED',encode(digest((?||?||?)::bytea,'sha256'),'hex'),now())",id,screen,event,version,screen,event,version);
  db.update("insert into implementation_queue(id,snapshot_id,screen_id,event_id,status) values (gen_random_uuid(),?::uuid,?,?,'IMPLEMENTATION_READY')",id,screen,event);
  return Map.of("snapshotId",id,"designVersion",Integer.toString(version),"state","IMPLEMENTATION_READY");
 }
 @Transactional public Map<String,String> reopen(String screen,String event){
  registry.require(screen,event);
  db.update("update implementation_queue set status='STALE' where screen_id=? and event_id=? and status<>'STALE'",screen,event);
  db.update("update patch_bundle set status='STALE' where screen_id=? and event_id=? and status<>'STALE'",screen,event);
  db.update("update design_snapshot set status='STALE' where screen_id=? and event_id=? and status='A_APPROVED'",screen,event);
  return Map.of("state","A_DRAFT");
 }
 public Map<String,String> start(String loop,String screen,String event){
  registry.require(screen,event);
  if(!Set.of("DESIGN","IMPLEMENT").contains(loop))throw new IllegalArgumentException("loopType must be DESIGN or IMPLEMENT");
  String snapshotId=null;
  if("IMPLEMENT".equals(loop)){
   var ready=db.queryForList("select snapshot_id from implementation_queue where screen_id=? and event_id=? and status='IMPLEMENTATION_READY' order by created_at desc limit 1",screen,event);
   if(ready.isEmpty())throw new IllegalStateException("Design approval required");
   snapshotId=ready.getFirst().get("snapshot_id").toString();
  }
  String id=UUID.randomUUID().toString(),state="IMPLEMENT".equals(loop)?"B_RUNNING":"A_REVIEW";
  String phase="IMPLEMENT".equals(loop)?"C00_SNAPSHOT_VERIFY":"D00_SNAPSHOT_FREEZE";
  db.update("insert into workloop_run(id,loop_type,screen_id,event_id,state,design_snapshot_id) values (?::uuid,?,?,?,?,?::uuid)",id,loop,screen,event,state,snapshotId);
  db.update("insert into run_phase(run_id,internal_phase,status,started_at) values (?::uuid,?,'RUNNING',now())",id,phase);
  audit("system","RUN_STARTED","workloop_run",id,"SUCCESS",Map.of("loopType",loop,"screenId",screen,"eventId",event));
  return Map.of("runId",id,"state",state,"phase",phase);
 }
 public List<Map<String,Object>> queue(String screen){return db.queryForList("select id,snapshot_id as snapshotId,screen_id as screenId,event_id as eventId,status,created_at as createdAt from implementation_queue where screen_id=? order by created_at",screen);}
 public List<Map<String,Object>> queue(String screen,String event){registry.require(screen,event);return db.queryForList("select id,snapshot_id as snapshotId,screen_id as screenId,event_id as eventId,status,created_at as createdAt from implementation_queue where screen_id=? and event_id=? order by created_at",screen,event);}
 public List<Map<String,Object>> snapshots(String screen){return db.queryForList("select id,screen_id as screenId,event_id as eventId,design_version as designVersion,status,content_hash as contentHash,approved_at as approvedAt from design_snapshot where screen_id=? order by created_at desc",screen);}
 public List<Map<String,Object>> snapshots(String screen,String event){registry.require(screen,event);return db.queryForList("select id,screen_id as screenId,event_id as eventId,design_version as designVersion,status,content_hash as contentHash,approved_at as approvedAt from design_snapshot where screen_id=? and event_id=? order by created_at desc",screen,event);}
 private void audit(String actor,String action,String type,String id,String outcome,Map<String,?> details){
  try{db.update("insert into audit_log(actor,action,target_type,target_id,outcome,details) values (?,?,?,?,?,?::jsonb)",actor,action,type,id,outcome,new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(details));}
  catch(Exception e){throw new IllegalStateException("Audit write failed",e);}
 }
}
