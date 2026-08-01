package io.forgeflow.kernel;

public final class ModelRouter{
 public record Route(String model,AgentRole role,int maxOutputTokens){}
 public Route route(TaskContract contract,AgentRole role){
  String model=(contract.riskLevel().compareTo(TaskContract.RiskLevel.HIGH)>=0
   ||role==AgentRole.REVIEWER||role==AgentRole.REPAIRER)?"reasoning":"worker";
  int output=(int)Math.min(contract.tokenBudget().output(),role==AgentRole.WORKER?2_000:1_000);
  return new Route(model,role,output);
 }
}
