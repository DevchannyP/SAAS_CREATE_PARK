alter table command_idempotency add column completed boolean not null default false;
alter table command_idempotency add column response_status int;
alter table command_idempotency add column response_content_type text;
alter table command_idempotency add column response_body text;
