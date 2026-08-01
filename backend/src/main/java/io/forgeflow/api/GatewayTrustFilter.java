package io.forgeflow.api;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component @Order(Ordered.HIGHEST_PRECEDENCE)
public class GatewayTrustFilter extends OncePerRequestFilter {
 private final byte[] expected;private final ObjectMapper mapper;
 GatewayTrustFilter(@Value("${forgeflow.gateway-token}")String token,ObjectMapper mapper){
  if(token==null||token.length()<16)throw new IllegalStateException("Gateway token must contain at least 16 characters");
  this.expected=token.getBytes(StandardCharsets.UTF_8);this.mapper=mapper;
 }
 @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)throws ServletException,IOException{
  boolean protectedPath=request.getRequestURI().startsWith("/api/v1/")||request.getRequestURI().startsWith("/actuator/");
  byte[] supplied=String.valueOf(request.getHeader("X-ForgeFlow-Gateway")).getBytes(StandardCharsets.UTF_8);
  if(protectedPath&&!MessageDigest.isEqual(expected,supplied)){
   response.setStatus(403);response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
   mapper.writeValue(response.getOutputStream(),Map.of("type","urn:forgeflow:gateway-required","title","Forbidden","status",403,"detail","Trusted gateway required"));return;
  }
  chain.doFilter(request,response);
 }
}
