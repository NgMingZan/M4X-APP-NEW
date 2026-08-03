-- ============================================================
-- M4X THEME v3.1.0
-- Né chướng ngại • Mê cung • Bản đồ kho báu • Thú cưng M4X
-- Không xóa dữ liệu cũ.
-- Yêu cầu các bảng/hàm v3 trước đó đã tồn tại: profiles, add_coin,
-- shop_items, user_inventory, theme_purchases, minigame_plays,
-- app_usage_daily.
-- ============================================================

begin;

create extension if not exists pgcrypto;

-- ---------- 1) Arcade và mê cung ----------
create table if not exists public.arcade_daily_rewards (
  user_id uuid not null references public.profiles(id) on delete cascade,
  reward_date date not null,
  game_code text not null,
  reward_total integer not null default 0,
  plays integer not null default 0,
  primary key(user_id, reward_date, game_code)
);

create table if not exists public.maze_sessions (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references public.profiles(id) on delete cascade,
  difficulty text not null check (difficulty in ('easy','medium','hard')),
  seed bigint not null,
  fee integer not null default 50,
  reward integer not null default 0,
  started_at timestamptz not null default now(),
  finished_at timestamptz,
  finished boolean not null default false
);

-- ---------- 2) Bản đồ kho báu ----------
create table if not exists public.treasure_actions (
  user_id uuid not null references public.profiles(id) on delete cascade,
  action_date date not null,
  action_code text not null,
  amount integer not null default 0,
  primary key(user_id, action_date, action_code)
);

create table if not exists public.treasure_action_events (
  user_id uuid not null references public.profiles(id) on delete cascade,
  action_date date not null,
  action_code text not null,
  ref_id text not null,
  created_at timestamptz not null default now(),
  primary key(user_id, action_date, action_code, ref_id)
);

create table if not exists public.treasure_weeks (
  user_id uuid not null references public.profiles(id) on delete cascade,
  week_start date not null,
  opened_days integer[] not null default '{}',
  keys_count integer not null default 0,
  secret_day integer not null check(secret_day between 1 and 7),
  secret_claimed boolean not null default false,
  bronze_claimed boolean not null default false,
  silver_claimed boolean not null default false,
  gold_claimed boolean not null default false,
  gold_claimed_at timestamptz,
  streak_weeks integer not null default 0,
  rescue_used boolean not null default false,
  boss_energy integer not null default 0,
  share_code text,
  created_at timestamptz not null default now(),
  primary key(user_id, week_start)
);

create table if not exists public.treasure_chest_claims (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references public.profiles(id) on delete cascade,
  week_start date not null,
  chest_type text not null check(chest_type in ('bronze','silver','gold')),
  reward integer not null,
  claimed_at timestamptz not null default now(),
  unique(user_id, week_start, chest_type)
);

create table if not exists public.treasure_bosses (
  week_start date primary key,
  name text not null default 'Cướp biển Bóng Đêm',
  max_hp integer not null default 5000,
  hp integer not null default 5000,
  defeated_at timestamptz,
  created_at timestamptz not null default now()
);

create table if not exists public.treasure_boss_participants (
  user_id uuid not null references public.profiles(id) on delete cascade,
  week_start date not null references public.treasure_bosses(week_start) on delete cascade,
  damage integer not null default 0,
  reward integer not null default 0,
  rewarded boolean not null default false,
  primary key(user_id, week_start)
);

create table if not exists public.treasure_share_codes (
  code text primary key,
  owner_id uuid not null references public.profiles(id) on delete cascade,
  week_start date not null,
  reward integer not null check(reward between 10 and 30),
  max_uses integer not null default 5,
  used_count integer not null default 0,
  expires_at timestamptz not null,
  active boolean not null default true
);

create table if not exists public.treasure_share_claims (
  code text not null references public.treasure_share_codes(code) on delete cascade,
  user_id uuid not null references public.profiles(id) on delete cascade,
  claimed_at timestamptz not null default now(),
  primary key(code, user_id)
);

create table if not exists public.treasure_seasons (
  id uuid primary key default gen_random_uuid(),
  name text not null,
  event_code text not null unique,
  starts_at timestamptz not null,
  ends_at timestamptz not null,
  reward_item_name text not null,
  reward_item_type text not null default 'badge',
  active boolean not null default true
);

create table if not exists public.treasure_top_rewards (
  week_start date not null,
  user_id uuid not null references public.profiles(id) on delete cascade,
  rank integer not null,
  opened_count integer not null,
  reward integer not null,
  rewarded_at timestamptz not null default now(),
  primary key(week_start, user_id),
  unique(week_start, rank)
);

