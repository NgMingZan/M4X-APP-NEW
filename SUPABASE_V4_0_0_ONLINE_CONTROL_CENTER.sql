-- ============================================================
-- M4X THEME v4.0.0 — TRUNG TÂM CẤU HÌNH ONLINE
--
-- Sau khi chạy file này, Admin có thể thay đổi trực tiếp trong app:
-- - Banner trang chủ
-- - Thông báo trong app
-- - Nhiệm vụ hằng ngày
-- - Quà điểm danh
-- - Vòng quay online
-- - Bật/tắt câu cá, hệ số giá cá và máu Boss
-- - Theme nổi bật
--
-- Giftcode tiếp tục dùng hệ thống Giftcode hiện có.
-- ============================================================

begin;

create table if not exists public.online_control_config (
  id text primary key default 'main',
  banner_enabled boolean not null default true,
  banner_title text not null default 'M4X Theme',
  banner_subtitle text not null
    default 'Kho giao diện HyperOS & MIUI',

  notice_enabled boolean not null default false,
  notice_title text not null default 'Thông báo',
  notice_message text not null default '',

  daily_quest_enabled boolean not null default true,
  daily_quest_title text not null
    default 'Điểm danh nhiệm vụ online',
  daily_quest_description text not null
    default 'Mở ứng dụng và nhận quà hôm nay',
  daily_quest_reward integer not null default 100
    check (daily_quest_reward between 0 and 1000000),

  checkin_enabled boolean not null default true,
  checkin_rewards jsonb not null
    default '[50,75,100,125,150,200,300]'::jsonb,

  spin_enabled boolean not null default true,
  spin_cost integer not null default 25
    check (spin_cost between 0 and 1000000),
  spin_rewards jsonb not null default
    '[
      {"reward":0,"weight":20},
      {"reward":10,"weight":45},
      {"reward":50,"weight":20},
      {"reward":100,"weight":10},
      {"reward":250,"weight":5}
    ]'::jsonb,

  fishing_enabled boolean not null default true,
  fishing_closed_message text not null
    default 'M4X Fishing đang bảo trì',
  fishing_reward_multiplier numeric(6,2) not null default 1.00
    check (
      fishing_reward_multiplier between 0.10 and 10.00
    ),
  fishing_boss_hp_multiplier numeric(6,2) not null default 1.00
    check (
      fishing_boss_hp_multiplier between 0.10 and 10.00
    ),

  featured_theme_id uuid null
    references public.themes(id) on delete set null,

  updated_at timestamptz not null default now()
);

insert into public.online_control_config(id)
values ('main')
on conflict (id) do nothing;


create table if not exists public.online_daily_quest_claims (
  user_id uuid not null
    references public.profiles(id) on delete cascade,
  claim_date date not null default current_date,
  reward integer not null default 0,
  claimed_at timestamptz not null default now(),
  primary key (user_id, claim_date)
);


create table if not exists public.online_checkin_state (
  user_id uuid primary key
    references public.profiles(id) on delete cascade,
  streak integer not null default 0,
  last_claim_date date null,
  updated_at timestamptz not null default now()
);


create table if not exists public.online_spin_history (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null
    references public.profiles(id) on delete cascade,
  cost integer not null,
  reward integer not null,
  created_at timestamptz not null default now()
);


alter table public.online_control_config
  enable row level security;
alter table public.online_daily_quest_claims
  enable row level security;
alter table public.online_checkin_state
  enable row level security;
alter table public.online_spin_history
  enable row level security;


grant select, update
on public.online_control_config
to authenticated;

grant select
on public.online_daily_quest_claims,
   public.online_checkin_state,
   public.online_spin_history
to authenticated;


drop policy if exists online_control_read
  on public.online_control_config;
create policy online_control_read
on public.online_control_config
for select
to authenticated
using (true);

drop policy if exists online_control_admin_update
  on public.online_control_config;
create policy online_control_admin_update
on public.online_control_config
for update
to authenticated
using (public.is_admin())
with check (public.is_admin());

drop policy if exists online_daily_claims_read_own
  on public.online_daily_quest_claims;
create policy online_daily_claims_read_own
on public.online_daily_quest_claims
for select
to authenticated
using (
  user_id = auth.uid() or public.is_admin()
);

drop policy if exists online_checkin_read_own
  on public.online_checkin_state;
create policy online_checkin_read_own
on public.online_checkin_state
for select
to authenticated
using (
  user_id = auth.uid() or public.is_admin()
);

drop policy if exists online_spin_read_own
  on public.online_spin_history;
