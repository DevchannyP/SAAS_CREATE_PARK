package io.forgeflow.harness;
import java.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/harnesses")
public class HarnessController {
 record DraftRequest(String content,String actor){}
 private final HarnessRegistry registry;
 private final JdbcTemplate db; private final ObjectMapper mapper;
 HarnessController(HarnessRegistry registry,JdbcTemplate db,ObjectMapper mapper){this.registry=registry;this.db=db;this.mapper=mapper;}
 @GetMapping("/{loopType}") List<HarnessRegistry.Agent> list(@PathVariable String loopType){return registry.list(normalize(loopType));}
 @GetMapping("/{loopType}/{agentId}") HarnessRegistry.Agent get(@PathVariable String loopType,@PathVariable String agentId){return registry.require(normalize(loopType),agentId);}
 @GetMapping("/{loopType}/versions") List<Map<String,Object>> versions(@PathVariable String loopType){
  String normalized=normalize(loopType);registry.list(normalized);
  return db.queryForList("select version,content_hash as contentHash,status,created_at as createdAt from harness_version where loop_type=? order by version desc",normalized);
 }
 @GetMapping("/{loopType}/drafts") List<Map<String,Object>> drafts(@PathVariable String loopType){
  String normalized=normalize(loopType);registry.list(normalized);
  return db.queryForList("select agent_id as \"agentId\",content,version,updated_by as \"updatedBy\",updated_at as \"updatedAt\" from harness_draft where loop_type=? order by agent_id",normalized);
 }
 @Transactional @PutMapping("/{loopType}/drafts/{agentId}") Map<String,Object> saveDraft(@PathVariable String loopType,@PathVariable String agentId,@RequestHeader(value="If-Match",required=false)String ifMatch,@RequestBody DraftRequest request){
  String normalized=normalize(loopType);registry.require(normalized,agentId);if(request.content()==null||request.content().isBlank())throw new IllegalArgumentException("Draft content required");
  Integer current=db.queryForObject("select coalesce(max(version),0) from harness_draft where loop_type=? and agent_id=?",Integer.class,normalized,agentId);int expected=parseVersion(ifMatch);
  if(current!=null&&current!=expected)throw new IllegalStateException("Draft version conflict");
  if(current==null||current==0)db.update("insert into harness_draft(loop_type,agent_id,content,updated_by) values (?,?,?,?)",normalized,agentId,request.content(),actor(request.actor()));
  else db.update("update harness_draft set content=?,version=version+1,updated_by=?,updated_at=now() where loop_type=? and agent_id=? and version=?",request.content(),actor(request.actor()),normalized,agentId,expected);
  return Map.of("loopType",normalized,"agentId",agentId,"version",expected+1,"status","DRAFT_SAVED");
 }
 @GetMapping("/{loopType}/diff") Map<String,Object> diff(@PathVariable String loopType)throws Exception{
  String normalized=normalize(loopType);registry.list(normalized);
  var draftRows=db.queryForList("select agent_id,content from harness_draft where loop_type=?",normalized);Map<String,String> draft=new TreeMap<>();for(var row:draftRows)draft.put(row.get("agent_id").toString(),row.get("content").toString());
  var publishedRows=db.queryForList("select files::text as files from harness_version where loop_type=? and status='PUBLISHED' order by version desc limit 1",normalized);Map<String,Object> published=publishedRows.isEmpty()?Map.of():mapper.readValue(publishedRows.getFirst().get("files").toString(),Map.class);
  var changed=new ArrayList<String>();for(String id:draft.keySet())if(!Objects.equals(draft.get(id),published.get(id)))changed.add(id);
  return Map.of("loopType",normalized,"changedAgents",changed,"changedCount",changed.size());
 }
 @PostMapping("/{loopType}/{agentId}/validate") Map<String,Object> validate(@PathVariable String loopType,@PathVariable String agentId,@RequestBody Map<String,String> body){
  registry.require(normalize(loopType),agentId);String content=body.getOrDefault("content","");
  var errors=new ArrayList<String>();if(content.isBlank())errors.add("content_required");if(content.contains("saas.repository.write"))errors.add("forbidden_capability");
  return Map.of("valid",errors.isEmpty(),"errors",errors,"contentHash",Integer.toHexString(content.hashCode()));
 }
 @Transactional @PostMapping("/{loopType}/publish") Map<String,Object> publish(@PathVariable String loopType,@RequestBody Map<String,Object> body){
  String normalized=normalize(loopType);registry.list(normalized);Object value=body.get("files");
  if(!(value instanceof Map<?,?> files)||files.isEmpty())throw new IllegalArgumentException("Harness files required");
  var errors=new ArrayList<String>();for(var entry:files.entrySet()){if(!(entry.getKey() instanceof String id)||!(entry.getValue() instanceof String content)){errors.add("invalid_file");continue;}try{registry.require(normalized,id);if(content.isBlank())errors.add(id+":content_required");if(content.contains("saas.repository.write"))errors.add(id+":forbidden_capability");}catch(IllegalArgumentException e){errors.add(id+":unknown_agent");}}
  if(!errors.isEmpty())throw new IllegalArgumentException(String.join(",",errors));
  try {
   String json=mapper.writeValueAsString(files);String hash=java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(json.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
   Integer latest=db.queryForObject("select coalesce(max(version),0) from harness_version where loop_type=?",Integer.class,normalized);int version=(latest==null?0:latest)+1;
   db.update("insert into harness_version(loop_type,version,files,content_hash,status,created_by) values (?,?,?::jsonb,?,'PUBLISHED','system')",normalized,version,json,hash);
   return Map.of("status","PUBLISHED","loopType",normalized,"version",version,"fileCount",files.size(),"contentHash",hash);
  } catch(Exception e){throw new IllegalStateException("Harness publish failed",e);}
 }
 private static int parseVersion(String value){if(value==null||value.isBlank())return 0;try{return Integer.parseInt(value.replace("\"","").trim());}catch(Exception e){throw new IllegalArgumentException("If-Match must be a numeric version");}}
 private static String actor(String value){return value==null||value.isBlank()?"system":value;}
 private static String normalize(String value){String normalized=value.toUpperCase(Locale.ROOT);return "CODE".equals(normalized)?"IMPLEMENT":normalized;}
}