-- ---------- 3) Thú cưng ----------
create table if not exists public.m4x_pets (
  user_id uuid primary key references public.profiles(id) on delete cascade,
  pet_name text not null default 'M4X Nova',
  pet_type text not null default 'nova',
  level integer not null default 1,
  xp integer not null default 0,
  hunger integer not null default 80 check(hunger between 0 and 100),
  food integer not null default 3,
  last_fed_at timestamptz,
  updated_at timestamptz not null default now()
);

-- ---------- 4) Hàm trợ giúp nội bộ ----------
create or replace function public.m4x_vn_date()
returns date language sql stable as $$
  select timezone('Asia/Ho_Chi_Minh', now())::date
$$;

create or replace function public.m4x_week_start()
returns date language sql stable as $$
  select date_trunc('week', timezone('Asia/Ho_Chi_Minh', now()))::date
$$;

create or replace function public.m4x_add_treasure_action(
  p_user uuid,
  p_action text,
  p_amount integer,
  p_ref text default ''
)
returns void language plpgsql security definer set search_path=public as $$
declare d date := public.m4x_vn_date();
begin
  if p_user is null or p_amount <= 0 then return; end if;
  if p_ref <> '' then
    insert into public.treasure_action_events(user_id,action_date,action_code,ref_id)
    values(p_user,d,p_action,p_ref)
    on conflict do nothing;
    if not found then return; end if;
  end if;
  insert into public.treasure_actions(user_id,action_date,action_code,amount)
  values(p_user,d,p_action,p_amount)
  on conflict(user_id,action_date,action_code) do update
  set amount=public.treasure_actions.amount+excluded.amount;
end $$;

create or replace function public.m4x_grant_pet_food(p_user uuid,p_amount integer default 1)
returns void language plpgsql security definer set search_path=public as $$
begin
  if p_user is null or p_amount <= 0 then return; end if;
  insert into public.m4x_pets(user_id,food)
  values(p_user,least(20,3+p_amount))
  on conflict(user_id) do update
  set food=least(20,public.m4x_pets.food+p_amount),updated_at=now();
end $$;

create or replace function public.m4x_ensure_treasure_week(p_user uuid)
returns public.treasure_weeks language plpgsql security definer set search_path=public as $$
declare w date := public.m4x_week_start(); r public.treasure_weeks%rowtype; prev_streak integer := 0;
begin
  select streak_weeks into prev_streak from public.treasure_weeks
  where user_id=p_user and week_start=w-7 and gold_claimed=true;
  insert into public.treasure_weeks(user_id,week_start,secret_day,streak_weeks)
  values(p_user,w,(floor(random()*7)+1)::integer,coalesce(prev_streak,0))
  on conflict(user_id,week_start) do nothing;
  insert into public.treasure_bosses(week_start) values(w) on conflict do nothing;
  select * into r from public.treasure_weeks where user_id=p_user and week_start=w;
  return r;
end $$;

-- ---------- 5) Trigger tự ghi tiến độ ----------
create or replace function public.trg_m4x_minigame_progress()
returns trigger language plpgsql security definer set search_path=public as $$
begin
  perform public.m4x_add_treasure_action(new.user_id,'play_minigame',1,new.id::text);
  perform public.m4x_grant_pet_food(new.user_id,1);
  return new;
end $$;

drop trigger if exists trg_m4x_minigame_progress on public.minigame_plays;
create trigger trg_m4x_minigame_progress after insert on public.minigame_plays
for each row execute function public.trg_m4x_minigame_progress();

create or replace function public.trg_m4x_purchase_progress()
returns trigger language plpgsql security definer set search_path=public as $$
begin
  perform public.m4x_add_treasure_action(new.user_id,'download_theme',1,new.theme_id::text);
  return new;
end $$;

drop trigger if exists trg_m4x_purchase_progress on public.theme_purchases;
create trigger trg_m4x_purchase_progress after insert on public.theme_purchases
for each row execute function public.trg_m4x_purchase_progress();

create or replace function public.trg_m4x_usage_progress()
returns trigger language plpgsql security definer set search_path=public as $$
declare delta_minutes integer; old_minutes integer:=0; food_gain integer:=0;
begin
  if tg_op='INSERT' then
    delta_minutes:=new.active_minutes;
  else
    old_minutes:=old.active_minutes;
    delta_minutes:=greatest(0,new.active_minutes-old.active_minutes);
  end if;
  if delta_minutes>0 then
    insert into public.treasure_actions(user_id,action_date,action_code,amount)
    values(new.user_id,public.m4x_vn_date(),'app_minutes',delta_minutes)
    on conflict(user_id,action_date,action_code) do update
    set amount=public.treasure_actions.amount+excluded.amount;
    food_gain:=greatest(0,floor(new.active_minutes/10.0)::integer-floor(old_minutes/10.0)::integer);
    if food_gain>0 then perform public.m4x_grant_pet_food(new.user_id,food_gain); end if;
  end if;
  return new;
