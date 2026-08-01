package io.forgeflow.api;
import java.net.URI;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
@RestControllerAdvice
public class ProblemHandler{
 @ExceptionHandler({IllegalArgumentException.class,IllegalStateException.class,SecurityException.class})
 ResponseEntity<ProblemDetail> handle(RuntimeException e){
  var p=ProblemDetail.forStatus(e instanceof SecurityException?HttpStatus.FORBIDDEN:e instanceof IllegalArgumentException?HttpStatus.BAD_REQUEST:HttpStatus.CONFLICT);
  p.setType(URI.create("urn:forgeflow:request-rejected"));p.setTitle("Request rejected");p.setDetail(e.getMessage());
  return ResponseEntity.status(p.getStatus()).body(p);
 }
}
