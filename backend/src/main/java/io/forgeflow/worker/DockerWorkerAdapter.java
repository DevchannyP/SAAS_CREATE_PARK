package io.forgeflow.worker;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.forgeflow.kernel.AgentEnvelope;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

public final class DockerWorkerAdapter implements WorkerAdapter{
 private static final long MAX_RESULT_BYTES=1_048_576;
 private final String dockerExecutable,image;private final List<String> agentCommand;
 private final ObjectMapper mapper;private final DockerSandboxCommand commands;
 public DockerWorkerAdapter(String dockerExecutable,String image,List<String> agentCommand,ObjectMapper mapper){
  this.dockerExecutable=dockerExecutable;this.image=image;this.agentCommand=List.copyOf(agentCommand);
  this.mapper=mapper;this.commands=new DockerSandboxCommand();
  if(agentCommand.isEmpty())throw new IllegalArgumentException("Typed agent command required");
 }
 @Override public Result execute(Request request)throws Exception{
  DockerSandboxSpec spec=new DockerSandboxSpec(request.worktree(),"/workspace",true,true,List.of("ALL"),
   request.timeout(),1_073_741_824L,1.0,128);
  List<String> command=new ArrayList<>(commands.create(spec,image,agentCommand));
  command.set(0,dockerExecutable);
  Files.createDirectories(request.output());
  Path stdout=request.output().resolve("agent-result.json"),stderr=request.output().resolve("agent-diagnostic.txt");
  ProcessBuilder builder=new ProcessBuilder(command).directory(request.worktree().toFile())
   .redirectOutput(stdout.toFile()).redirectError(stderr.toFile());
  builder.environment().clear();
  Process process=builder.start();
  boolean ended=process.waitFor(request.timeout().toMillis(),TimeUnit.MILLISECONDS);
  if(!ended){process.destroyForcibly();throw new IllegalStateException("Worker timeout");}
  if(Files.size(stdout)>MAX_RESULT_BYTES||Files.size(stderr)>MAX_RESULT_BYTES)throw new SecurityException("Worker output limit exceeded");
  if(process.exitValue()!=0)throw new IllegalStateException("Worker failed with exit code "+process.exitValue());
  AgentEnvelope.Result json=mapper.readValue(stdout.toFile(),AgentEnvelope.Result.class);
  if(!request.runId().equals(json.taskId())||request.role()!=json.role())throw new SecurityException("Invalid agent result contract");
  Path patch=request.output().resolve("patch.diff");
  if(!Files.exists(patch))Files.writeString(patch,"");
  Map<String,Object> evidence=new LinkedHashMap<>();
  evidence.put("adapter","docker");evidence.put("role",json.role().name());
  evidence.put("status",json.status());evidence.put("usage",json.usage());
  evidence.put("changedFiles",json.changedFiles());evidence.put("findings",json.findings());
  return new Result(0,patch,Map.copyOf(evidence));
 }
}
