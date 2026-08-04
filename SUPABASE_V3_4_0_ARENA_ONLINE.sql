-- M4X THEME v3.4.0 — ARENA ONLINE BETA
-- Chạy sau các SQL v3.0–v3.3.
--
-- Kiến trúc:
--   * Postgres: ghép trận, danh sách người thật, kết quả và thưởng M4X Coin.
--   * Supabase Realtime Broadcast: input và snapshot trận đấu.
--   * Bot chỉ được tạo trong trận bởi máy chủ phòng (host client).
--
-- Lưu ý: đây là Online Beta với host-authoritative client. Mức chống gian lận
-- tốt hơn việc tự cộng coin trong APK, nhưng chưa bằng dedicated game server.

create extension if not exists pgcrypto;

create table if not exists public.arena_matches (
  id uuid primary key default gen_random_uuid(),
  status text not null default 'waiting'
    check (status in ('waiting','playing','finished','cancelled')),
  host_user_id uuid not null references auth.users(id) on delete cascade,
  seed bigint not null default floor(random() * 2147483647)::bigint,
  max_players integer not null default 10
    check (max_players between 2 and 10),
  created_at timestamptz not null default now(),
  started_at timestamptz,
  finished_at timestamptz,
  expires_at timestamptz not null default now() + interval '12 minutes'
);

create table if not exists public.arena_match_players (
  match_id uuid not null references public.arena_matches(id) on delete cascade,
  user_id uuid not null references auth.users(id) on delete cascade,
  display_name text not null default 'M4X Hunter',
  player_slot integer not null check (player_slot between 0 and 9),
  joined_at timestamptz not null default now(),
  last_seen_at timestamptz not null default now(),
  left_at timestamptz,
  primary key (match_id, user_id)
);

alter table public.arena_match_players
  drop constraint if exists arena_match_players_match_id_player_slot_key;

create unique index if not exists idx_arena_active_player_slot
  on public.arena_match_players(match_id, player_slot)
  where left_at is null;

create table if not exists public.arena_match_rewards (
  match_id uuid not null references public.arena_matches(id) on delete cascade,
  user_id uuid not null references auth.users(id) on delete cascade,
  rank integer not null check (rank between 1 and 10),
  kills integer not null default 0 check (kills between 0 and 100),
  deaths integer not null default 0 check (deaths between 0 and 100),
  reward integer not null check (reward between 0 and 250),
  created_at timestamptz not null default now(),
  claimed_at timestamptz,
  primary key (match_id, user_id)
);

create index if not exists idx_arena_matches_waiting
  on public.arena_matches(status, created_at);

create index if not exists idx_arena_players_user
  on public.arena_match_players(user_id, joined_at desc);

create index if not exists idx_arena_rewards_user
  on public.arena_match_rewards(user_id, created_at desc);

alter table public.arena_matches enable row level security;
alter table public.arena_match_players enable row level security;
alter table public.arena_match_rewards enable row level security;

-- Không mở SELECT trực tiếp cho client.
-- Matchmaking, trạng thái phòng và phần thưởng chỉ đi qua SECURITY DEFINER RPC.
drop policy if exists arena_matches_member_read on public.arena_matches;
drop policy if exists arena_players_member_read on public.arena_match_players;
drop policy if exists arena_rewards_own_read on public.arena_match_rewards;

revoke all on public.arena_matches from public, anon, authenticated;
revoke all on public.arena_match_players from public, anon, authenticated;
revoke all on public.arena_match_rewards from public, anon, authenticated;


create or replace function public.arena_ticket_json(
  p_match_id uuid,
  p_user_id uuid
)
returns jsonb
language sql
security definer
set search_path = public
as $$
  select jsonb_build_object(
    'matchId', m.id,
    'slot', me.player_slot,
    'hostUserId', m.host_user_id,
    'status', m.status,
    'waitSeconds',
      greatest(
        0,
        8 - floor(extract(epoch from (now() - m.created_at)))::integer
      ),
    'players',
      coalesce(
        (
          select jsonb_agg(
            jsonb_build_object(
              'userId', p.user_id,
              'displayName', p.display_name,
              'slot', p.player_slot
            )
            order by p.player_slot
          )
          from public.arena_match_players p
          where p.match_id = m.id
            and p.left_at is null
        ),
        '[]'::jsonb
      )
  )
  from public.arena_matches m
  join public.arena_match_players me
    on me.match_id = m.id
   and me.user_id = p_user_id
  where m.id = p_match_id;