create policy online_spin_read_own
on public.online_spin_history
for select
to authenticated
using (
  user_id = auth.uid() or public.is_admin()
);


create or replace function
public.touch_online_control_updated_at()
returns trigger
language plpgsql
set search_path = public
as $$
begin
  new.updated_at := now();
  return new;
end;
$$;

drop trigger if exists
  trg_online_control_updated_at
  on public.online_control_config;

create trigger trg_online_control_updated_at
before update on public.online_control_config
for each row
execute function public.touch_online_control_updated_at();


create or replace function public.claim_online_daily_quest()
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
  uid uuid := auth.uid();
  cfg public.online_control_config%rowtype;
  inserted_user uuid;
  new_balance bigint;
begin
  if uid is null then
    raise exception 'Bạn cần đăng nhập';
  end if;

  select *
    into cfg
  from public.online_control_config
  where id = 'main';

  if not coalesce(cfg.daily_quest_enabled, false) then
    raise exception 'Nhiệm vụ online đang tạm đóng';
  end if;

  perform pg_advisory_xact_lock(
    hashtext('online-daily-' || uid::text)
  );

  insert into public.online_daily_quest_claims(
    user_id,
    claim_date,
    reward
  )
  values (
    uid,
    current_date,
    greatest(cfg.daily_quest_reward, 0)
  )
  on conflict (user_id, claim_date) do nothing
  returning user_id into inserted_user;

  if inserted_user is null then
    raise exception 'Bạn đã nhận nhiệm vụ online hôm nay';
  end if;

  update public.profiles
     set points =
       points + greatest(cfg.daily_quest_reward, 0)
   where id = uid
  returning points into new_balance;

  insert into public.coin_transactions(
    user_id,
    amount,
    reason,
    ref_id
  )
  values (
    uid,
    greatest(cfg.daily_quest_reward, 0),
    'Nhiệm vụ online',
    current_date::text
  );

  return jsonb_build_object(
    'reward', greatest(cfg.daily_quest_reward, 0),
    'balance', coalesce(new_balance, 0),
    'message',
      'Đã nhận ' ||
      greatest(cfg.daily_quest_reward, 0) ||
      ' M4X Coin từ nhiệm vụ online'
  );
end;
$$;


create or replace function public.claim_online_checkin()
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
  uid uuid := auth.uid();
  cfg public.online_control_config%rowtype;
  state_row public.online_checkin_state%rowtype;
  reward_count integer;
  next_streak integer;
  reward_amount integer;
  new_balance bigint;
begin
  if uid is null then
    raise exception 'Bạn cần đăng nhập';
  end if;

  select *
    into cfg
  from public.online_control_config
  where id = 'main';

  if not coalesce(cfg.checkin_enabled, false) then
    raise exception 'Điểm danh đang tạm đóng';
  end if;

  reward_count :=
    jsonb_array_length(cfg.checkin_rewards);

  if reward_count <= 0 then
    raise exception 'Admin chưa cấu hình quà điểm danh';
  end if;

  perform pg_advisory_xact_lock(
    hashtext('online-checkin-' || uid::text)
  );

  select *
    into state_row
  from public.online_checkin_state
  where user_id = uid
  for update;

  if state_row.user_id is not null
     and state_row.last_claim_date = current_date then
    raise exception 'Bạn đã điểm danh hôm nay';
  end if;

  if state_row.user_id is null then
    next_streak := 1;
  elsif state_row.last_claim_date =
      current_date - 1 then
    next_streak := state_row.streak + 1;
  else
    next_streak := 1;
  end if;

  if next_streak > reward_count then
    next_streak := 1;
  end if;

  reward_amount :=
    greatest(
      0,
      coalesce(
        (
          cfg.checkin_rewards ->>
          (next_streak - 1)
        )::integer,
        0
      )
    );

  insert into public.online_checkin_state(
    user_id,
    streak,
    last_claim_date,
    updated_at
  )
  values (
    uid,
    next_streak,
    current_date,
    now()
  )
  on conflict (user_id) do update
  set streak = excluded.streak,
      last_claim_date = excluded.last_claim_date,
      updated_at = now();

  update public.profiles
     set points = points + reward_amount
   where id = uid
  returning points into new_balance;

  insert into public.coin_transactions(
    user_id,
    amount,
    reason,
    ref_id
  )
  values (
    uid,
    reward_amount,
    'Điểm danh online',
    current_date::text
  );

  return jsonb_build_object(
    'reward', reward_amount,
    'balance', coalesce(new_balance, 0),
    'streak', next_streak,
    'message',
      'Điểm danh ngày ' || next_streak ||
      ', nhận ' || reward_amount || ' M4X Coin'
  );
