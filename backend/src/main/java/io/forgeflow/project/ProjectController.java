package io.forgeflow.project;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/projects")
public class ProjectController {
 record Register(String name,String targetPath,String actor){}
 private final JdbcTemplate db;private final Path controlRoot;private final Path runRoot;
 ProjectController(JdbcTemplate db,@Value("${forgeflow.control-root:./}")String controlRoot,@Value("${forgeflow.run-root:./run-data}")String runRoot){this.db=db;this.controlRoot=Path.of(controlRoot).toAbsolutePath().normalize();this.runRoot=Path.of(runRoot).toAbsolutePath().normalize();}
 @GetMapping List<Map<String,Object>> list(){return db.queryForList("select id,name,target_path as targetPath,status,version,content_hash as contentHash,created_at as createdAt from project order by created_at desc");}
 @Transactional @PostMapping Map<String,Object> register(@RequestBody Register request){
  if(request.name()==null||request.name().isBlank()||request.targetPath()==null)throw new IllegalArgumentException("Project name and targetPath are required");
  try{
   Path target=Path.of(request.targetPath()).toRealPath();if(!target.startsWith(controlRoot)&&!controlRoot.startsWith(target)&&!target.startsWith(runRoot)&&!runRoot.startsWith(target)){}else throw new SecurityException("Target overlaps protected root");
   if(!Files.isDirectory(target.resolve(".git")))throw new IllegalArgumentException("Target must be a Git worktree");
   String hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(target.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8)));String id=UUID.randomUUID().toString();
   db.update("insert into project(id,name,target_path,status,content_hash,created_by) values (?::uuid,?,?, 'ACTIVE',?,?)",id,request.name(),target.toString(),hash,request.actor()==null?"system":request.actor());return Map.of("id",id,"name",request.name(),"targetPath",target.toString(),"status","ACTIVE","contentHash",hash);
  }catch(SecurityException|IllegalArgumentException e){throw e;}catch(Exception e){throw new IllegalStateException("Project registration failed",e);}
 }
}
