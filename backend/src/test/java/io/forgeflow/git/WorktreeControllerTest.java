package io.forgeflow.git;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

class WorktreeControllerTest{
 @TempDir Path temp;
 @Test void createsDetachedWorktreeAtPhysicalBoundary()throws Exception{
  Path repo=Files.createDirectory(temp.resolve("target"));Files.createDirectory(repo.resolve(".git"));
  Path generated=temp.resolve("generated-projects");String sha="a".repeat(40);
  List<List<String>> calls=new ArrayList<>();
  var controller=new WorktreeController(generated,(root,args)->{
   calls.add(args);
   if(args.getFirst().equals("worktree")){
    Path destination=Path.of(args.get(3));Files.createDirectories(destination);Files.writeString(destination.resolve(".git"),"gitdir: controller-only");
   }
   return sha;
  });
  Path result=controller.create(repo,"project-1","11111111-1111-1111-1111-111111111111",sha);
  assertTrue(result.startsWith(generated.toAbsolutePath()));
  assertEquals(List.of("worktree","add","--detach",result.toString(),sha),calls.get(1));
 }
 @Test void rejectsSubmodulesAndInvalidIdentity()throws Exception{
  Path repo=Files.createDirectory(temp.resolve("target"));Files.createDirectory(repo.resolve(".git"));
  Files.writeString(repo.resolve(".gitmodules"),"[submodule \"x\"]");
  var controller=new WorktreeController(temp.resolve("generated-projects"),(root,args)->"a".repeat(40));
  assertThrows(SecurityException.class,()->controller.create(repo,"p","11111111-1111-1111-1111-111111111111","a".repeat(40)));
  assertThrows(SecurityException.class,()->controller.create(repo,"../p","11111111-1111-1111-1111-111111111111","a".repeat(40)));
 }
}