end;
$$;


create or replace function public.spin_online_wheel()
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
  uid uuid := auth.uid();
  cfg public.online_control_config%rowtype;
  current_balance bigint;
  new_balance bigint;
  reward_item jsonb;
  selected_reward integer := 0;
  total_weight integer := 0;
  cumulative_weight integer := 0;
  random_weight integer;
begin
  if uid is null then
    raise exception 'Bạn cần đăng nhập';
  end if;

  select *
    into cfg
  from public.online_control_config
  where id = 'main';

  if not coalesce(cfg.spin_enabled, false) then
    raise exception 'Vòng quay online đang tạm đóng';
  end if;

  perform pg_advisory_xact_lock(
    hashtext('online-spin-' || uid::text)
  );

  select points
    into current_balance
  from public.profiles
  where id = uid
  for update;

  if current_balance is null then
    raise exception 'Không tìm thấy hồ sơ';
  end if;

  if current_balance < greatest(cfg.spin_cost, 0) then
    raise exception 'Không đủ M4X Coin để quay';
  end if;

  select coalesce(
    sum(
      greatest(
        coalesce((item ->> 'weight')::integer, 0),
        0
      )
    ),
    0
  )
  into total_weight
  from jsonb_array_elements(
    cfg.spin_rewards
  ) as rewards(item);

  if total_weight <= 0 then
    raise exception 'Admin chưa cấu hình phần thưởng vòng quay';
  end if;

  random_weight :=
    floor(random() * total_weight)::integer + 1;

  for reward_item in
    select item
    from jsonb_array_elements(
      cfg.spin_rewards
    ) as rewards(item)
  loop
    cumulative_weight :=
      cumulative_weight +
      greatest(
        coalesce(
          (reward_item ->> 'weight')::integer,
          0
        ),
        0
      );

    if random_weight <= cumulative_weight then
      selected_reward :=
        greatest(
          coalesce(
            (reward_item ->> 'reward')::integer,
            0
          ),
          0
        );
      exit;
    end if;
  end loop;

  update public.profiles
     set points =
       points -
       greatest(cfg.spin_cost, 0) +
       selected_reward
   where id = uid
  returning points into new_balance;

  insert into public.coin_transactions(
    user_id,
    amount,
    reason,
    ref_id
  )
  values (
    uid,
    selected_reward - greatest(cfg.spin_cost, 0),
    'Vòng quay online',
    gen_random_uuid()::text
  );

  insert into public.online_spin_history(
    user_id,
    cost,
    reward
  )
  values (
    uid,
    greatest(cfg.spin_cost, 0),
    selected_reward
  );

  return jsonb_build_object(
    'reward', selected_reward,
    'cost', greatest(cfg.spin_cost, 0),
    'balance', coalesce(new_balance, 0),
    'message',
      case
        when selected_reward > 0 then
          'Vòng quay nhận ' ||
          selected_reward || ' M4X Coin'
        else
          'Chúc bạn may mắn lần sau'
      end
  );
end;
$$;


-- Giá trị cá được nhân trực tiếp theo cấu hình online
-- ngay trước khi ghi vào kho.
create or replace function
public.apply_online_fishing_reward_multiplier()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
  multiplier numeric := 1.00;
begin
  select fishing_reward_multiplier
    into multiplier
  from public.online_control_config
  where id = 'main';

  new.sell_value :=
    greatest(
      1,
      round(
        new.sell_value *
        coalesce(multiplier, 1.00)
      )::integer
    );

  return new;
end;
$$;

drop trigger if exists
  trg_online_fishing_reward_multiplier
  on public.fishing_catches;

create trigger trg_online_fishing_reward_multiplier
before insert on public.fishing_catches
for each row
execute function
  public.apply_online_fishing_reward_multiplier();


revoke all on function
  public.claim_online_daily_quest()
from public, anon, authenticated;

revoke all on function
  public.claim_online_checkin()
from public, anon, authenticated;

revoke all on function
  public.spin_online_wheel()
from public, anon, authenticated;

grant execute on function
  public.claim_online_daily_quest()
to authenticated;

grant execute on function
  public.claim_online_checkin()
to authenticated;

grant execute on function
  public.spin_online_wheel()
to authenticated;

notify pgrst, 'reload schema';

commit;

select
  'M4X v4.0.0 Online Control Center installed successfully'
  as result;