end $$;

drop trigger if exists trg_m4x_usage_progress on public.app_usage_daily;
create trigger trg_m4x_usage_progress after insert or update on public.app_usage_daily
for each row execute function public.trg_m4x_usage_progress();

-- ---------- 6) Ghi lượt xem theme ----------
create or replace function public.record_theme_view(p_theme_id uuid)
returns void language plpgsql security definer set search_path=public as $$
begin
  if auth.uid() is null then raise exception 'Chưa đăng nhập'; end if;
  if not exists(select 1 from public.themes where id=p_theme_id and status='approved') then
    raise exception 'Theme không tồn tại';
  end if;
  perform public.m4x_add_treasure_action(auth.uid(),'view_theme',1,p_theme_id::text);
end $$;

-- ---------- 7) Né chướng ngại ----------
create or replace function public.claim_obstacle_reward(p_score integer)
returns jsonb language plpgsql security definer set search_path=public as $$
declare d date:=public.m4x_vn_date(); total integer; reward_amount integer; new_balance bigint;
begin
  if auth.uid() is null then raise exception 'Chưa đăng nhập'; end if;
  if p_score<0 or p_score>100000 then raise exception 'Điểm không hợp lệ'; end if;
  select coalesce(reward_total,0) into total from public.arcade_daily_rewards
  where user_id=auth.uid() and reward_date=d and game_code='obstacle' for update;
  total:=coalesce(total,0);
  reward_amount:=least(greatest(floor(p_score/5.0)::integer,0),greatest(200-total,0));
  insert into public.arcade_daily_rewards(user_id,reward_date,game_code,reward_total,plays)
  values(auth.uid(),d,'obstacle',reward_amount,1)
  on conflict(user_id,reward_date,game_code) do update
  set reward_total=public.arcade_daily_rewards.reward_total+excluded.reward_total,
      plays=public.arcade_daily_rewards.plays+1;
  if reward_amount>0 then perform public.add_coin(auth.uid(),reward_amount,'obstacle_game',d::text); end if;
  perform public.m4x_add_treasure_action(auth.uid(),'play_minigame',1,gen_random_uuid()::text);
  perform public.m4x_grant_pet_food(auth.uid(),1);
  select points into new_balance from public.profiles where id=auth.uid();
  return jsonb_build_object('reward',reward_amount,'balance',new_balance,'daily_total',total+reward_amount,
    'message',case when reward_amount>0 then 'Điểm càng cao, thưởng càng lớn' else 'Bạn đã đạt giới hạn 200 coin hôm nay' end);
end $$;

-- ---------- 8) Mê cung ----------
create or replace function public.start_maze_game(p_difficulty text)
returns jsonb language plpgsql security definer set search_path=public as $$
declare s public.maze_sessions%rowtype; b bigint; min_seconds integer;
begin
  if auth.uid() is null then raise exception 'Chưa đăng nhập'; end if;
  if p_difficulty not in ('easy','medium','hard') then raise exception 'Độ khó không hợp lệ'; end if;
  select points into b from public.profiles where id=auth.uid() for update;
  if b<50 then raise exception 'Bạn cần 50 M4X COIN để chơi'; end if;
  perform public.add_coin(auth.uid(),-50,'maze_entry',p_difficulty);
  insert into public.maze_sessions(user_id,difficulty,seed)
  values(auth.uid(),p_difficulty,(floor(random()*2147483000)+1)::bigint)
  returning * into s;
  min_seconds:=case p_difficulty when 'easy' then 4 when 'medium' then 8 else 12 end;
  select points into b from public.profiles where id=auth.uid();
  return jsonb_build_object('session_id',s.id,'difficulty',s.difficulty,'seed',s.seed,'fee',50,'balance',b,'min_seconds',min_seconds);
end $$;

create or replace function public.finish_maze_game(p_session_id uuid)
returns jsonb language plpgsql security definer set search_path=public as $$
declare s public.maze_sessions%rowtype; reward_amount integer; min_seconds integer; b bigint;
begin
  if auth.uid() is null then raise exception 'Chưa đăng nhập'; end if;
  select * into s from public.maze_sessions where id=p_session_id and user_id=auth.uid() for update;
  if s.id is null then raise exception 'Không tìm thấy lượt mê cung'; end if;
  if s.finished then raise exception 'Lượt này đã nhận thưởng'; end if;
  min_seconds:=case s.difficulty when 'easy' then 4 when 'medium' then 8 else 12 end;
  if extract(epoch from (now()-s.started_at))<min_seconds then raise exception 'Hoàn thành quá nhanh, hãy đi đúng đường mê cung'; end if;
  reward_amount:=case s.difficulty when 'easy' then 30 when 'medium' then 100 else 200 end;
  update public.maze_sessions set finished=true,finished_at=now(),reward=reward_amount where id=s.id;
  perform public.add_coin(auth.uid(),reward_amount,'maze_reward',s.id::text);
  perform public.m4x_add_treasure_action(auth.uid(),'play_minigame',1,s.id::text);
  perform public.m4x_grant_pet_food(auth.uid(),1);
  select points into b from public.profiles where id=auth.uid();
  return jsonb_build_object('reward',reward_amount,'balance',b,'message','Hoàn thành mê cung '||s.difficulty);
