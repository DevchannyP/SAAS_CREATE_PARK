create table design_artifact(
  id uuid primary key default gen_random_uuid(),
  screen_id text not null,
  event_id text not null,
  artifact_type text not null,
  artifact_version int not null,
  content text not null,
  content_hash text not null,
  status text not null default 'DRAFT',
  evaluation_result jsonb not null default '{}',
  created_at timestamptz not null default now(),
  created_by text not null,
  unique(screen_id,event_id,artifact_type,artifact_version)
);
create index design_artifact_context_idx on design_artifact(screen_id,event_id,created_at desc);
