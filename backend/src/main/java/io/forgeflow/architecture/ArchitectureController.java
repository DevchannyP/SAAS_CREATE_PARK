package io.forgeflow.architecture;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.MessageDigest;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/architecture-profiles")
public class ArchitectureController {
 record Profile(String name,List<String> layers,String actor){}
 private final JdbcTemplate db;private final ObjectMapper mapper;
 ArchitectureController(JdbcTemplate db,ObjectMapper mapper){this.db=db;this.mapper=mapper;}
 @GetMapping List<Map<String,Object>> list(){return db.queryForList("select id,name,layers,version,content_hash as \"contentHash\",status,created_at as \"createdAt\" from architecture_profile where status='ACTIVE' order by name");}
 @Transactional @PostMapping Map<String,Object> create(@RequestBody Profile request){
  if(request.name()==null||request.name().isBlank()||request.layers()==null||request.layers().isEmpty())throw new IllegalArgumentException("Profile name and ordered layers are required");
  try{String json=mapper.writeValueAsString(request.layers());String hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(json.getBytes(java.nio.charset.StandardCharsets.UTF_8)));String id=UUID.randomUUID().toString();db.update("insert into architecture_profile(id,name,layers,content_hash,created_by) values (?::uuid,? ,?::jsonb,?,?)",id,request.name(),json,hash,request.actor()==null?"system":request.actor());return Map.of("id",id,"name",request.name(),"layers",request.layers(),"contentHash",hash);}catch(Exception e){throw new IllegalStateException("Architecture profile save failed",e);}
 }
 @DeleteMapping("/{id}") Map<String,String> remove(@PathVariable String id){int n=db.update("update architecture_profile set status='DELETED' where id=?::uuid and status='ACTIVE'",id);if(n!=1)throw new IllegalArgumentException("Profile not found");return Map.of("id",id,"status","DELETED");}
 @Transactional @PostMapping("/{id}/apply") Map<String,Object> apply(@PathVariable String id,@RequestParam String projectId,@RequestHeader("If-Match")String ifMatch){
  int expected;try{expected=Integer.parseInt(ifMatch.replace("\"","").trim());}catch(Exception e){throw new IllegalArgumentException("If-Match must be a numeric project version");}
  Integer active=db.queryForObject("select count(*) from architecture_profile where id=?::uuid and status='ACTIVE'",Integer.class,id);if(active==null||active!=1)throw new IllegalArgumentException("Architecture profile not found");
  int n=db.update("update project set architecture_profile_id=?::uuid,version=version+1 where id=?::uuid and version=? and status='ACTIVE'",id,projectId,expected);
  if(n!=1)throw new IllegalStateException("Project version conflict");
  db.update("insert into audit_log(actor,action,target_type,target_id,outcome,details) values ('system','ARCHITECTURE_APPLIED','project',?,'SUCCESS',jsonb_build_object('profileId',?,'version',?))",projectId,id,expected+1);
  return Map.of("projectId",projectId,"architectureProfileId",id,"version",expected+1,"status","APPLIED");
 }
}