end $$;

-- ---------- 9) Trạng thái bản đồ ----------
create or replace function public.get_treasure_state()
returns jsonb language plpgsql security definer set search_path=public as $$
declare w public.treasure_weeks%rowtype; d date:=public.m4x_vn_date(); day_no integer; task_code text; task_title text; target integer; progress integer; boss public.treasure_bosses%rowtype; season jsonb; leaders jsonb;
begin
  if auth.uid() is null then raise exception 'Chưa đăng nhập'; end if;
  select * into w from public.m4x_ensure_treasure_week(auth.uid());
  day_no:=extract(isodow from timezone('Asia/Ho_Chi_Minh',now()))::integer;
  task_code:=case day_no when 1 then 'view_theme' when 2 then 'download_theme' when 3 then 'app_minutes' when 4 then 'play_minigame' when 5 then 'view_theme' when 6 then 'download_theme' else 'play_minigame' end;
  task_title:=case task_code when 'view_theme' then 'Xem 5 theme' when 'download_theme' then 'Tải 1 theme' when 'app_minutes' then 'Dùng app 10 phút' else 'Chơi 2 minigame' end;
  target:=case task_code when 'view_theme' then 5 when 'download_theme' then 1 when 'app_minutes' then 10 else 2 end;
  select coalesce(amount,0) into progress from public.treasure_actions where user_id=auth.uid() and action_date=d and action_code=task_code;
  select * into boss from public.treasure_bosses where week_start=w.week_start;
  select jsonb_build_object('name',name,'event_code',event_code,'reward_item',reward_item_name)
    into season from public.treasure_seasons where active=true and now() between starts_at and ends_at order by starts_at desc limit 1;
  select coalesce(jsonb_agg(x order by x.rank),'[]'::jsonb) into leaders from (
    select row_number() over(order by cardinality(tw.opened_days) desc,tw.gold_claimed_at asc nulls last,p.display_name)::integer rank,
      p.display_name,cardinality(tw.opened_days) opened_count
    from public.treasure_weeks tw join public.profiles p on p.id=tw.user_id
    where tw.week_start=w.week_start order by cardinality(tw.opened_days) desc limit 10
  ) x;
  return jsonb_build_object(
    'week_start',w.week_start,'day',day_no,'opened_days',to_jsonb(w.opened_days),'keys',w.keys_count,
    'secret_day',case when w.secret_claimed then w.secret_day else 0 end,'secret_claimed',w.secret_claimed,
    'bronze_claimed',w.bronze_claimed,'silver_claimed',w.silver_claimed,'gold_claimed',w.gold_claimed,
    'streak_weeks',w.streak_weeks,'rescue_used',w.rescue_used,'boss_energy',w.boss_energy,
    'share_code',coalesce(w.share_code,''),'task_code',task_code,'task_title',task_title,'task_progress',coalesce(progress,0),'task_target',target,
    'boss_name',boss.name,'boss_hp',boss.hp,'boss_max_hp',boss.max_hp,'season',coalesce(season,'{}'::jsonb),'leaders',leaders
  );
end $$;

