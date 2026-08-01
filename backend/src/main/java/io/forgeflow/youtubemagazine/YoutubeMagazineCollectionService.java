package io.forgeflow.youtubemagazine;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

@Service
public class YoutubeMagazineCollectionService {
 private final JdbcTemplate db;private final RestClient orchestrator;
 YoutubeMagazineCollectionService(JdbcTemplate db,@Value("${youtube-magazine.orchestrator-url:http://localhost:8090}")String url){this.db=db;this.orchestrator=RestClient.builder().baseUrl(url).build();}

 @Transactional Map<String,Object> collect(String regionCode,String categoryId,int maxResults){
  String region=regionCode==null?"KR":regionCode.trim().toUpperCase(Locale.ROOT);String category=categoryId==null?"24":categoryId.trim();
  if(!region.matches("[A-Z]{2}"))throw new IllegalArgumentException("regionCode must be an ISO alpha-2 code");
  if(!category.matches("[0-9]{1,4}"))throw new IllegalArgumentException("categoryId must be numeric");
  int maximum=Math.min(Math.max(maxResults,6),50);
  JsonNode result=orchestrator.post().uri("/v1/collect-rank-group").contentType(MediaType.APPLICATION_JSON)
    .body(Map.of("regionCode",region,"categoryId",category,"maxResults",maximum)).retrieve().body(JsonNode.class);
  if(result==null||!result.path("videos").isArray()||!result.has("group")||!result.path("group").path("items").isArray())throw new IllegalStateException("Invalid orchestrator response");
  JsonNode group=result.path("group");UUID groupId=UUID.randomUUID();
  db.update("insert into youtube_magazine_group(id,group_title,category_id,topic_keyword) values (?,?,?,?)",groupId,text(group,"title"),category,text(group,"topicKeyword"));
  int saved=0;
  for(JsonNode item:result.path("videos")){
   UUID videoId=UUID.randomUUID();String externalId=text(item,"videoId");
   db.update("insert into youtube_magazine_video(id,video_id,title,channel_title,category_id,published_at,view_count,like_count,comment_count,hot_score,thumbnail_url,collected_at) values (?,?,?,?,?,?,?,?,?,?,?,now()) on conflict(video_id) do update set title=excluded.title,channel_title=excluded.channel_title,category_id=excluded.category_id,published_at=excluded.published_at,view_count=excluded.view_count,like_count=excluded.like_count,comment_count=excluded.comment_count,hot_score=excluded.hot_score,thumbnail_url=excluded.thumbnail_url,collected_at=now()",
    videoId,externalId,text(item,"title"),text(item,"channelTitle"),category,OffsetDateTime.parse(text(item,"publishedAt")),number(item,"viewCount").longValue(),number(item,"likeCount").longValue(),number(item,"commentCount").longValue(),number(item,"hotScore"),text(item,"thumbnailUrl"));
   saved++;
  }
  for(JsonNode item:group.path("items")){
   String externalId=text(item,"videoId");
   UUID stored=db.queryForObject("select id from youtube_magazine_video where video_id=?",UUID.class,externalId);
   db.update("insert into youtube_magazine_group_item(id,group_id,video_id,rank_no,score,reason) values (?,?,?,?,?,?)",UUID.randomUUID(),groupId,stored,number(item,"rankNo").intValue(),number(item,"hotScore"),text(item,"reason"));
  }
  return Map.of("mode",text(result,"mode"),"collectedCount",result.path("collectedCount").asInt(),"savedCount",saved,"groupId",groupId,"groupTitle",text(group,"title"));
 }
 private static String text(JsonNode node,String field){return node.path(field).asText("");}
 private static BigDecimal number(JsonNode node,String field){return node.path(field).decimalValue();}
}
