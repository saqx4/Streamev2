-- Base tables for Streame Cloud.
-- These are the core tables referenced by the app and by later migrations.
-- Drop legacy tables that may exist with incompatible schemas from a previous setup.
drop table if exists public.profiles cascade;
drop table if exists public.watch_history cascade;
drop table if exists public.watchlist cascade;
drop table if exists public.watched_movies cascade;
drop table if exists public.watched_episodes cascade;
drop table if exists public.auth_audit_log cascade;
drop table if exists public.sync_state cascade;

-- User profiles (one row per Supabase Auth user)
create table if not exists public.profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  email text,
  trakt_token jsonb,
  default_subtitle text,
  auto_play_next boolean default true,
  addons text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

alter table public.profiles enable row level security;
alter table public.profiles force row level security;

do $$
begin
  if not exists (
    select 1
    from pg_policies
    where schemaname = 'public'
      and tablename = 'profiles'
      and policyname = 'users_manage_own_profiles'
  ) then
    create policy users_manage_own_profiles
      on public.profiles
      for all
      to authenticated
      using ((select auth.uid()) = id)
      with check ((select auth.uid()) = id);
  end if;
end $$;

-- Watch history (continue watching / resume progress)
create table if not exists public.watch_history (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  media_type text not null check (media_type in ('movie', 'tv')),
  show_tmdb_id integer,
  show_trakt_id integer,
  season integer,
  episode integer,
  trakt_episode_id integer,
  tmdb_episode_id integer,
  progress float not null default 0,
  position_seconds bigint not null default 0,
  duration_seconds bigint not null default 0,
  paused_at timestamptz,
  source text,
  title text,
  episode_title text,
  backdrop_path text,
  poster_path text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

alter table public.watch_history enable row level security;
alter table public.watch_history force row level security;

do $$
begin
  if not exists (
    select 1
    from pg_policies
    where schemaname = 'public'
      and tablename = 'watch_history'
      and policyname = 'users_manage_own_watch_history'
  ) then
    create policy users_manage_own_watch_history
      on public.watch_history
      for all
      to authenticated
      using ((select auth.uid()) = user_id)
      with check ((select auth.uid()) = user_id);
  end if;
end $$;

create index if not exists watch_history_user_updated_idx
  on public.watch_history (user_id, updated_at desc);

-- Watchlist
create table if not exists public.watchlist (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  tmdb_id integer not null,
  media_type text not null check (media_type in ('movie', 'tv')),
  added_at timestamptz not null default now()
);

alter table public.watchlist enable row level security;
alter table public.watchlist force row level security;

do $$
begin
  if not exists (
    select 1
    from pg_policies
    where schemaname = 'public'
      and tablename = 'watchlist'
      and policyname = 'users_manage_own_watchlist'
  ) then
    create policy users_manage_own_watchlist
      on public.watchlist
      for all
      to authenticated
      using ((select auth.uid()) = user_id)
      with check ((select auth.uid()) = user_id);
  end if;
end $$;

create unique index if not exists watchlist_user_tmdb_media_uidx
  on public.watchlist (user_id, tmdb_id, media_type);

create index if not exists watchlist_user_added_idx
  on public.watchlist (user_id, added_at desc);

-- Watched movies
create table if not exists public.watched_movies (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  tmdb_id integer not null,
  trakt_id integer,
  watched_at timestamptz not null default now()
);

alter table public.watched_movies enable row level security;
alter table public.watched_movies force row level security;

do $$
begin
  if not exists (
    select 1
    from pg_policies
    where schemaname = 'public'
      and tablename = 'watched_movies'
      and policyname = 'users_manage_own_watched_movies'
  ) then
    create policy users_manage_own_watched_movies
      on public.watched_movies
      for all
      to authenticated
      using ((select auth.uid()) = user_id)
      with check ((select auth.uid()) = user_id);
  end if;
end $$;

-- Watched episodes
create table if not exists public.watched_episodes (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  tmdb_id integer not null,
  season integer not null,
  episode integer not null,
  trakt_episode_id integer,
  tmdb_episode_id integer,
  show_trakt_id integer,
  watched boolean not null default true,
  watched_at timestamptz not null default now(),
  source text,
  updated_at timestamptz not null default now()
);

alter table public.watched_episodes enable row level security;
alter table public.watched_episodes force row level security;

do $$
begin
  if not exists (
    select 1
    from pg_policies
    where schemaname = 'public'
      and tablename = 'watched_episodes'
      and policyname = 'users_manage_own_watched_episodes'
  ) then
    create policy users_manage_own_watched_episodes
      on public.watched_episodes
      for all
      to authenticated
      using ((select auth.uid()) = user_id)
      with check ((select auth.uid()) = user_id);
  end if;
end $$;

-- Auth audit log (used by edge functions)
create table if not exists public.auth_audit_log (
  id uuid primary key default gen_random_uuid(),
  user_id uuid references auth.users(id) on delete set null,
  action text not null,
  metadata jsonb default '{}',
  created_at timestamptz not null default now()
);

-- RPC: mark_episode_watched — upserts a watched episode row
create or replace function public.mark_episode_watched(
  p_user_id uuid,
  p_tmdb_id integer,
  p_season integer,
  p_episode integer,
  p_show_trakt_id integer default null,
  p_source text default 'Streame'
)
returns void
language sql
security definer
set search_path = public, pg_catalog
as $$
  insert into public.watched_episodes (user_id, tmdb_id, season, episode, show_trakt_id, source, watched, watched_at)
  values (p_user_id, p_tmdb_id, p_season, p_episode, p_show_trakt_id, p_source, true, now())
  on conflict (user_id, tmdb_id, season, episode) do update
  set watched = true, watched_at = now(), source = p_source;
$$;

-- Auto-create profile on signup
create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = public, pg_catalog
as $$
begin
  insert into public.profiles (id, email)
  values (new.id, new.email)
  on conflict (id) do nothing;
  return new;
end;
$$;

-- Trigger: auto-create profile when a new auth user signs up
drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
  after insert on auth.users
  for each row execute function public.handle_new_user();