create or replace function public.claim_treasure_day()
returns jsonb language plpgsql security definer set search_path=public as $$
declare w public.treasure_weeks%rowtype; d date:=public.m4x_vn_date(); day_no integer; task_code text; target integer; progress integer; bonus integer:=0; rare boolean:=false; b bigint;
begin
  if auth.uid() is null then raise exception 'Chưa đăng nhập'; end if;
  select * into w from public.m4x_ensure_treasure_week(auth.uid());
  day_no:=extract(isodow from timezone('Asia/Ho_Chi_Minh',now()))::integer;
  if day_no=any(w.opened_days) then raise exception 'Ô hôm nay đã mở'; end if;
  task_code:=case day_no when 1 then 'view_theme' when 2 then 'download_theme' when 3 then 'app_minutes' when 4 then 'play_minigame' when 5 then 'view_theme' when 6 then 'download_theme' else 'play_minigame' end;
  target:=case task_code when 'view_theme' then 5 when 'download_theme' then 1 when 'app_minutes' then 10 else 2 end;
  select coalesce(amount,0) into progress from public.treasure_actions where user_id=auth.uid() and action_date=d and action_code=task_code;
  if coalesce(progress,0)<target then raise exception 'Bạn chưa hoàn thành nhiệm vụ hôm nay'; end if;
  if day_no=w.secret_day then
    if random()<0.80 then
      bonus:=(floor(random()*101)+50)::integer;
      perform public.add_coin(auth.uid(),bonus,'treasure_secret',w.week_start::text);
    else
      rare:=true;
      insert into public.user_inventory(user_id,item_name,item_type,equipped,item_metadata)
      values(auth.uid(),'Mảnh bản đồ Huyền thoại','badge',false,'{"badge":"treasure_legend"}'::jsonb);
    end if;
  end if;
  update public.treasure_weeks set opened_days=array_append(opened_days,day_no),keys_count=keys_count+1,
    secret_claimed=secret_claimed or day_no=secret_day,boss_energy=boss_energy+1 where user_id=auth.uid() and week_start=w.week_start;
  perform public.m4x_grant_pet_food(auth.uid(),1);
  select points into b from public.profiles where id=auth.uid();
  return jsonb_build_object('opened_day',day_no,'secret',day_no=w.secret_day,'bonus',bonus,'rare_item',rare,'balance',b);
end $$;

create or replace function public.claim_treasure_chest(p_chest_type text)
returns jsonb language plpgsql security definer set search_path=public as $$
declare w public.treasure_weeks%rowtype; need integer; reward_amount integer; streak_bonus integer:=0; b bigint; code text:=''; season_row public.treasure_seasons%rowtype;
begin
  if auth.uid() is null then raise exception 'Chưa đăng nhập'; end if;
  select * into w from public.m4x_ensure_treasure_week(auth.uid());
  if p_chest_type='bronze' then need:=3; if w.bronze_claimed then raise exception 'Rương Đồng đã nhận'; end if; reward_amount:=(floor(random()*51)+30)::integer;
  elsif p_chest_type='silver' then need:=5; if w.silver_claimed then raise exception 'Rương Bạc đã nhận'; end if; reward_amount:=(floor(random()*131)+70)::integer;
  elsif p_chest_type='gold' then need:=7; if w.gold_claimed then raise exception 'Rương Vàng đã nhận'; end if; reward_amount:=(floor(random()*401)+100)::integer;
  else raise exception 'Loại rương không hợp lệ'; end if;
  if cardinality(w.opened_days)<need then raise exception 'Chưa mở đủ ô bản đồ'; end if;
  perform public.add_coin(auth.uid(),reward_amount,'treasure_'||p_chest_type,w.week_start::text);
  if p_chest_type='bronze' then update public.treasure_weeks set bronze_claimed=true where user_id=auth.uid() and week_start=w.week_start;
  elsif p_chest_type='silver' then update public.treasure_weeks set silver_claimed=true where user_id=auth.uid() and week_start=w.week_start;
  else
    streak_bonus:=case least(w.streak_weeks+1,4) when 1 then 50 when 2 then 100 when 3 then 200 else 0 end;
    if streak_bonus>0 then perform public.add_coin(auth.uid(),streak_bonus,'treasure_streak',w.week_start::text); end if;
    if w.streak_weeks+1>=4 and not exists(
      select 1 from public.user_inventory where user_id=auth.uid() and item_name='Huy hiệu Thợ săn 4 tuần'
    ) then
      insert into public.user_inventory(user_id,item_name,item_type,equipped,item_metadata)
      values(auth.uid(),'Huy hiệu Thợ săn 4 tuần','badge',false,'{"badge":"hunter_4_week"}'::jsonb);
    end if;
    select * into season_row from public.treasure_seasons where active=true and now() between starts_at and ends_at order by starts_at desc limit 1;
    if season_row.id is not null and not exists(select 1 from public.user_inventory where user_id=auth.uid() and item_name=season_row.reward_item_name) then
      insert into public.user_inventory(user_id,item_name,item_type,equipped,item_metadata)
      values(auth.uid(),season_row.reward_item_name,season_row.reward_item_type,false,jsonb_build_object('season',season_row.event_code));
    end if;
    code:='M4X'||upper(substr(replace(gen_random_uuid()::text,'-',''),1,8));
    insert into public.treasure_share_codes(code,owner_id,week_start,reward,max_uses,expires_at)
    values(code,auth.uid(),w.week_start,(floor(random()*21)+10)::integer,5,now()+interval '7 days');
    update public.treasure_weeks set gold_claimed=true,gold_claimed_at=now(),streak_weeks=least(streak_weeks+1,4),share_code=code where user_id=auth.uid() and week_start=w.week_start;
  end if;
  insert into public.treasure_chest_claims(user_id,week_start,chest_type,reward) values(auth.uid(),w.week_start,p_chest_type,reward_amount);
  select points into b from public.profiles where id=auth.uid();
  return jsonb_build_object('reward',reward_amount,'streak_bonus',streak_bonus,'balance',b,'share_code',code);
