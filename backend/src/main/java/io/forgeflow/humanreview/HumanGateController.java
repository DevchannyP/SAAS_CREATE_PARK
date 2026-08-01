package io.forgeflow.humanreview;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/human-gates")
public class HumanGateController {
 record Decision(String decision,String actor,String comment){}
 private final JdbcTemplate db;
 HumanGateController(JdbcTemplate db){this.db=db;}
 @GetMapping List<Map<String,Object>> list(@RequestParam String runId){return db.queryForList("select id,run_id as \"runId\",gate_type as \"gateType\",decision,decided_by as \"decidedBy\",decided_at as \"decidedAt\",version from human_gate where run_id=?::uuid order by id",runId);}
 @GetMapping("/{gateId}") Map<String,Object> get(@PathVariable String gateId){var rows=db.queryForList("select id,run_id as runId,gate_type as gateType,decision,decided_by as decidedBy,decided_at as decidedAt,version from human_gate where id=?::uuid",gateId);if(rows.isEmpty())throw new IllegalArgumentException("Human gate not found");return rows.getFirst();}
 @Transactional @PostMapping("/{gateId}/decide") Map<String,Object> decide(@PathVariable String gateId,@RequestBody Decision decision){
  if(!Set.of("APPROVE","REJECT").contains(decision.decision()))throw new IllegalArgumentException("Decision must be APPROVE or REJECT");
  int updated=db.update("update human_gate set decision=?,decided_by=?,decided_at=now(),version=version+1 where id=?::uuid and decision is null",decision.decision(),decision.actor(),gateId);if(updated!=1)throw new IllegalStateException("Gate already decided or missing");
  String state="APPROVE".equals(decision.decision())?"ACCEPTED":"B_REPAIR";
  db.update("update workloop_run set state=?,updated_at=now() where id=(select run_id from human_gate where id=?::uuid)",state,gateId);
  db.update("insert into audit_log(actor,action,target_type,target_id,outcome,details) values (?,'HUMAN_GATE_DECIDED','human_gate',?,'SUCCESS',jsonb_build_object('decision',?))",decision.actor()==null?"human":decision.actor(),gateId,decision.decision());
  return Map.of("gateId",gateId,"decision",decision.decision(),"state",state);
 }
}