$$;

create or replace function public.arena_join_match(
  p_display_name text default 'M4X Hunter'
)
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
  uid uuid := auth.uid();
  chosen public.arena_matches%rowtype;
  chosen_slot integer;
  active_match uuid;
  player_count integer;
begin
  if uid is null then
    raise exception 'Bạn cần đăng nhập';
  end if;

  perform pg_advisory_xact_lock(hashtext('m4x_arena_matchmaking'));

  update public.arena_matches
     set status = 'cancelled',
         finished_at = coalesce(finished_at, now())
   where status in ('waiting','playing')
     and expires_at < now();

  select p.match_id
    into active_match
  from public.arena_match_players p
  join public.arena_matches m on m.id = p.match_id
  where p.user_id = uid
    and p.left_at is null
    and m.status in ('waiting','playing')
    and m.expires_at > now()
  order by p.joined_at desc
  limit 1;

  if active_match is not null then
    update public.arena_match_players
       set last_seen_at = now()
     where match_id = active_match and user_id = uid;

    return public.arena_ticket_json(active_match, uid);
  end if;

  select m.*
    into chosen
  from public.arena_matches m
  where m.status = 'waiting'
    and m.created_at > now() - interval '45 seconds'
    and (
      select count(*)
      from public.arena_match_players p
      where p.match_id = m.id
        and p.left_at is null
    ) < m.max_players
  order by m.created_at asc
  for update skip locked
  limit 1;

  if chosen.id is null then
    insert into public.arena_matches(host_user_id)
    values (uid)
    returning * into chosen;
  end if;

  select slot
    into chosen_slot
  from generate_series(0, 9) slot
  where not exists (
    select 1
    from public.arena_match_players p
    where p.match_id = chosen.id
      and p.player_slot = slot
      and p.left_at is null
  )
  order by slot
  limit 1;

  if chosen_slot is null then
    raise exception 'Phòng đã đầy, hãy tìm lại';
  end if;

  insert into public.arena_match_players(
    match_id, user_id, display_name, player_slot
  )
  values (
    chosen.id,
    uid,
    left(coalesce(nullif(trim(p_display_name),''),'M4X Hunter'), 28),
    chosen_slot
  )
  on conflict (match_id, user_id)
  do update set
    display_name = excluded.display_name,
    player_slot = excluded.player_slot,
    last_seen_at = now(),
    left_at = null;

  select count(*)
    into player_count
  from public.arena_match_players
  where match_id = chosen.id
    and left_at is null;

  if player_count >= chosen.max_players
     or chosen.created_at <= now() - interval '8 seconds' then
    update public.arena_matches
       set status = 'playing',
           started_at = coalesce(started_at, now()),
           expires_at = now() + interval '7 minutes'
     where id = chosen.id
       and status = 'waiting';
  end if;

  return public.arena_ticket_json(chosen.id, uid);
end;
$$;

create or replace function public.arena_match_status(
  p_match_id uuid
)
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
  uid uuid := auth.uid();
  match_row public.arena_matches%rowtype;
  player_count integer;
begin
  if uid is null then
    raise exception 'Bạn cần đăng nhập';
  end if;

  select *
    into match_row
  from public.arena_matches
  where id = p_match_id
  for update;

  if match_row.id is null then
    raise exception 'Không tìm thấy trận';
  end if;

  if not exists (
    select 1
    from public.arena_match_players
    where match_id = p_match_id
      and user_id = uid
      and left_at is null
  ) then
    raise exception 'Bạn không thuộc trận này';
  end if;

  update public.arena_match_players
     set last_seen_at = now()
   where match_id = p_match_id and user_id = uid;

  select count(*)
    into player_count
  from public.arena_match_players
  where match_id = p_match_id and left_at is null;

  if match_row.status = 'waiting'
     and (
       player_count >= match_row.max_players
       or match_row.created_at <= now() - interval '8 seconds'
     ) then
    update public.arena_matches
       set status = 'playing',
           started_at = coalesce(started_at, now()),
           expires_at = now() + interval '7 minutes'
     where id = p_match_id;
  end if;

  return public.arena_ticket_json(p_match_id, uid);
