create table youtube_magazine_artifact (
  id uuid primary key,
  job_id uuid not null references youtube_magazine_job(id) on delete cascade,
  kind varchar(32) not null,
  content_json jsonb not null,
  created_at timestamptz not null default now(),
  unique(job_id, kind)
);

create index youtube_magazine_artifact_job_idx on youtube_magazine_artifact(job_id, created_at);
