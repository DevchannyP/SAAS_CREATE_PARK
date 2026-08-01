package io.forgeflow.evaluation;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/evaluations")
public class EvaluationController {
 private final JdbcTemplate db;
 EvaluationController(JdbcTemplate db){this.db=db;}
 @GetMapping("/{runId}") Map<String,Object> evaluate(@PathVariable String runId){
  var runs=db.queryForList("select loop_type,state from workloop_run where id=?::uuid",runId);
  if(runs.isEmpty())throw new IllegalArgumentException("Run not found");
  String loop=runs.getFirst().get("loop_type").toString(),state=runs.getFirst().get("state").toString();
  Integer passed=db.queryForObject("select count(*) from run_phase where run_id=?::uuid and status='PASS'",Integer.class,runId);
  Integer evidence=db.queryForObject("select count(*) from evidence where run_id=?::uuid",Integer.class,runId);
  Integer findings=db.queryForObject("select count(*) from evaluation_finding where run_id=?::uuid and severity in ('CRITICAL','HIGH') and status<>'RESOLVED'",Integer.class,runId);
  int required="DESIGN".equals(loop)?11:13;boolean gates=passed!=null&&passed==required&&evidence!=null&&evidence>=required&&(findings==null||findings==0);
  OptionalInt score=gates?OptionalInt.of(Math.min(100,90+Math.min(10,evidence-required))):OptionalInt.empty();
  return Map.of("runId",runId,"passed",gates,"phasePassCount",passed,"requiredPhases",required,"evidenceCount",evidence,"openCriticalHigh",findings,"state",state,"score",score.isPresent()?score.getAsInt():nullValue());
 }
 private Object nullValue(){return "BLOCKED";}
}
