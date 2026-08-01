package io.forgeflow.evidence;
import java.util.*;
import java.nio.file.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1")
public class EvidenceController {
 private final JdbcTemplate db;
 private final Path runRoot;
 EvidenceController(JdbcTemplate db,@Value("${forgeflow.run-root:./run-data}")String runRoot){this.db=db;this.runRoot=Path.of(runRoot).toAbsolutePath().normalize();}
 @GetMapping("/patches/{patchId}") Map<String,Object> patch(@PathVariable String patchId){
  var rows=db.queryForList("select id,run_id as runId,design_snapshot_id as designSnapshotId,design_version as designVersion,screen_id as screenId,event_id as eventId,changed_files as changedFiles,changed_symbols as changedSymbols,unified_diff as unifiedDiff,findings,evidence_refs as evidenceRefs,status,content_hash as contentHash from patch_bundle where id=?::uuid",patchId);
  if(rows.isEmpty())throw new IllegalArgumentException("Patch not found");return rows.getFirst();
 }
 @GetMapping("/evidence") List<Map<String,Object>> evidence(@RequestParam String runId){return db.queryForList("select id,run_id as \"runId\",kind,artifact_path as \"artifactPath\",content_hash as \"contentHash\",summary,created_at as \"createdAt\" from evidence where run_id=?::uuid order by created_at",runId);}
 @GetMapping("/evidence/{evidenceId}") Map<String,Object> evidenceById(@PathVariable String evidenceId){
  var rows=db.queryForList("select id,run_id as \"runId\",kind,artifact_path as \"artifactPath\",content_hash as \"contentHash\",summary,created_at as \"createdAt\" from evidence where id=?::uuid",evidenceId);
  if(rows.isEmpty())throw new IllegalArgumentException("Evidence not found");return rows.getFirst();
 }
 @GetMapping("/evidence/{evidenceId}/artifact") ResponseEntity<byte[]> artifact(@PathVariable String evidenceId)throws Exception{
  var item=evidenceById(evidenceId);Object raw=item.get("artifactPath");if(raw==null)throw new IllegalArgumentException("Evidence has no artifact");
  Path file=Path.of(raw.toString()).toAbsolutePath().normalize();if(!file.startsWith(runRoot)||!Files.isRegularFile(file)||Files.isSymbolicLink(file))throw new SecurityException("Artifact path rejected");
  return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM).body(Files.readAllBytes(file));
 }
}
