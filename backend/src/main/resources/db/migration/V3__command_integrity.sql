create table command_idempotency(
  idempotency_key text primary key,
  request_method text not null,
  request_path text not null,
  request_id text not null,
  actor text not null,
  created_at timestamptz not null default now()
);
create index audit_log_created_idx on audit_log(created_at desc);
