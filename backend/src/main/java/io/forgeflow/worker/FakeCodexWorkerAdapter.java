package io.forgeflow.worker;
import java.util.Map;
public final class FakeCodexWorkerAdapter implements WorkerAdapter{
 public Result execute(Request r)throws Exception{
  java.nio.file.Files.createDirectories(r.output());
  var patch=r.output().resolve("patch.diff");java.nio.file.Files.writeString(patch,"");
  return new Result(0,patch,Map.of("adapter","fake","runId",r.runId(),"role",r.role().name(),
   "readScopes",r.contract().readScopes(),"writeScopes",r.contract().writeScopes(),"contextItems",r.context().size(),
   "usage",Map.of("inputTokens",0L,"outputTokens",0L,"toolCalls",0L,"toolOutputCharacters",0L)));
 }
}
