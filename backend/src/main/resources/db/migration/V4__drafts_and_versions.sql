alter table thread add column version int not null default 1;
alter table project add column architecture_profile_id uuid references architecture_profile(id);

create table harness_draft(
  loop_type text not null,
  agent_id text not null,
  content text not null,
  version int not null default 1,
  updated_by text not null,
  updated_at timestamptz not null default now(),
  primary key(loop_type,agent_id)
);
