package io.forgeflow.worker;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;

class DockerWorkerAdapterTest{
 @Test void requiresTypedCommand(){
  assertThrows(IllegalArgumentException.class,()->new DockerWorkerAdapter("docker",
   "worker@sha256:"+"a".repeat(64),List.of(),new ObjectMapper()));
 }
}
