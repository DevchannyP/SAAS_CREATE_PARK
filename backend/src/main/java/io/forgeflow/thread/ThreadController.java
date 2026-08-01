package io.forgeflow.thread;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/threads")
public class ThreadController {
 record Create(String name,String loopType){}
 record Message(String actor,String content){}
 private final JdbcTemplate db;
 ThreadController(JdbcTemplate db){this.db=db;}
 @GetMapping List<Map<String,Object>> list(){return db.queryForList("select id,name,loop_type as \"loopType\",version,created_at as \"createdAt\" from thread order by created_at");}
 @Transactional @PostMapping Map<String,Object> create(@RequestBody Create request){
  if(request.name()==null||request.name().isBlank())throw new IllegalArgumentException("Thread name required");String loop=normalize(request.loopType());String id=UUID.randomUUID().toString();db.update("insert into thread(id,name,loop_type) values (?::uuid,?,?)",id,request.name(),loop);return Map.of("id",id,"name",request.name(),"loopType",loop);
 }
 @GetMapping("/{threadId}/messages") List<Map<String,Object>> messages(@PathVariable String threadId){return db.queryForList("select id,thread_id as threadId,actor,content,created_at as createdAt from thread_message where thread_id=?::uuid order by created_at",threadId);}
 @Transactional @PostMapping("/{threadId}/messages") Map<String,Object> message(@PathVariable String threadId,@RequestBody Message request){if(request.content()==null||request.content().isBlank())throw new IllegalArgumentException("Message content required");if(request.actor()==null||request.actor().isBlank())throw new IllegalArgumentException("Actor required");String id=UUID.randomUUID().toString();int n=db.update("insert into thread_message(id,thread_id,actor,content) select ?::uuid,id,?,? from thread where id=?::uuid",id,request.actor(),request.content(),threadId);if(n!=1)throw new IllegalArgumentException("Thread not found");return Map.of("id",id,"threadId",threadId,"actor",request.actor());}
 @PutMapping("/{threadId}") Map<String,Object> update(@PathVariable String threadId,@RequestHeader("If-Match")String ifMatch,@RequestBody Create request){
  if(request.loopType()!=null)throw new IllegalStateException("loopType is immutable");if(request.name()==null||request.name().isBlank())throw new IllegalArgumentException("Thread name required");
  int expected=parseVersion(ifMatch),n=db.update("update thread set name=?,version=version+1 where id=?::uuid and version=?",request.name(),threadId,expected);
  if(n!=1)throw new IllegalStateException("Thread version conflict");return Map.of("id",threadId,"status","UPDATED","version",expected+1);
 }
 private static int parseVersion(String value){try{return Integer.parseInt(value.replace("\"","").trim());}catch(Exception e){throw new IllegalArgumentException("If-Match must be a numeric version");}}
 private static String normalize(String value){if(value==null)throw new IllegalArgumentException("loopType required");String result=value.toUpperCase(Locale.ROOT);if(!Set.of("DESIGN","IMPLEMENT").contains(result))throw new IllegalArgumentException("loopType must be DESIGN or IMPLEMENT");return result;}
}
