package io.forgeflow.worker;
import java.util.List;
public final class CodexCliWorkerAdapter implements WorkerAdapter{
 public CodexCliWorkerAdapter(String executable,List<String> codexHelpLines){}
 public Result execute(Request request){throw new SecurityException("Direct host CLI permanently disabled; use DockerSandboxCommand and ToolGateway");}
}
