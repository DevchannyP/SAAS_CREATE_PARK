package io.forgeflow.kernel;

import java.util.List;
import java.util.Map;

public record AgentEnvelope(
 String taskId,AgentRole role,TaskContract contract,List<ContextItem> context,int attempt
){
 public record ContextItem(String kind,String reference,String content,int characters){}
 public record Result(String taskId,AgentRole role,String status,List<String> changedFiles,
                      List<String> findings,Map<String,Long> usage){}
}
