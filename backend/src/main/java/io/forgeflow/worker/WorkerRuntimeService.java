package io.forgeflow.worker;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermission;
import java.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import io.forgeflow.kernel.*;

@Service
public class WorkerRuntimeService {
 private final Path runRoot,controlRoot;
 private final String workerMode,dockerExecutable,workerImage;private final JdbcTemplate db;private final ObjectMapper mapper;
 WorkerRuntimeService(@Value("${forgeflow.run-root:./run-data}")String runRoot,
  @Value("${forgeflow.control-root:./}")String controlRoot,
  @Value("${forgeflow.worker.mode:fake}")String workerMode,
  @Value("${forgeflow.worker.docker-executable:docker}")String dockerExecutable,
  @Value("${forgeflow.worker.image:}")String workerImage,JdbcTemplate db,ObjectMapper mapper){
  this.runRoot=Path.of(runRoot).toAbsolutePath().normalize();this.controlRoot=Path.of(controlRoot).toAbsolutePath().normalize();
  this.workerMode=workerMode;this.dockerExecutable=dockerExecutable;this.workerImage=workerImage;this.db=db;this.mapper=mapper;
 }
 public WorkerAdapter.Result executeFake(String runId)throws Exception{
  long started=System.nanoTime();
  Path run=runRoot.resolve("runs").resolve(runId);
  Path worktree=runRoot.resolve("generated-projects").resolve("default").resolve("worktrees").resolve(runId);
  Path input=run.resolve("input"),output=run.resolve("output");
  Files.createDirectories(worktree);Files.createDirectories(input);Files.createDirectories(output);
  try{Files.setPosixFilePermissions(input,Set.of(PosixFilePermission.OWNER_READ,PosixFilePermission.OWNER_EXECUTE));}
  catch(UnsupportedOperationException ignored){input.toFile().setWritable(false,false);}
  var contract=new TaskNormalizer().normalize(runId,"Execute approved vertical slice",List.of("produce patch evidence"),
   List.of(Path.of("src")),List.of(Path.of("src")),"0000000000000000000000000000000000000000");
  WorkerAdapter adapter=switch(workerMode){
   case "fake"->new FakeCodexWorkerAdapter();
   case "docker"->new DockerWorkerAdapter(dockerExecutable,workerImage,List.of("forgeflow-agent"),mapper);
   default->throw new IllegalStateException("Unknown worker mode");
  };
  var supervisor=new WorkerSupervisor(controlRoot,runRoot,adapter);
  WorkerAdapter.Result result=new GeneratorKernel(supervisor,new RepositoryMap()).executeWorker(contract,List.of());
  persistUsage(runId,result,System.nanoTime()-started);
  return result;
 }
 @SuppressWarnings("unchecked")
 private void persistUsage(String runId,WorkerAdapter.Result result,long nanos){
  Map<String,Object> usage=result.evidence().get("usage") instanceof Map<?,?> raw?(Map<String,Object>)raw:Map.of();
  long input=number(usage.get("inputTokens")),output=number(usage.get("outputTokens"));
  long calls=number(usage.get("toolCalls")),characters=number(usage.get("toolOutputCharacters"));
  long duration=Math.max(0,nanos/1_000_000);
  db.update("insert into token_ledger(run_id,agent,input_tokens,output_tokens,duration_ms) values (?::uuid,'WORKER',?,?,?)",
   runId,input,output,duration);
  db.update("insert into audit_log(actor,action,target_type,target_id,outcome,details) values ('kernel','AGENT_USAGE','workloop_run',?,'SUCCESS',jsonb_build_object('role','WORKER','toolCalls',?,'toolOutputCharacters',?,'adapter',?))",
   runId,calls,characters,String.valueOf(result.evidence().get("adapter")));
 }
 private static long number(Object value){return value instanceof Number n?n.longValue():0L;}
}
