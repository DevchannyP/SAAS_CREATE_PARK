package io.forgeflow.git;

import static org.junit.jupiter.api.Assertions.*;
import io.forgeflow.kernel.*;
import java.nio.file.Path;
import java.util.*;
import org.junit.jupiter.api.Test;

class GitControllerTest{
 private TaskContract contract(){return new TaskContract(UUID.randomUUID().toString(),"goal",List.of("pass"),List.of(),
  List.of(Path.of("src")),List.of(Path.of("src")),List.of(Path.of(".git")),TaskContract.RiskLevel.LOW,
  new TaskContract.TokenBudget(100,100),"c".repeat(40));}
 private WorkflowController.Outcome outcome(boolean passed){
  var report=new Verifier.Report(List.of(new Verifier.CheckResult(Verifier.Check.PATH_POLICY,passed,passed?"ok":"FAIL")),1,1);
  return new WorkflowController.Outcome(passed?"VERIFIED":"STOPPED_REPAIR_LIMIT",List.of(AgentRole.WORKER),0,report);
 }
 @Test void noCommitIsCreatedOnPolicyFailure(){
  List<List<String>> commands=new ArrayList<>();
  var git=new GitController(args->{commands.add(args);return "d".repeat(40);},new PolicyEngine(),(sha,branch)->true);
  assertThrows(SecurityException.class,()->git.publish(contract(),outcome(false),List.of(Path.of("src/A.java")),"task/x"));
  assertTrue(commands.isEmpty());
 }
 @Test void protectsMainAndStagesOnlyAllowedFiles(){
  List<List<String>> commands=new ArrayList<>();
  var git=new GitController(args->{commands.add(args);return "d".repeat(40);},new PolicyEngine(),(sha,branch)->true);
  assertThrows(SecurityException.class,()->git.publish(contract(),outcome(true),List.of(Path.of("src/A.java")),"main"));
  assertTrue(commands.isEmpty());
  assertThrows(SecurityException.class,()->git.publish(contract(),outcome(true),List.of(Path.of("src/A.java")),"task/x:main"));
  assertTrue(commands.isEmpty());
  assertThrows(SecurityException.class,()->git.publish(contract(),outcome(true),List.of(Path.of("other/A.java")),"task/x"));
  assertTrue(commands.isEmpty());
 }
 @Test void agentsCannotUseGitWrites(){
  var git=new GitController(args->"",new PolicyEngine(),(sha,branch)->true);
  assertThrows(SecurityException.class,()->git.rejectAgentGitCommand(List.of("commit","-m","x")));
  assertThrows(SecurityException.class,()->git.rejectAgentGitCommand(List.of("push","--force")));
 }
 @Test void cleanCheckoutCiMustPass()throws Exception{
  List<List<String>> commands=new ArrayList<>();
  var git=new GitController(args->{commands.add(args);return "d".repeat(40);},new PolicyEngine(),(sha,branch)->false);
  assertThrows(IllegalStateException.class,()->git.publish(contract(),outcome(true),List.of(Path.of("src/A.java")),"task/x"));
  assertTrue(commands.stream().anyMatch(c->c.getFirst().equals("push")));
 }
}
