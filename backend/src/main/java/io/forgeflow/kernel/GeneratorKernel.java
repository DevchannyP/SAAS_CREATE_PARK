package io.forgeflow.kernel;

import io.forgeflow.worker.*;
import java.util.*;
import java.util.function.Supplier;

public final class GeneratorKernel{
 private final WorkerSupervisor supervisor;private final RepositoryMap repositoryMap;
 private final ContextBuilder contextBuilder;private final ModelRouter modelRouter;
 public GeneratorKernel(WorkerSupervisor supervisor,RepositoryMap repositoryMap){
  this.supervisor=supervisor;this.repositoryMap=repositoryMap;
  this.contextBuilder=new ContextBuilder();this.modelRouter=new ModelRouter();
 }
 public WorkerAdapter.Result executeWorker(TaskContract contract,List<AgentEnvelope.ContextItem> selectedSymbols)throws Exception{
  List<RepositoryMap.Entry> selectedMap=repositoryMap.select(contract.readScopes());
  List<AgentEnvelope.ContextItem> context=contextBuilder.build(contract,selectedMap,selectedSymbols,List.of());
  modelRouter.route(contract,AgentRole.WORKER);
  return supervisor.execute(contract,AgentRole.WORKER,context);
 }
 public WorkflowController.Outcome execute(TaskContract contract,List<AgentEnvelope.ContextItem> selectedSymbols,
                                           Supplier<Verifier.Report> deterministicVerifier)throws Exception{
  List<RepositoryMap.Entry> selectedMap=repositoryMap.select(contract.readScopes());
  List<AgentEnvelope.ContextItem> context=contextBuilder.build(contract,selectedMap,selectedSymbols,List.of());
  WorkflowController flow=new WorkflowController(request->{
   modelRouter.route(contract,request.role());
   WorkerAdapter.Result result=supervisor.execute(contract,request.role(),request.context());
   Map<String,Long> usage=usage(result.evidence().get("usage"));
   return new AgentEnvelope.Result(contract.taskId(),request.role(),result.exitCode()==0?"OK":"FAILED",
    strings(result.evidence().get("changedFiles")),strings(result.evidence().get("findings")),usage);
  });
  return flow.execute(contract,context,deterministicVerifier);
 }
 @SuppressWarnings("unchecked")
 private static Map<String,Long> usage(Object raw){
  if(!(raw instanceof Map<?,?> map))return Map.of();
  Map<String,Long> result=new TreeMap<>();
  map.forEach((k,v)->{if(k instanceof String key&&v instanceof Number number)result.put(key,number.longValue());});
  return Map.copyOf(result);
 }
 private static List<String> strings(Object raw){
  if(!(raw instanceof Collection<?> values))return List.of();
  return values.stream().map(String::valueOf).toList();
 }
}
