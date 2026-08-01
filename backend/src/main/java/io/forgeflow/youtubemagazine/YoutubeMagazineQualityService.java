package io.forgeflow.youtubemagazine;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

@Service
public class YoutubeMagazineQualityService {
 private final JdbcTemplate db;private final RestClient orchestrator;
 YoutubeMagazineQualityService(JdbcTemplate db,@Value("${youtube-magazine.orchestrator-url:http://localhost:8090}")String url){this.db=db;this.orchestrator=RestClient.builder().baseUrl(url).build();}

 @Transactional Map<String,Object> check(UUID jobId){
  Map<String,Object> job=db.queryForMap("select status,stage,risk_score from youtube_magazine_job where id=? for update",jobId);
  if(!"DRAFT".equals(job.get("status"))||!"RENDERED_PREVIEW".equals(job.get("stage")))throw new IllegalStateException("A RENDERED_PREVIEW draft is required");
  JsonNode report=orchestrator.post().uri("/v1/quality-check").contentType(MediaType.APPLICATION_JSON).body(Map.of("jobId",jobId)).retrieve().body(JsonNode.class);
  if(report==null||!report.path("passed").asBoolean(false))throw new IllegalStateException("Technical quality checks did not pass");
  int score=report.path("score").asInt();
  db.update("insert into youtube_magazine_artifact(id,job_id,kind,content_json) values (?,?, 'QUALITY_REPORT',?::jsonb) on conflict(job_id,kind) do update set content_json=excluded.content_json,created_at=now()",UUID.randomUUID(),jobId,report.toString());
  db.update("update youtube_magazine_job set stage='QUALITY_PASSED',progress=90,quality_score=?,updated_at=now() where id=?",score,jobId);
  return Map.of("jobId",jobId,"stage","QUALITY_PASSED","qualityScore",score,"report",report);
 }
}
