package io.forgeflow.worker;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
class PathGuardTest{
 @TempDir Path temp;
 @Test void allowsOnlyRealPathInsideRun()throws Exception{
  Path root=Files.createDirectory(temp.resolve("run"));Path ok=Files.createFile(root.resolve("patch.diff"));
  var guard=new PathGuard(java.util.List.of(root));
  assertEquals(ok.toRealPath(),guard.requireAllowed(ok.toAbsolutePath()));
  Path outside=Files.createFile(temp.resolve("secret"));
  assertThrows(SecurityException.class,()->guard.requireAllowed(outside.toAbsolutePath()));
  assertThrows(SecurityException.class,()->guard.requireAllowed(root.resolve("..").resolve("secret").toAbsolutePath()));
 }
}
