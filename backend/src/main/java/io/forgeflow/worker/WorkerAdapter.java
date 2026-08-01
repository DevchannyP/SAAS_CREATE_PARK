package io.forgeflow.worker;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import io.forgeflow.kernel.*;
public interface WorkerAdapter{
 record Request(String runId,AgentRole role,Path worktree,Path input,Path output,TaskContract contract,
                List<AgentEnvelope.ContextItem> context,Duration timeout){}
 record Result(int exitCode,Path patch,Map<String,Object> evidence){}
 Result execute(Request request)throws Exception;
}