end;
$$;

create or replace function public.arena_leave_match(
  p_match_id uuid
)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
  uid uuid := auth.uid();
  match_row public.arena_matches%rowtype;
  next_host uuid;
begin
  if uid is null then
    raise exception 'Bạn cần đăng nhập';
  end if;

  select *
    into match_row
  from public.arena_matches
  where id = p_match_id
  for update;

  if match_row.id is null then
    return;
  end if;

  if match_row.status = 'waiting' then
    delete from public.arena_match_players
    where match_id = p_match_id
      and user_id = uid;
  else
    update public.arena_match_players
       set left_at = now(),
           last_seen_at = now()
     where match_id = p_match_id
       and user_id = uid
       and left_at is null;
  end if;

  if match_row.host_user_id = uid then
    if match_row.status = 'waiting' then
      select user_id
        into next_host
      from public.arena_match_players
      where match_id = p_match_id
        and left_at is null
      order by player_slot
      limit 1;

      if next_host is null then
        update public.arena_matches
           set status = 'cancelled',
               finished_at = now()
         where id = p_match_id;
      else
        update public.arena_matches
           set host_user_id = next_host
         where id = p_match_id;
      end if;
    elsif match_row.status = 'playing' then
      -- Online Beta chưa hỗ trợ chuyển host giữa trận.
      update public.arena_matches
         set status = 'cancelled',
             finished_at = now()
       where id = p_match_id;
    end if;
  end if;
end;
$$;

create or replace function public.arena_finish_match(
  p_match_id uuid,
  p_duration_seconds integer,
  p_results jsonb
)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
  uid uuid := auth.uid();
  match_row public.arena_matches%rowtype;
  p record;
  result_row jsonb;
  safe_kills integer;
  safe_deaths integer;
  computed_rank integer;
  computed_reward integer;
begin
  if uid is null then
    raise exception 'Bạn cần đăng nhập';
  end if;

  select *
    into match_row
  from public.arena_matches
  where id = p_match_id
  for update;

  if match_row.id is null then
    raise exception 'Không tìm thấy trận';
  end if;

  if match_row.host_user_id <> uid then
    raise exception 'Chỉ chủ phòng được xác nhận kết quả';
  end if;

  if match_row.status = 'finished' then
    return;
  end if;

  if match_row.status <> 'playing' then
    raise exception 'Trận chưa bắt đầu';
  end if;

  if match_row.started_at is null
     or now() - match_row.started_at < interval '45 seconds'
     or p_duration_seconds < 45 then
    raise exception 'Trận quá ngắn để nhận thưởng';
  end if;

  if jsonb_typeof(p_results) <> 'array' then
    raise exception 'Kết quả không hợp lệ';
  end if;

  -- Xóa kết quả cũ nếu một lần finish trước bị ngắt giữa chừng.
  delete from public.arena_match_rewards
  where match_id = p_match_id and claimed_at is null;

  for p in
    select *
    from public.arena_match_players
    where match_id = p_match_id
      and left_at is null
  loop
    select value
      into result_row
    from jsonb_array_elements(p_results)
    where value->>'userId' = p.user_id::text
    limit 1;

    safe_kills := least(
      100,
      greatest(0, coalesce((result_row->>'kills')::integer, 0))
    );
    safe_deaths := least(
      100,
      greatest(0, coalesce((result_row->>'deaths')::integer, 0))
    );

    select 1 + count(*)
      into computed_rank
    from jsonb_array_elements(p_results) rival
    where least(
            100,
            greatest(0, coalesce((rival->>'kills')::integer, 0))
          ) > safe_kills
       or (
            least(
              100,
              greatest(0, coalesce((rival->>'kills')::integer, 0))
            ) = safe_kills
            and least(
                  100,
                  greatest(0, coalesce((rival->>'deaths')::integer, 0))
                ) < safe_deaths
          );

    computed_rank := least(10, greatest(1, computed_rank));

    computed_reward :=
      35
      + least(60, safe_kills * 3)
      + case
          when computed_rank = 1 then 100
          when computed_rank in (2,3) then 60
          when computed_rank between 4 and 6 then 25
          else 0
        end;

    computed_reward := least(200, greatest(0, computed_reward));

    -- Giới hạn tối đa 6 trận thưởng trong 1 giờ cho mỗi tài khoản.
    if (
      select count(*)
      from public.arena_match_rewards r
      where r.user_id = p.user_id
        and r.created_at > now() - interval '1 hour'
    ) >= 6 then
      computed_reward := 0;
    end if;

    insert into public.arena_match_rewards(
      match_id, user_id, rank, kills, deaths, reward
    )
    values (
      p_match_id,
      p.user_id,
      computed_rank,
      safe_kills,
      safe_deaths,
      computed_reward
    )
    on conflict (match_id, user_id)
    do update set
      rank = excluded.rank,
      kills = excluded.kills,
      deaths = excluded.deaths,
      reward = excluded.reward;
  end loop;

  update public.arena_matches
     set status = 'finished',
         finished_at = now(),
         expires_at = now() + interval '10 minutes'
   where id = p_match_id;
