export type CodeFile={name:string;layer:string;path:string;language:string;content:string};

export const codeFiles:CodeFile[]=[
 {name:"ArchitectureController",layer:"Controller",path:"backend/src/main/java/io/forgeflow/architecture/ArchitectureController.java",language:"java",content:`package io.forgeflow.architecture;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/architecture-profiles")
public class ArchitectureController {
 record Profile(String name, List<String> layers, String actor) {}
 private final JdbcTemplate db;
 private final ObjectMapper mapper;

 ArchitectureController(JdbcTemplate db, ObjectMapper mapper) {
  this.db = db;
  this.mapper = mapper;
 }

 @GetMapping
 List<Map<String,Object>> list() {
  return db.queryForList("select id,name,layers,version,status from architecture_profile where status='ACTIVE' order by name");
 }

 @Transactional
 @PostMapping
 Map<String,Object> create(@RequestBody Profile request) {
  if (request.name() == null || request.name().isBlank() || request.layers() == null || request.layers().isEmpty())
   throw new IllegalArgumentException("Profile name and ordered layers are required");
  return Map.of("name", request.name(), "layers", request.layers());
 }
}`},
 {name:"ProjectController",layer:"Controller",path:"backend/src/main/java/io/forgeflow/project/ProjectController.java",language:"java",content:`package io.forgeflow.project;

import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/projects")
public class ProjectController {
 private final JdbcTemplate db;
 ProjectController(JdbcTemplate db) { this.db = db; }

 @GetMapping
 List<Map<String,Object>> list() {
  return db.queryForList("select id,name,target_path as targetPath,status from project order by created_at desc");
 }
}`},
 {name:"ForgeFlowApplication",layer:"Service/Application",path:"backend/src/main/java/io/forgeflow/ForgeFlowApplication.java",language:"java",content:`package io.forgeflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ForgeFlowApplication {
 public static void main(String[] args) {
  SpringApplication.run(ForgeFlowApplication.class, args);
 }
}`},
 {name:"PolicyEngine",layer:"Domain·Policy",path:"backend/src/main/java/io/forgeflow/kernel/PolicyEngine.java",language:"java",content:`package io.forgeflow.kernel;

import org.springframework.stereotype.Component;

@Component
public class PolicyEngine {
 public void require(boolean condition, String message) {
  if (!condition) throw new IllegalStateException(message);
 }
}`},
 {name:"RepositoryMap",layer:"Repository·Mapper",path:"backend/src/main/java/io/forgeflow/kernel/RepositoryMap.java",language:"java",content:`package io.forgeflow.kernel;

import java.util.List;

public record RepositoryMap(
 String projectId,
 List<String> sourceRoots,
 List<String> protectedPaths
) {}`},
 {name:"V1__core.sql",layer:"SQL",path:"backend/src/main/resources/db/migration/V1__core.sql",language:"sql",content:`-- ForgeFlow core schema
create table if not exists project (
 id uuid primary key,
 name text not null,
 target_path text not null,
 status text not null default 'ACTIVE',
 created_at timestamptz not null default now()
);`}
];

export const architectureLayers=["Controller","Service/Application","Domain·Policy","Repository·Mapper","SQL"];
