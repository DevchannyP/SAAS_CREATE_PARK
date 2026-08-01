package io.forgeflow.worker;

import java.util.*;

public final class DockerSandboxCommand{
 public List<String> create(DockerSandboxSpec spec,String image,List<String> agentCommand){
  if(image==null||!image.matches(".+@sha256:[0-9a-f]{64}"))
   throw new IllegalArgumentException("Worker image must be digest-pinned");
  var command=new ArrayList<>(List.of("docker","run","--rm","--network","none","--read-only",
   "--cap-drop","ALL","--security-opt","no-new-privileges","--pids-limit",String.valueOf(spec.pids()),
   "--memory",String.valueOf(spec.memoryBytes()),"--cpus",String.valueOf(spec.cpus()),
   "--user","65532:65532","--tmpfs","/tmp:rw,noexec,nosuid,size=64m",
   "--mount","type=bind,src="+spec.hostWorktree()+",dst=/workspace,rw",
   "--workdir","/workspace","--env","HOME=/tmp/agent-home"));
  command.add(image);command.addAll(agentCommand);
  return List.copyOf(command);
 }
}
