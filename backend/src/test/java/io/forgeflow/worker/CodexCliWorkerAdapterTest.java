package io.forgeflow.worker;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.Test;

class CodexCliWorkerAdapterTest{
 @Test void directHostExecutionIsPermanentlyDisabled(){
  var adapter=new CodexCliWorkerAdapter("codex",List.of("--sandbox","--cd"));
  assertThrows(SecurityException.class,()->adapter.execute(null));
 }
}
