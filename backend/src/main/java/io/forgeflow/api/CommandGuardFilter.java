package io.forgeflow.api;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.*;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

@Component
public class CommandGuardFilter extends OncePerRequestFilter {
 private final JdbcTemplate db;private final ObjectMapper mapper;
 CommandGuardFilter(JdbcTemplate db,ObjectMapper mapper){this.db=db;this.mapper=mapper;}
 @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)throws ServletException,IOException{
  String requestId=header(request,"X-Request-ID",UUID.randomUUID().toString());
  if(!requestId.matches("[A-Za-z0-9._:-]{8,128}")){problem(response,400,"Invalid X-Request-ID");return;}
  response.setHeader("X-Request-ID",requestId);
  boolean command=Set.of("POST","PUT","PATCH","DELETE").contains(request.getMethod())&&request.getRequestURI().startsWith("/api/v1/");
  String key=null;
  if(command){
   key=request.getHeader("X-Idempotency-Key");String actor=request.getHeader("X-Actor");
   if(key==null||!key.matches("[A-Za-z0-9._:-]{8,128}")){problem(response,400,"Valid X-Idempotency-Key required");return;}
   if(actor==null||actor.isBlank()||actor.length()>128){problem(response,400,"X-Actor required");return;}
   try{db.update("insert into command_idempotency(idempotency_key,request_method,request_path,request_id,actor) values (?,?,?,?,?)",key,request.getMethod(),request.getRequestURI(),requestId,actor);}
   catch(DuplicateKeyException e){
    var rows=db.queryForList("select request_method,request_path,completed,response_status,response_content_type,response_body from command_idempotency where idempotency_key=?",key);
    if(rows.isEmpty()){problem(response,409,"Command state unavailable");return;}
    var saved=rows.getFirst();
    if(!request.getMethod().equals(saved.get("request_method"))||!request.getRequestURI().equals(saved.get("request_path"))){problem(response,409,"Idempotency key belongs to another command");return;}
    if(!Boolean.TRUE.equals(saved.get("completed"))){problem(response,409,"Command is already in progress");return;}
    response.setHeader("X-Idempotent-Replay","true");response.setStatus(((Number)saved.get("response_status")).intValue());
    response.setContentType(String.valueOf(saved.get("response_content_type")));response.getWriter().write(String.valueOf(saved.get("response_body")));return;
   }
  }
  if(!command){chain.doFilter(request,response);return;}
  var wrapped=new ContentCachingResponseWrapper(response);
  try{chain.doFilter(request,wrapped);}
  finally{
   if(wrapped.getStatus()>=400)db.update("delete from command_idempotency where idempotency_key=?",key);
   else db.update("update command_idempotency set completed=true,response_status=?,response_content_type=?,response_body=? where idempotency_key=?",wrapped.getStatus(),Optional.ofNullable(wrapped.getContentType()).orElse(MediaType.APPLICATION_JSON_VALUE),new String(wrapped.getContentAsByteArray(),java.nio.charset.StandardCharsets.UTF_8),key);
   wrapped.copyBodyToResponse();
  }
 }
 private String header(HttpServletRequest request,String name,String fallback){String value=request.getHeader(name);return value==null||value.isBlank()?fallback:value;}
 private void problem(HttpServletResponse response,int status,String detail)throws IOException{
  response.setStatus(status);response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
  mapper.writeValue(response.getOutputStream(),Map.of("type","urn:forgeflow:request-rejected","title","Request rejected","status",status,"detail",detail));
 }
}