end $$;

create or replace function public.use_treasure_rescue_card()
returns jsonb language plpgsql security definer set search_path=public as $$
declare w public.treasure_weeks%rowtype; card_id uuid; today_no integer; missed integer; b bigint;
begin
  if auth.uid() is null then raise exception 'Chưa đăng nhập'; end if;
  select * into w from public.m4x_ensure_treasure_week(auth.uid());
  if w.rescue_used then raise exception 'Tuần này bạn đã dùng Thẻ cứu ngày'; end if;
  select id into card_id from public.user_inventory where user_id=auth.uid() and item_type='rescue_card' order by acquired_at limit 1 for update;
  if card_id is null then raise exception 'Bạn chưa có Thẻ cứu ngày'; end if;
  today_no:=extract(isodow from timezone('Asia/Ho_Chi_Minh',now()))::integer;
  select max(x) into missed from generate_series(1,today_no-1) x where not (x=any(w.opened_days));
  if missed is null then raise exception 'Không có ngày bỏ lỡ để cứu'; end if;
  delete from public.user_inventory where id=card_id;
  update public.treasure_weeks set opened_days=array_append(opened_days,missed),keys_count=keys_count+1,rescue_used=true where user_id=auth.uid() and week_start=w.week_start;
  select points into b from public.profiles where id=auth.uid();
  return jsonb_build_object('rescued_day',missed,'balance',b);
end $$;

create or replace function public.attack_treasure_boss()
returns jsonb language plpgsql security definer set search_path=public as $$
declare w public.treasure_weeks%rowtype; boss public.treasure_bosses%rowtype; damage_amount integer; reward_amount integer; p record; b bigint;
begin
  if auth.uid() is null then raise exception 'Chưa đăng nhập'; end if;
  select * into w from public.m4x_ensure_treasure_week(auth.uid());
  if w.boss_energy<=0 then raise exception 'Hãy hoàn thành nhiệm vụ để nhận năng lượng đánh Boss'; end if;
  select * into boss from public.treasure_bosses where week_start=w.week_start for update;
  if boss.hp<=0 then raise exception 'Boss đã bị hạ'; end if;
  damage_amount:=(floor(random()*101)+100)::integer;
  damage_amount:=least(damage_amount,boss.hp);
  update public.treasure_weeks set boss_energy=boss_energy-1 where user_id=auth.uid() and week_start=w.week_start;
  update public.treasure_bosses set hp=hp-damage_amount,defeated_at=case when hp-damage_amount<=0 then now() else defeated_at end where week_start=w.week_start returning * into boss;
  insert into public.treasure_boss_participants(user_id,week_start,damage) values(auth.uid(),w.week_start,damage_amount)
  on conflict(user_id,week_start) do update set damage=public.treasure_boss_participants.damage+excluded.damage;
  if boss.hp<=0 then
    for p in select user_id from public.treasure_boss_participants where week_start=w.week_start and rewarded=false loop
      reward_amount:=(floor(random()*251)+50)::integer;
      perform public.add_coin(p.user_id,reward_amount,'treasure_boss',w.week_start::text);
      update public.treasure_boss_participants set rewarded=true,reward=reward_amount where user_id=p.user_id and week_start=w.week_start;
    end loop;
  end if;
  select points into b from public.profiles where id=auth.uid();
  return jsonb_build_object('damage',damage_amount,'boss_hp',boss.hp,'boss_max_hp',boss.max_hp,'defeated',boss.hp<=0,'balance',b);
end $$;

create or replace function public.redeem_treasure_share_code(p_code text)
returns jsonb language plpgsql security definer set search_path=public as $$
declare c public.treasure_share_codes%rowtype; b bigint;
begin
  if auth.uid() is null then raise exception 'Chưa đăng nhập'; end if;
  select * into c from public.treasure_share_codes where upper(code)=upper(trim(p_code)) and active=true and used_count<max_uses and expires_at>now() for update;
  if c.code is null then raise exception 'Mã kho báu không hợp lệ hoặc đã hết lượt'; end if;
  if c.owner_id=auth.uid() then raise exception 'Không thể dùng mã của chính mình'; end if;
  insert into public.treasure_share_claims(code,user_id) values(c.code,auth.uid());
  update public.treasure_share_codes set used_count=used_count+1,active=(used_count+1<max_uses) where code=c.code;
  perform public.add_coin(auth.uid(),c.reward,'treasure_share',c.code);
  select points into b from public.profiles where id=auth.uid();
  return jsonb_build_object('reward',c.reward,'balance',b);
