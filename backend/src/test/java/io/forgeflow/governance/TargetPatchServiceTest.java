package io.forgeflow.governance;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TargetPatchServiceTest {
 @TempDir Path temp;
 @Test void appliesAndRollsBackWithoutCommitting()throws Exception{
  Path target=Files.createDirectory(temp.resolve("target")),backup=temp.resolve("backup");
  git(target,"init");git(target,"config","user.email","test@forgeflow.local");git(target,"config","user.name","ForgeFlow Test");
  Files.writeString(target.resolve("hello.txt"),"before\n");git(target,"add","hello.txt");git(target,"commit","-m","baseline");
  String revision=git(target,"rev-parse","HEAD").trim();
  String diff="diff --git a/hello.txt b/hello.txt\n--- a/hello.txt\n+++ b/hello.txt\n@@ -1 +1 @@\n-before\n+after\n";
  var service=new TargetPatchService(target,backup);
  var result=service.apply(diff,Set.of(Path.of("hello.txt")),revision);
  assertEquals("after\n",Files.readString(target.resolve("hello.txt")));
  assertEquals(revision,result.revision());
  service.rollback(result.rollbackToken(),result.changedFiles());
  assertEquals("before\n",Files.readString(target.resolve("hello.txt")));
 }
 private String git(Path directory,String...args)throws Exception{
  var command=new ArrayList<String>();command.add("git");command.addAll(List.of(args));
  Process process=new ProcessBuilder(command).directory(directory.toFile()).redirectErrorStream(true).start();
  String output=new String(process.getInputStream().readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);
  if(process.waitFor()!=0)throw new IllegalStateException(output);return output;
 }
}
