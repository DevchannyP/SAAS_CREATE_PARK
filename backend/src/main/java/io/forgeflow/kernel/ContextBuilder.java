package io.forgeflow.kernel;

import java.util.*;

public final class ContextBuilder{
 public List<AgentEnvelope.ContextItem> build(TaskContract contract,List<RepositoryMap.Entry> map,
                                               List<AgentEnvelope.ContextItem> symbols,
                                               List<AgentEnvelope.ContextItem> prior){
  var result=new ArrayList<AgentEnvelope.ContextItem>();
  result.add(new AgentEnvelope.ContextItem("POLICY","scoped-harness",
   "JSON only; no agent-to-agent communication; no git or network; use approved tools and scopes only",85));
  result.add(new AgentEnvelope.ContextItem("CONTRACT",contract.taskId(),contract.goal(),contract.goal().length()));
  map.forEach(e->result.add(new AgentEnvelope.ContextItem("REPOSITORY_MAP",e.path().toString(),
   String.join(",",e.symbols()),String.join(",",e.symbols()).length())));
  result.addAll(symbols);result.addAll(prior);
  long chars=result.stream().mapToLong(AgentEnvelope.ContextItem::characters).sum();
  if(chars>contract.tokenBudget().input()*4)throw new IllegalStateException("BLOCKED_BUDGET");
  return List.copyOf(result);
 }
}
