package io.forgeflow.youtubemagazine;

import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/youtube-magazine")
public class YoutubeMagazineController {
 private final JdbcTemplate db;private final YoutubeMagazineCollectionService collection;private final YoutubeMagazineGenerationService generation;private final YoutubeMagazineRenderService rendering;
 YoutubeMagazineController(JdbcTemplate db,YoutubeMagazineCollectionService collection,YoutubeMagazineGenerationService generation,YoutubeMagazineRenderService rendering){this.db=db;this.collection=collection;this.generation=generation;this.rendering=rendering;}

 record CreateJob(String groupId,String format){}
 record CollectRequest(String regionCode,String categoryId,Integer maxResults){}

 @PostMapping("/collect") Map<String,Object> collect(@RequestBody(required=false) CollectRequest request){
  return collection.collect(request==null?"KR":request.regionCode(),request==null?"24":request.categoryId(),request==null||request.maxResults()==null?18:request.maxResults());
 }

 @GetMapping("/jobs") List<Map<String,Object>> jobs(){
  return db.queryForList("select id,group_id as \"groupId\",status,stage,progress,format,privacy_status as \"privacyStatus\",quality_score as \"qualityScore\",risk_score as \"riskScore\",output_path as \"outputPath\",created_at as \"createdAt\",updated_at as \"updatedAt\" from youtube_magazine_job order by created_at desc");
 }
 @GetMapping("/jobs/{id}") Map<String,Object> job(@PathVariable UUID id){
  return db.queryForMap("select id,group_id as \"groupId\",status,stage,progress,format,privacy_status as \"privacyStatus\",quality_score as \"qualityScore\",risk_score as \"riskScore\",output_path as \"outputPath\",created_at as \"createdAt\",updated_at as \"updatedAt\" from youtube_magazine_job where id=?",id);
 }
 @PostMapping("/jobs/{id}/generate") Map<String,Object> generate(@PathVariable UUID id){return generation.generate(id);}
 @GetMapping("/jobs/{id}/artifacts") List<Map<String,Object>> artifacts(@PathVariable UUID id){return generation.artifacts(id);}
 @PostMapping("/jobs/{id}/render-preview") Map<String,Object> render(@PathVariable UUID id){return rendering.render(id);}
 @GetMapping(value="/jobs/{id}/preview",produces="video/mp4") ResponseEntity<Resource> preview(@PathVariable UUID id){return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"inline; filename=preview.mp4").contentType(MediaType.valueOf("video/mp4")).body(rendering.preview(id));}
 @Transactional @PostMapping("/jobs") Map<String,Object> create(@RequestBody(required=false) CreateJob request){
  UUID id=UUID.randomUUID();UUID groupId=request==null||request.groupId()==null||request.groupId().isBlank()?null:UUID.fromString(request.groupId());
  String format=request==null||request.format()==null?"SHORTS":request.format().toUpperCase(Locale.ROOT);
  if(!Set.of("SHORTS","LONGFORM").contains(format))throw new IllegalArgumentException("format must be SHORTS or LONGFORM");
  db.update("insert into youtube_magazine_job(id,group_id,status,stage,progress,format) values (?,?, 'DRAFT','READY',0,?)",id,groupId,format);
  return job(id);
 }
 @PostMapping("/jobs/{id}/approve") Map<String,Object> approve(@PathVariable UUID id){
  int changed=db.update("update youtube_magazine_job set status='APPROVED',stage='HUMAN_APPROVED',progress=95,updated_at=now() where id=? and status='DRAFT' and stage='RENDERED_PREVIEW' and quality_score>=90 and risk_score<=30",id);
  if(changed==0)throw new IllegalStateException("A rendered preview with quality >= 90 and risk <= 30 is required");return job(id);
 }
 @PostMapping("/jobs/{id}/upload") Map<String,Object> upload(@PathVariable UUID id){
  int changed=db.update("update youtube_magazine_job set status='UPLOAD_READY',stage='YOUTUBE_UPLOAD',progress=100,updated_at=now() where id=? and status='APPROVED'",id);
  if(changed==0)throw new IllegalStateException("Approval is required before upload preparation");return job(id);
 }
 @GetMapping("/videos") List<Map<String,Object>> videos(){
  return db.queryForList("select id,video_id as \"videoId\",title,channel_title as \"channelTitle\",category_id as \"categoryId\",published_at as \"publishedAt\",view_count as \"viewCount\",like_count as \"likeCount\",comment_count as \"commentCount\",hot_score as \"hotScore\",thumbnail_url as \"thumbnailUrl\",collected_at as \"collectedAt\" from youtube_magazine_video order by hot_score desc");
 }
 @GetMapping("/groups") List<Map<String,Object>> groups(){
  return db.queryForList("select g.id,g.group_title as \"groupTitle\",g.category_id as \"categoryId\",g.topic_keyword as \"topicKeyword\",g.created_at as \"createdAt\",count(i.id) as \"itemCount\" from youtube_magazine_group g left join youtube_magazine_group_item i on i.group_id=g.id group by g.id order by g.created_at desc");
 }
}
