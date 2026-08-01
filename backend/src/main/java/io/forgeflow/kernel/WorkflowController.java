package io.forgeflow.kernel;

import java.util.*;

public final class WorkflowController{
 public interface AgentRunner{AgentEnvelope.Result run(AgentEnvelope request)throws Exception;}
 public record Outcome(String status,List<AgentRole> executed,int repairs,Verifier.Report report){}
 private final AgentRunner runner;
 public WorkflowController(AgentRunner runner){this.runner=runner;}
 public Outcome execute(TaskContract contract,List<AgentEnvelope.ContextItem> context,
                        java.util.function.Supplier<Verifier.Report> verify)throws Exception{
  var executed=new ArrayList<AgentRole>();
  var currentContext=new ArrayList<>(context);
  if(requiresDiscovery(contract)){
   currentContext.add(structured(run(AgentRole.EXPLORER,0,contract,currentContext,executed)));
   currentContext.add(structured(run(AgentRole.PLANNER,0,contract,currentContext,executed)));
  }
  currentContext.add(structured(run(AgentRole.WORKER,0,contract,currentContext,executed)));
  Verifier.Report report=verify.get();
  if(report.passed()&&!report.reviewerRequired(contract))return new Outcome("VERIFIED",List.copyOf(executed),0,report);
  if(report.reviewerRequired(contract))currentContext.add(structured(run(AgentRole.REVIEWER,0,contract,currentContext,executed)));
  int repairs=0;String previousFailure=fingerprint(report);
  while(!report.passed()&&repairs<2){
   repairs++;currentContext.add(structured(run(AgentRole.REPAIRER,repairs,contract,currentContext,executed)));
   Verifier.Report next=verify.get();
   String failure=fingerprint(next);
   if(!next.passed()&&failure.equals(previousFailure)&&repairs==2)
    return new Outcome("STOPPED_REPEATED_FAILURE",List.copyOf(executed),repairs,next);
   previousFailure=failure;report=next;
  }
  return new Outcome(report.passed()?"VERIFIED":"STOPPED_REPAIR_LIMIT",List.copyOf(executed),repairs,report);
 }
 private AgentEnvelope.Result run(AgentRole role,int attempt,TaskContract contract,List<AgentEnvelope.ContextItem> context,List<AgentRole> executed)throws Exception{
  AgentEnvelope.Result result=runner.run(new AgentEnvelope(contract.taskId(),role,contract,context,attempt));
  if(!result.taskId().equals(contract.taskId())||result.role()!=role)throw new SecurityException("Invalid agent JSON contract");
  executed.add(role);
  return result;
 }
 private static boolean requiresDiscovery(TaskContract contract){
  return contract.riskLevel()!=TaskContract.RiskLevel.LOW||contract.writeScopes().size()>1;
 }
 private static AgentEnvelope.ContextItem structured(AgentEnvelope.Result result){
  String content="status="+result.status()+";changedFiles="+result.changedFiles()+";findings="+result.findings()+";usage="+result.usage();
  return new AgentEnvelope.ContextItem("STRUCTURED_RESULT",result.role().name(),content,content.length());
 }
 private static String fingerprint(Verifier.Report report){
  return report.checks().stream().filter(c->!c.passed()).map(c->c.check()+":"+c.evidence()).sorted().toList().toString();
 }
}
