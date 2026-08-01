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
public class YoutubeMagazineGenerationService {
 private final JdbcTemplate db;private final RestClient orchestrator;private final ObjectMapper mapper;
 YoutubeMagazineGenerationService(JdbcTemplate db,ObjectMapper mapper,@Value("${youtube-magazine.orchestrator-url:http://localhost:8090}")String url){this.db=db;this.mapper=mapper;this.orchestrator=RestClient.builder().baseUrl(url).build();}

 @Transactional Map<String,Object> generate(UUID jobId){
  Map<String,Object> job=db.queryForMap("select id,group_id,status,format from youtube_magazine_job where id=? for update",jobId);
  if(job.get("group_id")==null)throw new IllegalStateException("A TOP 6 group is required");
  if(!"DRAFT".equals(job.get("status")))throw new IllegalStateException("Only a DRAFT job can generate artifacts");
  UUID groupId=(UUID)job.get("group_id");
  Map<String,Object> group=db.queryForMap("select group_title as title,topic_keyword as \"topicKeyword\" from youtube_magazine_group where id=?",groupId);
  List<Map<String,Object>> items=db.queryForList("select i.rank_no as \"rankNo\",i.score,i.reason,v.video_id as \"videoId\",v.title,v.channel_title as \"channelTitle\",v.view_count as \"viewCount\",v.like_count as \"likeCount\",v.comment_count as \"commentCount\" from youtube_magazine_group_item i join youtube_magazine_video v on v.id=i.video_id where i.group_id=? order by i.rank_no",groupId);
  if(items.size()!=6)throw new IllegalStateException("The selected group must contain exactly six videos");
  JsonNode result=orchestrator.post().uri("/v1/generate-magazine").contentType(MediaType.APPLICATION_JSON)
    .body(Map.of("jobId",jobId,"format",job.get("format"),"group",group,"items",items)).retrieve().body(JsonNode.class);
  if(result==null||!result.path("entries").isArray()||result.path("entries").size()!=6)throw new IllegalStateException("Invalid generation response");
  db.update("insert into youtube_magazine_artifact(id,job_id,kind,content_json) values (?,?, 'MAGAZINE_PLAN',?::jsonb) on conflict(job_id,kind) do update set content_json=excluded.content_json,created_at=now()",UUID.randomUUID(),jobId,result.toString());
  int quality=result.path("quality").path("score").asInt();int risk=result.path("risk").path("score").asInt();
  db.update("update youtube_magazine_job set stage='SCRIPT_READY',progress=45,quality_score=?,risk_score=?,updated_at=now() where id=?",quality,risk,jobId);
  return Map.of("jobId",jobId,"stage","SCRIPT_READY","qualityScore",quality,"riskScore",risk,"artifact",result);
 }

 List<Map<String,Object>> artifacts(UUID jobId){
  return db.query("select id,kind,content_json::text as content,created_at from youtube_magazine_artifact where job_id=? order by created_at",(rs,row)->{
   try{return Map.of("id",rs.getObject("id",UUID.class),"kind",rs.getString("kind"),"content",mapper.readTree(rs.getString("content")),"createdAt",rs.getObject("created_at"));}
   catch(Exception e){throw new IllegalStateException("Artifact decoding failed",e);}
  },jobId);
 }
}
