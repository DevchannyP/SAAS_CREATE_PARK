package io.forgeflow.audit;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/audit-logs")
public class AuditController {
 private final JdbcTemplate db;
 AuditController(JdbcTemplate db){this.db=db;}
 @GetMapping List<Map<String,Object>> list(@RequestParam(defaultValue="50")int limit){
  if(limit<1||limit>200)throw new IllegalArgumentException("limit must be between 1 and 200");
  return db.queryForList("select id,actor,action,target_type as \"targetType\",target_id as \"targetId\",outcome,details,created_at as \"createdAt\" from audit_log order by created_at desc limit ?",limit);
 }
}