end;
$$;

create or replace function public.arena_claim_reward(
  p_match_id uuid
)
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
  uid uuid := auth.uid();
  reward_row public.arena_match_rewards%rowtype;
  new_balance bigint;
begin
  if uid is null then
    raise exception 'Bạn cần đăng nhập';
  end if;

  select *
    into reward_row
  from public.arena_match_rewards
  where match_id = p_match_id
    and user_id = uid
  for update;

  if reward_row.match_id is null then
    raise exception 'Kết quả chưa được chủ phòng xác nhận';
  end if;

  if reward_row.claimed_at is null then
    update public.arena_match_rewards
       set claimed_at = now()
     where match_id = p_match_id and user_id = uid;

    if reward_row.reward > 0 then
      update public.profiles
         set points = points + reward_row.reward
       where id = uid
       returning points into new_balance;

      insert into public.coin_transactions(
        user_id, amount, reason, ref_id
      )
      values (
        uid,
        reward_row.reward,
        'M4X Arena Online',
        p_match_id::text
      );
    else
      select points into new_balance
      from public.profiles where id = uid;
    end if;
  else
    select points into new_balance
    from public.profiles where id = uid;
  end if;

  return jsonb_build_object(
    'reward', reward_row.reward,
    'balance', coalesce(new_balance, 0),
    'message',
      case
        when reward_row.claimed_at is not null
          then 'Phần thưởng trận này đã được nhận'
        when reward_row.reward > 0
          then 'Đã cộng ' || reward_row.reward || ' M4X Coin'
        else 'Đã đạt giới hạn thưởng theo giờ'
      end
  );
end;
$$;

revoke all on function public.arena_ticket_json(uuid, uuid)
  from public, anon, authenticated;
revoke all on function public.arena_join_match(text)
  from public, anon, authenticated;
revoke all on function public.arena_match_status(uuid)
  from public, anon, authenticated;
revoke all on function public.arena_leave_match(uuid)
  from public, anon, authenticated;
revoke all on function public.arena_finish_match(uuid, integer, jsonb)
  from public, anon, authenticated;
revoke all on function public.arena_claim_reward(uuid)
  from public, anon, authenticated;

grant execute on function public.arena_join_match(text) to authenticated;
grant execute on function public.arena_match_status(uuid) to authenticated;
grant execute on function public.arena_leave_match(uuid) to authenticated;
grant execute on function public.arena_finish_match(uuid, integer, jsonb) to authenticated;
grant execute on function public.arena_claim_reward(uuid) to authenticated;

notify pgrst, 'reload schema';
