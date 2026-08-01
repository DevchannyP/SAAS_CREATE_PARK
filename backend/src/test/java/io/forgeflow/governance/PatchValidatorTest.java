package io.forgeflow.governance;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.Test;
class PatchValidatorTest{
 @Test void rejectsPathsOutsidePlan(){
  var v=new PatchValidator();
  assertTrue(v.validate("--- a/src/A.java\n+++ b/src/A.java\n",Set.of(Path.of("src/A.java"))).valid());
  assertFalse(v.validate("--- a/../.env\n+++ b/../.env\n",Set.of(Path.of("src/A.java"))).valid());
  assertFalse(v.validate("--- a/frontend/App.tsx\n+++ b/frontend/App.tsx\n",Set.of(Path.of("src/A.java"))).valid());
 }
}
