package io.forgeflow.governance;
import java.nio.file.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/governance")
public class GovernanceController {
 private final PatchValidator validator=new PatchValidator();
 private final JdbcTemplate db;private final Path backupRoot;
 GovernanceController(JdbcTemplate db,@Value("${forgeflow.run-root:./run-data}")String runRoot){this.db=db;this.backupRoot=Path.of(runRoot).toAbsolutePath().normalize().resolve("rollback");}
 record PatchRequest(String projectId,String unifiedDiff,Set<String> allowedPaths,boolean humanApproved,String expectedRevision,String actor){}
 record RollbackRequest(String projectId,String rollbackToken,List<String> changedFiles,String actor){}
 @PostMapping("/validate") Map<String,Object> validate(@RequestBody PatchRequest r){
  if(r.unifiedDiff()==null||r.allowedPaths()==null)throw new IllegalArgumentException("unifiedDiff and allowedPaths are required");
  var paths=r.allowedPaths().stream().map(Path::of).collect(java.util.stream.Collectors.toSet());
  var result=validator.validate(r.unifiedDiff(),paths);return Map.of("valid",result.valid(),"violations",result.violations());
 }
 @PostMapping("/dry-run") Map<String,Object> dryRun(@RequestBody PatchRequest r){
  var result=validate(r);return Map.of("mode","DRY_RUN","valid",result.get("valid"),"violations",result.get("violations"),"applied",false);
 }
 @Transactional @PostMapping("/apply") Map<String,Object> apply(@RequestBody PatchRequest r)throws Exception{
  if(!r.humanApproved())throw new IllegalStateException("Human approval required");
  var checked=validate(r);if(!Boolean.TRUE.equals(checked.get("valid")))throw new IllegalArgumentException("Patch validation failed");
  var service=service(r.projectId());var allowed=r.allowedPaths().stream().map(Path::of).collect(java.util.stream.Collectors.toSet());
  var result=service.apply(r.unifiedDiff(),allowed,r.expectedRevision());
  audit(r.actor(),"PATCH_APPLIED",r.projectId(),Map.of("revision",result.revision(),"rollbackToken",result.rollbackToken(),"changedFiles",result.changedFiles()));
  return Map.of("mode","APPLY","applied",true,"revision",result.revision(),"rollbackToken",result.rollbackToken(),"changedFiles",result.changedFiles());
 }
 @Transactional @PostMapping("/rollback") Map<String,Object> rollback(@RequestBody RollbackRequest r)throws Exception{
  if(r.rollbackToken()==null||r.changedFiles()==null)throw new IllegalArgumentException("rollbackToken and changedFiles are required");
  service(r.projectId()).rollback(r.rollbackToken(),r.changedFiles());audit(r.actor(),"PATCH_ROLLED_BACK",r.projectId(),Map.of("rollbackToken",r.rollbackToken(),"changedFiles",r.changedFiles()));
  return Map.of("rolledBack",true,"rollbackToken",r.rollbackToken(),"changedFiles",r.changedFiles());
 }
 private TargetPatchService service(String projectId)throws Exception{
  if(projectId==null)throw new IllegalArgumentException("projectId is required");
  var rows=db.queryForList("select target_path from project where id=?::uuid and status='ACTIVE'",projectId);
  if(rows.isEmpty())throw new IllegalArgumentException("Active project not found");
  return new TargetPatchService(Path.of(rows.getFirst().get("target_path").toString()),backupRoot.resolve(projectId));
 }
 private void audit(String actor,String action,String target,Map<String,?> details)throws Exception{
  String json=new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(details);
  db.update("insert into audit_log(actor,action,target_type,target_id,outcome,details) values (?,?,'project',?,'SUCCESS',?::jsonb)",actor==null?"human":actor,action,target,json);
 }
}