end $$;

-- ---------- 10) Thú cưng ----------
create or replace function public.get_m4x_pet()
returns jsonb language plpgsql security definer set search_path=public as $$
declare p public.m4x_pets%rowtype; b bigint; decay integer;
begin
  if auth.uid() is null then raise exception 'Chưa đăng nhập'; end if;
  insert into public.m4x_pets(user_id) values(auth.uid()) on conflict do nothing;
  select * into p from public.m4x_pets where user_id=auth.uid() for update;
  decay:=least(60,greatest(0,floor(extract(epoch from (now()-p.updated_at))/21600.0)::integer*5));
  if decay>0 then update public.m4x_pets set hunger=greatest(0,hunger-decay),updated_at=now() where user_id=auth.uid() returning * into p; end if;
  select points into b from public.profiles where id=auth.uid();
  return jsonb_build_object('name',p.pet_name,'type',p.pet_type,'level',p.level,'xp',p.xp,'xp_target',p.level*100,'hunger',p.hunger,'food',p.food,'balance',b);
end $$;

create or replace function public.feed_m4x_pet()
returns jsonb language plpgsql security definer set search_path=public as $$
declare p public.m4x_pets%rowtype; target integer; level_reward integer:=0; b bigint;
begin
  if auth.uid() is null then raise exception 'Chưa đăng nhập'; end if;
  insert into public.m4x_pets(user_id) values(auth.uid()) on conflict do nothing;
  select * into p from public.m4x_pets where user_id=auth.uid() for update;
  if p.food<=0 then raise exception 'Hết thức ăn. Hãy chơi minigame hoặc mở ô kho báu'; end if;
  update public.m4x_pets set food=food-1,hunger=least(100,hunger+20),xp=xp+25,last_fed_at=now(),updated_at=now() where user_id=auth.uid() returning * into p;
  target:=p.level*100;
  if p.xp>=target then
    p.level:=p.level+1; p.xp:=p.xp-target;
    level_reward:=least(300,50+(p.level-2)*25);
    update public.m4x_pets set level=p.level,xp=p.xp where user_id=auth.uid() returning * into p;
    perform public.add_coin(auth.uid(),level_reward,'pet_level_up',p.level::text);
  end if;
  select points into b from public.profiles where id=auth.uid();
  return jsonb_build_object('name',p.pet_name,'type',p.pet_type,'level',p.level,'xp',p.xp,'xp_target',p.level*100,'hunger',p.hunger,'food',p.food,'level_reward',level_reward,'balance',b);
end $$;

-- ---------- 11) Top thợ săn tự động ----------
create or replace function public.distribute_treasure_top_rewards(force_run boolean default false)
returns jsonb language plpgsql security definer set search_path=public as $$
declare w date:=public.m4x_week_start(); r record; reward_amount integer; count_rewarded integer:=0;
begin
  if not force_run and extract(dow from timezone('Asia/Ho_Chi_Minh',now()))<>0 then return jsonb_build_object('status','skipped'); end if;
  if exists(select 1 from public.treasure_top_rewards where week_start=w) then return jsonb_build_object('status','already_distributed'); end if;
  for r in select * from (
    select tw.user_id,cardinality(tw.opened_days) opened_count,row_number() over(order by cardinality(tw.opened_days) desc,tw.gold_claimed_at asc nulls last,tw.user_id)::integer rank
    from public.treasure_weeks tw where tw.week_start=w and cardinality(tw.opened_days)>0
  ) z where rank<=10 order by rank loop
    reward_amount:=case r.rank when 1 then 1000 when 2 then 700 when 3 then 500 else 200 end;
    perform public.add_coin(r.user_id,reward_amount,'treasure_top_'||r.rank,w::text);
    insert into public.treasure_top_rewards values(w,r.user_id,r.rank,r.opened_count,reward_amount,now());
    count_rewarded:=count_rewarded+1;
  end loop;
  return jsonb_build_object('status','distributed','rewarded',count_rewarded);
end $$;

-- ---------- 12) Dữ liệu mặc định ----------
insert into public.shop_items(name,item_type,price,image_url,limited,active,metadata)
select 'Thẻ cứu ngày','rescue_card',100,'',false,true,'{"use":"treasure_rescue"}'::jsonb
where not exists(select 1 from public.shop_items where name='Thẻ cứu ngày');

insert into public.treasure_seasons(name,event_code,starts_at,ends_at,reward_item_name,reward_item_type,active)
select 'Mùa HyperOS','hyperos_2026',now()-interval '1 day',now()+interval '90 days','Huy hiệu Thợ săn HyperOS','badge',true
where not exists(select 1 from public.treasure_seasons where event_code='hyperos_2026');

