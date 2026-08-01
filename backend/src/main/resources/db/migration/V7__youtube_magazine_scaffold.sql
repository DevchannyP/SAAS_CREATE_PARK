create table youtube_magazine_video (
  id uuid primary key,
  video_id varchar(32) not null unique,
  title text not null,
  channel_title text not null,
  category_id varchar(16) not null,
  published_at timestamptz not null,
  view_count bigint not null default 0,
  like_count bigint not null default 0,
  comment_count bigint not null default 0,
  hot_score numeric(12,4) not null default 0,
  thumbnail_url text,
  collected_at timestamptz not null default now()
);

create table youtube_magazine_group (
  id uuid primary key,
  group_title text not null,
  category_id varchar(16) not null,
  topic_keyword text not null,
  created_at timestamptz not null default now()
);

create table youtube_magazine_group_item (
  id uuid primary key,
  group_id uuid not null references youtube_magazine_group(id) on delete cascade,
  video_id uuid not null references youtube_magazine_video(id),
  rank_no int not null check (rank_no between 1 and 6),
  score numeric(12,4) not null,
  reason text not null,
  unique(group_id, rank_no), unique(group_id, video_id)
);

create table youtube_magazine_job (
  id uuid primary key,
  group_id uuid references youtube_magazine_group(id),
  status varchar(32) not null,
  stage varchar(64) not null,
  progress int not null default 0 check (progress between 0 and 100),
  format varchar(16) not null default 'SHORTS',
  privacy_status varchar(16) not null default 'private',
  quality_score int,
  risk_score int,
  output_path text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index youtube_magazine_video_hot_idx on youtube_magazine_video(hot_score desc);
create index youtube_magazine_job_created_idx on youtube_magazine_job(created_at desc);
