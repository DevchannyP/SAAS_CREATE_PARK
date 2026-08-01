package io.forgeflow.kernel;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class WorkflowControllerTest{
 private TaskContract contract(TaskContract.RiskLevel risk){
  return new TaskContract(UUID.randomUUID().toString(),"goal",List.of("pass"),List.of(),
   List.of(Path.of("src")),List.of(Path.of("src")),List.of(Path.of(".git")),risk,
   new TaskContract.TokenBudget(1000,500),"b".repeat(40));
 }
 private Verifier.Report report(boolean pass){
  return new Verifier.Report(List.of(new Verifier.CheckResult(Verifier.Check.REPOSITORY_TYPE,pass,pass?"git":"FAIL:x")),1,2);
 }
 @Test void lowRiskPassRunsOnlyWorker()throws Exception{
  var flow=new WorkflowController(r->new AgentEnvelope.Result(r.taskId(),r.role(),"OK",List.of(),List.of(),Map.of()));
  var out=flow.execute(contract(TaskContract.RiskLevel.LOW),List.of(),()->report(true));
  assertEquals(List.of(AgentRole.WORKER),out.executed());
 }
 @Test void failedChecksRunReviewerAndAtMostTwoRepairs()throws Exception{
  AtomicInteger calls=new AtomicInteger();
  var flow=new WorkflowController(r->{calls.incrementAndGet();return new AgentEnvelope.Result(r.taskId(),r.role(),"OK",List.of(),List.of(),Map.of());});
  var out=flow.execute(contract(TaskContract.RiskLevel.LOW),List.of(),()->report(false));
  assertEquals("STOPPED_REPEATED_FAILURE",out.status());assertEquals(2,out.repairs());
  assertEquals(List.of(AgentRole.WORKER,AgentRole.REVIEWER,AgentRole.REPAIRER,AgentRole.REPAIRER),out.executed());
 }
 @Test void highRiskUsesExplorerPlannerAndReviewer()throws Exception{
  var flow=new WorkflowController(r->new AgentEnvelope.Result(r.taskId(),r.role(),"OK",List.of(),List.of(),Map.of()));
  var out=flow.execute(contract(TaskContract.RiskLevel.HIGH),List.of(),()->report(true));
  assertEquals(List.of(AgentRole.EXPLORER,AgentRole.PLANNER,AgentRole.WORKER,AgentRole.REVIEWER),out.executed());
  assertEquals("VERIFIED",out.status());
 }
}
