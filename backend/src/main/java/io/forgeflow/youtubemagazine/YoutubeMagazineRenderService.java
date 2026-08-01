package io.forgeflow.youtubemagazine;

import com.fasterxml.jackson.databind.*;
import java.nio.file.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.*;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

@Service
public class YoutubeMagazineRenderService {
 private final JdbcTemplate db;private final ObjectMapper mapper;private final RestClient orchestrator;private final Path outputRoot;
 YoutubeMagazineRenderService(JdbcTemplate db,ObjectMapper mapper,@Value("${youtube-magazine.orchestrator-url:http://localhost:8090}")String url,@Value("${youtube-magazine.output-root:./magazine-output}")String outputRoot){this.db=db;this.mapper=mapper;this.orchestrator=RestClient.builder().baseUrl(url).build();this.outputRoot=Path.of(outputRoot).toAbsolutePath().normalize();}

 @Transactional Map<String,Object> render(UUID jobId){
  Map<String,Object> job=db.queryForMap("select status,stage from youtube_magazine_job where id=? for update",jobId);
  if(!"DRAFT".equals(job.get("status"))||!"SCRIPT_READY".equals(job.get("stage")))throw new IllegalStateException("A SCRIPT_READY draft is required");
  String content=db.queryForObject("select content_json::text from youtube_magazine_artifact where job_id=? and kind='MAGAZINE_PLAN'",String.class,jobId);
  try{
   JsonNode plan=mapper.readTree(content);JsonNode result=orchestrator.post().uri("/v1/render-preview").contentType(MediaType.APPLICATION_JSON).body(Map.of("jobId",jobId,"plan",plan)).retrieve().body(JsonNode.class);
   if(result==null||result.path("publishable").asBoolean(true)||!"1080x1920".equals(result.path("resolution").asText()))throw new IllegalStateException("Invalid preview render response");
   db.update("insert into youtube_magazine_artifact(id,job_id,kind,content_json) values (?,?, 'RENDER_MANIFEST',?::jsonb) on conflict(job_id,kind) do update set content_json=excluded.content_json,created_at=now()",UUID.randomUUID(),jobId,result.toString());
   db.update("update youtube_magazine_job set stage='RENDERED_PREVIEW',progress=85,quality_score=?,output_path=?,updated_at=now() where id=?",result.path("quality").path("score").asInt(),result.path("videoPath").asText(),jobId);
   return Map.of("jobId",jobId,"stage","RENDERED_PREVIEW","manifest",result);
  }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException("Preview rendering failed",e);}
 }

 Resource preview(UUID jobId){
  Path file=outputRoot.resolve(jobId.toString()).resolve("preview.mp4").normalize();
  if(!file.startsWith(outputRoot)||!Files.isRegularFile(file))throw new IllegalArgumentException("Preview not found");
  return new FileSystemResource(file);
 }
}
