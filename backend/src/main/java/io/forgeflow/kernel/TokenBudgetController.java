package io.forgeflow.kernel;

import java.util.EnumMap;
import java.util.Map;

public final class TokenBudgetController{
 public record Usage(long inputTokens,long outputTokens,long toolCalls,long toolOutputCharacters){}
 private final TaskContract.TokenBudget budget;
 private final Map<AgentRole,Usage> ledger=new EnumMap<>(AgentRole.class);
 public TokenBudgetController(TaskContract.TokenBudget budget){this.budget=budget;}
 public synchronized void record(AgentRole role,Usage next){
  long in=ledger.values().stream().mapToLong(Usage::inputTokens).sum()+next.inputTokens();
  long out=ledger.values().stream().mapToLong(Usage::outputTokens).sum()+next.outputTokens();
  if(in>budget.input()||out>budget.output())throw new IllegalStateException("BLOCKED_BUDGET");
  Usage old=ledger.getOrDefault(role,new Usage(0,0,0,0));
  ledger.put(role,new Usage(old.inputTokens()+next.inputTokens(),old.outputTokens()+next.outputTokens(),
   old.toolCalls()+next.toolCalls(),old.toolOutputCharacters()+next.toolOutputCharacters()));
 }
 public synchronized Map<AgentRole,Usage> ledger(){return Map.copyOf(ledger);}
}
