package io.forgeflow.worker;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import org.junit.jupiter.api.Test;

class DockerSandboxCommandTest{
 @Test void mountsOnlyGeneratedWorktreeAndDisablesPrivilege(){
  Path worktree=Path.of("/data/generated-projects/p1/worktrees/11111111-1111-1111-1111-111111111111");
  var spec=new DockerSandboxSpec(worktree,"/workspace",true,true,List.of("ALL"),Duration.ofMinutes(15),
   1_073_741_824L,1.0,128);
  List<String> command=new DockerSandboxCommand().create(spec,"worker@sha256:"+"a".repeat(64),List.of("agent"));
  assertTrue(command.containsAll(List.of("--network","none","--read-only","--cap-drop","ALL","no-new-privileges")));
  assertEquals(1,command.stream().filter("--mount"::equals).count());
  assertTrue(command.stream().anyMatch(x->x.equals("type=bind,src="+worktree+",dst=/workspace,rw")));
  assertFalse(command.toString().contains("docker.sock"));
  assertFalse(command.toString().contains("generator-ui"));
  assertFalse(command.toString().contains(".git"));
  assertThrows(IllegalArgumentException.class,()->new DockerSandboxCommand().create(spec,"worker:latest",List.of("agent")));
 }
}
