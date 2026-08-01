package io.forgeflow.kernel;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public record TaskContract(
 String taskId,
 String goal,
 List<String> acceptanceCriteria,
 List<String> constraints,
 List<Path> readScopes,
 List<Path> writeScopes,
 List<Path> denyScopes,
 RiskLevel riskLevel,
 TokenBudget tokenBudget,
 String baseCommitSha
){
 public enum RiskLevel{LOW,MEDIUM,HIGH,CRITICAL}
 public record TokenBudget(long input,long output){
  public TokenBudget{if(input<1||output<1)throw new IllegalArgumentException("Token budgets must be positive");}
 }
 public TaskContract{
  if(taskId==null||!taskId.matches("[0-9a-fA-F-]{36}"))throw new IllegalArgumentException("Invalid taskId");
  if(goal==null||goal.isBlank())throw new IllegalArgumentException("Goal required");
  acceptanceCriteria=List.copyOf(acceptanceCriteria);
  constraints=List.copyOf(constraints);
  List<Path> normalizedRead=normalize(readScopes);
  List<Path> normalizedWrite=normalize(writeScopes);
  List<Path> normalizedDeny=normalize(denyScopes);
  readScopes=normalizedRead;
  writeScopes=normalizedWrite;
  denyScopes=normalizedDeny;
  Objects.requireNonNull(riskLevel);Objects.requireNonNull(tokenBudget);
  if(baseCommitSha==null||!baseCommitSha.matches("[0-9a-f]{40}"))throw new IllegalArgumentException("Base commit SHA required");
  if(normalizedWrite.stream().anyMatch(w->normalizedRead.stream().noneMatch(w::startsWith)))throw new IllegalArgumentException("Write scope must be readable");
  if(normalizedWrite.stream().anyMatch(w->normalizedDeny.stream().anyMatch(w::startsWith)))throw new IllegalArgumentException("Write and deny scopes overlap");
 }
 private static List<Path> normalize(List<Path> paths){
  return paths.stream().map(p->{
   if(p.isAbsolute()||p.toString().contains(".."))throw new IllegalArgumentException("Scopes must be relative");
   Path n=p.normalize();if(n.toString().isBlank())throw new IllegalArgumentException("Empty scope");
   return n;
  }).distinct().toList();
 }
}
