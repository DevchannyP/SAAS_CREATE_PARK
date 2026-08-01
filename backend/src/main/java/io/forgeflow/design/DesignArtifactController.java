package io.forgeflow.design;
import io.forgeflow.registry.EventRegistry;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/design-artifacts")
public class DesignArtifactController {
 record ArtifactRequest(String screenId,String eventId,String artifactType,String content,String actor){}
 private final JdbcTemplate db;private final EventRegistry registry;
 DesignArtifactController(JdbcTemplate db,EventRegistry registry){this.db=db;this.registry=registry;}
 @GetMapping List<Map<String,Object>> list(@RequestParam String screenId,@RequestParam(required=false)String eventId){
  if(eventId!=null)registry.require(screenId,eventId);
  else registry.screens().stream().filter(x->x.id().equals(screenId)).findFirst().orElseThrow(()->new IllegalArgumentException("Unknown screen"));
  String base="select id,screen_id as \"screenId\",event_id as \"eventId\",artifact_type as \"artifactType\",artifact_version as \"artifactVersion\",content_hash as \"contentHash\",status,evaluation_result as \"evaluationResult\",created_at as \"createdAt\",created_by as \"createdBy\" from design_artifact where screen_id=?";
  return eventId==null?db.queryForList(base+" order by created_at desc",screenId):db.queryForList(base+" and event_id=? order by created_at desc",screenId,eventId);
 }
 @Transactional @PostMapping Map<String,Object> create(@RequestBody ArtifactRequest request)throws Exception{
  validate(request);Integer latest=db.queryForObject("select coalesce(max(artifact_version),0) from design_artifact where screen_id=? and event_id=? and artifact_type=?",Integer.class,request.screenId(),request.eventId(),request.artifactType());int version=(latest==null?0:latest)+1;
  String id=UUID.randomUUID().toString(),hash=hash(request.content());
  db.update("insert into design_artifact(id,screen_id,event_id,artifact_type,artifact_version,content,content_hash,created_by) values (?::uuid,?,?,?,?,?,?,?)",id,request.screenId(),request.eventId(),request.artifactType(),version,request.content(),hash,actor(request.actor()));
  return Map.of("id",id,"artifactVersion",version,"contentHash",hash,"status","DRAFT");
 }
 @Transactional @PutMapping("/{id}") Map<String,Object> update(@PathVariable String id,@RequestHeader("If-Match")String ifMatch,@RequestBody ArtifactRequest request)throws Exception{
  int expected=parse(ifMatch);if(request.content()==null||request.content().isBlank())throw new IllegalArgumentException("Artifact content required");String hash=hash(request.content());
  int n=db.update("update design_artifact set content=?,content_hash=?,artifact_version=artifact_version+1,status='DRAFT',evaluation_result='{}' where id=?::uuid and artifact_version=?",request.content(),hash,id,expected);
  if(n!=1)throw new IllegalStateException("Artifact version conflict");return Map.of("id",id,"artifactVersion",expected+1,"contentHash",hash,"status","DRAFT");
 }
 @Transactional @PostMapping("/{id}/evaluate") Map<String,Object> evaluate(@PathVariable String id){
  var rows=db.queryForList("select content,content_hash from design_artifact where id=?::uuid",id);if(rows.isEmpty())throw new IllegalArgumentException("Artifact not found");
  String content=rows.getFirst().get("content").toString();var failures=new ArrayList<String>();if(content.length()<20)failures.add("CONTENT_TOO_SHORT");boolean passed=failures.isEmpty();
  db.update("update design_artifact set status=?,evaluation_result=jsonb_build_object('passed',?,'failures',?::text[]) where id=?::uuid",passed?"REVIEWED":"REPAIR",passed,failures.toArray(String[]::new),id);
  return Map.of("id",id,"passed",passed,"failures",failures,"status",passed?"REVIEWED":"REPAIR");
 }
 private void validate(ArtifactRequest r){registry.require(r.screenId(),r.eventId());if(r.artifactType()==null||r.artifactType().isBlank()||r.content()==null||r.content().isBlank())throw new IllegalArgumentException("artifactType and content are required");}
 private static String actor(String actor){return actor==null||actor.isBlank()?"system":actor;}
 private static int parse(String value){try{return Integer.parseInt(value.replace("\"","").trim());}catch(Exception e){throw new IllegalArgumentException("If-Match must be a numeric version");}}
 private static String hash(String value)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}
}