-- ---------- 13) RLS ----------
alter table public.arcade_daily_rewards enable row level security;
alter table public.maze_sessions enable row level security;
alter table public.treasure_actions enable row level security;
alter table public.treasure_action_events enable row level security;
alter table public.treasure_weeks enable row level security;
alter table public.treasure_chest_claims enable row level security;
alter table public.treasure_bosses enable row level security;
alter table public.treasure_boss_participants enable row level security;
alter table public.treasure_share_codes enable row level security;
alter table public.treasure_share_claims enable row level security;
alter table public.treasure_seasons enable row level security;
alter table public.treasure_top_rewards enable row level security;
alter table public.m4x_pets enable row level security;

do $$ declare t text; p record; begin
  foreach t in array array['arcade_daily_rewards','maze_sessions','treasure_actions','treasure_action_events','treasure_weeks','treasure_chest_claims','treasure_boss_participants','treasure_share_claims','m4x_pets'] loop
    for p in select policyname from pg_policies where schemaname='public' and tablename=t loop execute format('drop policy if exists %I on public.%I',p.policyname,t); end loop;
  end loop;
end $$;

drop policy if exists boss_read on public.treasure_bosses;
drop policy if exists share_codes_read on public.treasure_share_codes;
drop policy if exists seasons_read on public.treasure_seasons;
drop policy if exists treasure_top_read on public.treasure_top_rewards;

create policy arcade_own on public.arcade_daily_rewards for select to authenticated using(user_id=auth.uid() or public.is_admin());
create policy maze_own on public.maze_sessions for select to authenticated using(user_id=auth.uid() or public.is_admin());
create policy treasure_actions_own on public.treasure_actions for select to authenticated using(user_id=auth.uid() or public.is_admin());
create policy treasure_events_own on public.treasure_action_events for select to authenticated using(user_id=auth.uid() or public.is_admin());
create policy treasure_weeks_read on public.treasure_weeks for select to authenticated using(true);
create policy treasure_claims_own on public.treasure_chest_claims for select to authenticated using(user_id=auth.uid() or public.is_admin());
create policy boss_read on public.treasure_bosses for select to authenticated using(true);
create policy boss_participants_read on public.treasure_boss_participants for select to authenticated using(user_id=auth.uid() or public.is_admin());
create policy share_codes_read on public.treasure_share_codes for select to authenticated using(owner_id=auth.uid() or public.is_admin());
create policy share_claims_own on public.treasure_share_claims for select to authenticated using(user_id=auth.uid() or public.is_admin());
create policy seasons_read on public.treasure_seasons for select to authenticated using(active=true or public.is_admin());
create policy treasure_top_read on public.treasure_top_rewards for select to authenticated using(true);
create policy pets_own on public.m4x_pets for select to authenticated using(user_id=auth.uid() or public.is_admin());

-- Không cho client gọi các helper nội bộ.
revoke all on function public.m4x_add_treasure_action(uuid,text,integer,text) from public,anon,authenticated;
revoke all on function public.m4x_grant_pet_food(uuid,integer) from public,anon,authenticated;
revoke all on function public.m4x_ensure_treasure_week(uuid) from public,anon,authenticated;
revoke all on function public.distribute_treasure_top_rewards(boolean) from public,anon,authenticated;

grant execute on function public.record_theme_view(uuid) to authenticated;
grant execute on function public.claim_obstacle_reward(integer) to authenticated;
grant execute on function public.start_maze_game(text) to authenticated;
grant execute on function public.finish_maze_game(uuid) to authenticated;
grant execute on function public.get_treasure_state() to authenticated;
grant execute on function public.claim_treasure_day() to authenticated;
grant execute on function public.claim_treasure_chest(text) to authenticated;
grant execute on function public.use_treasure_rescue_card() to authenticated;
grant execute on function public.attack_treasure_boss() to authenticated;
grant execute on function public.redeem_treasure_share_code(text) to authenticated;
grant execute on function public.get_m4x_pet() to authenticated;
grant execute on function public.feed_m4x_pet() to authenticated;
grant execute on function public.distribute_treasure_top_rewards(boolean) to service_role;

notify pgrst,'reload schema';
commit;

-- ---------- 14) Cron Top thợ săn: 23:40 Chủ nhật giờ Việt Nam ----------
create extension if not exists pg_cron with schema extensions;
do $$ declare j bigint; begin
  select jobid into j from cron.job where jobname='m4x-treasure-top-rewards' limit 1;
  if j is not null then perform cron.unschedule(j); end if;
end $$;
select cron.schedule('m4x-treasure-top-rewards','40 16 * * 0',$$select public.distribute_treasure_top_rewards(false);$$);
