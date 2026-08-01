package io.forgeflow.youtubemagazine;

import com.fasterxml.jackson.databind.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

@Service
public class YoutubeMagazineUploadService {
 private final JdbcTemplate db;private final ObjectMapper mapper;private final RestClient orchestrator;
 YoutubeMagazineUploadService(JdbcTemplate db,ObjectMapper mapper,@Value("${youtube-magazine.orchestrator-url:http://localhost:8090}")String url){this.db=db;this.mapper=mapper;this.orchestrator=RestClient.builder().baseUrl(url).build();}

 @Transactional Map<String,Object> prepare(UUID jobId){
  Map<String,Object> job=db.queryForMap("select status,stage,risk_score from youtube_magazine_job where id=? for update",jobId);
  if(!"APPROVED".equals(job.get("status"))||!"HUMAN_APPROVED".equals(job.get("stage")))throw new IllegalStateException("Human approval is required");
  try{
   JsonNode plan=artifact(jobId,"MAGAZINE_PLAN"),quality=artifact(jobId,"QUALITY_REPORT");
   int risk=job.get("risk_score") instanceof Number value?value.intValue():100;
   JsonNode result=orchestrator.post().uri("/v1/prepare-upload").contentType(MediaType.APPLICATION_JSON).body(Map.of("jobId",jobId,"plan",plan,"quality",quality,"riskScore",risk)).retrieve().body(JsonNode.class);
   if(result==null||!"private".equals(result.path("metadata").path("status").path("privacyStatus").asText()))throw new IllegalStateException("Upload package must default to private");
   db.update("insert into youtube_magazine_artifact(id,job_id,kind,content_json) values (?,?, 'UPLOAD_PACKAGE',?::jsonb) on conflict(job_id,kind) do update set content_json=excluded.content_json,created_at=now()",UUID.randomUUID(),jobId,result.toString());
   db.update("update youtube_magazine_job set status='UPLOAD_PACKAGE_READY',stage='UPLOAD_PACKAGE',progress=100,updated_at=now() where id=?",jobId);
   return Map.of("jobId",jobId,"status","UPLOAD_PACKAGE_READY","package",result);
  }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException("Upload package preparation failed",e);}
 }
 private JsonNode artifact(UUID jobId,String kind)throws Exception{
  String content=db.queryForObject("select content_json::text from youtube_magazine_artifact where job_id=? and kind=?",String.class,jobId,kind);return mapper.readTree(content);
 }
}
