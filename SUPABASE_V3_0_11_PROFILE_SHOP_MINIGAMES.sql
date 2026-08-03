-- ============================================================
-- M4X THEME v3.0.11
-- Hồ sơ mới + avatar điện thoại + Cửa hàng M4X + Minigame
-- Không xóa dữ liệu cũ.
-- ============================================================

begin;

-- 1) Avatar hồ sơ
alter table public.profiles
  add column if not exists avatar_url text not null default '';

-- 2) Chuẩn hóa bảng cửa hàng và kho đồ
create table if not exists public.shop_items (
  id uuid primary key default gen_random_uuid(),
  name text not null,
  item_type text not null,
  price integer not null default 0 check (price >= 0),
  image_url text not null default '',
  limited boolean not null default false,
  active boolean not null default true,
  metadata jsonb not null default '{}'::jsonb
);

alter table public.shop_items add column if not exists image_url text not null default '';
alter table public.shop_items add column if not exists limited boolean not null default false;
alter table public.shop_items add column if not exists active boolean not null default true;
alter table public.shop_items add column if not exists metadata jsonb not null default '{}'::jsonb;

create table if not exists public.user_inventory (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references public.profiles(id) on delete cascade,
  item_id uuid references public.shop_items(id) on delete set null,
  item_name text not null,
  item_type text not null,
  acquired_at timestamptz not null default now()
);

create unique index if not exists user_inventory_user_item_unique
  on public.user_inventory(user_id, item_id)
  where item_id is not null;

-- 3) Lịch sử minigame
create table if not exists public.minigame_plays (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references public.profiles(id) on delete cascade,
  game_code text not null,
  choice integer not null default 0,
  result_value integer not null default 0,
  reward integer not null default 0 check (reward >= 0),
  played_at timestamptz not null default now()
);

create index if not exists minigame_plays_user_game_time_idx
  on public.minigame_plays(user_id, game_code, played_at desc);

-- 4) RLS
alter table public.shop_items enable row level security;
alter table public.user_inventory enable row level security;
alter table public.minigame_plays enable row level security;

drop policy if exists shop_read on public.shop_items;
create policy shop_read
on public.shop_items for select to authenticated
using (active = true or public.is_admin());

drop policy if exists inventory_read on public.user_inventory;
create policy inventory_read
on public.user_inventory for select to authenticated
using (user_id = auth.uid() or public.is_admin());

drop policy if exists minigame_plays_read on public.minigame_plays;
create policy minigame_plays_read
on public.minigame_plays for select to authenticated
using (user_id = auth.uid() or public.is_admin());

-- 5) Mua vật phẩm bằng M4X COIN
create or replace function public.purchase_shop_item(p_item_id uuid)
returns bigint
language plpgsql
security definer
set search_path = public
as $$
declare
  item_row public.shop_items%rowtype;
  current_balance bigint;
begin
  if auth.uid() is null then
    raise exception 'Chưa đăng nhập';
  end if;

  select * into item_row
  from public.shop_items
  where id = p_item_id and active = true
  for update;

  if item_row.id is null then
    raise exception 'Vật phẩm không tồn tại hoặc đã tắt bán';
  end if;

  if exists (
    select 1 from public.user_inventory
    where user_id = auth.uid() and item_id = p_item_id
  ) then
    raise exception 'Bạn đã sở hữu vật phẩm này';
  end if;

  select points into current_balance
  from public.profiles
  where id = auth.uid()
  for update;

  if current_balance is null then
    raise exception 'Không tìm thấy hồ sơ';
  end if;

  if current_balance < item_row.price then
    raise exception 'Không đủ M4X COIN';
  end if;

  if item_row.price > 0 then
    perform public.add_coin(
      auth.uid(),
      -item_row.price,
      'shop_purchase',
      item_row.id::text
    );
  end if;

  insert into public.user_inventory(user_id, item_id, item_name, item_type)
  values(auth.uid(), item_row.id, item_row.name, item_row.item_type);

  select points into current_balance
  from public.profiles
  where id = auth.uid();

  return current_balance;
end
$$;

-- 6) Minigame chạy hoàn toàn trên server
create or replace function public.play_m4x_minigame(
  p_game_code text,
  p_choice integer default 0
)
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
  vn_date date;
  used_plays integer;
  max_plays integer;
  reward_amount integer;
  result_value integer;
  new_balance bigint;
  result_message text;
  remaining_plays integer;
  roll_value double precision;
begin
  if auth.uid() is null then
    raise exception 'Chưa đăng nhập';
  end if;

  vn_date := timezone('Asia/Ho_Chi_Minh', now())::date;

  if p_game_code = 'number_guess' then
    if p_choice < 1 or p_choice > 5 then
      raise exception 'Hãy chọn số từ 1 đến 5';
    end if;
    max_plays := 5;
  elsif p_game_code = 'lucky_card' then
    if p_choice < 1 or p_choice > 3 then
      raise exception 'Hãy chọn một trong ba thẻ';
    end if;
    max_plays := 3;
  else
    raise exception 'Minigame không hợp lệ';
  end if;

  select count(*) into used_plays
  from public.minigame_plays
  where user_id = auth.uid()
    and game_code = p_game_code
    and timezone('Asia/Ho_Chi_Minh', played_at)::date = vn_date;

  if used_plays >= max_plays then
    raise exception 'Bạn đã hết lượt minigame hôm nay';
  end if;

  if p_game_code = 'number_guess' then
    result_value := (floor(random() * 5) + 1)::integer;
    if p_choice = result_value then
      reward_amount := 50;
      result_message := 'Chính xác! Số may mắn là ' || result_value || '.';
    else
      reward_amount := 5;
      result_message := 'Số đúng là ' || result_value || '. Bạn vẫn nhận quà khích lệ.';
    end if;
  else
    result_value := p_choice;
    roll_value := random();
    if roll_value < 0.50 then
      reward_amount := 5;
    elsif roll_value < 0.80 then
      reward_amount := 10;
    elsif roll_value < 0.95 then
      reward_amount := 20;
    else
      reward_amount := 50;
    end if;
    result_message := 'Thẻ ' || p_choice || ' mang về ' || reward_amount || ' M4X COIN.';
  end if;

  perform public.add_coin(
    auth.uid(),
    reward_amount,
    'minigame_' || p_game_code,
    vn_date::text
  );

  insert into public.minigame_plays(
    user_id, game_code, choice, result_value, reward
  ) values(
    auth.uid(), p_game_code, p_choice, result_value, reward_amount
  );

  select points into new_balance
  from public.profiles
  where id = auth.uid();

  remaining_plays := max_plays - used_plays - 1;

  return jsonb_build_object(
    'reward', reward_amount,
    'balance', new_balance,
    'message', result_message,
    'remaining', greatest(remaining_plays, 0)
  );
end
$$;

revoke all on function public.purchase_shop_item(uuid) from public, anon;
revoke all on function public.play_m4x_minigame(text, integer) from public, anon;
grant execute on function public.purchase_shop_item(uuid) to authenticated;
grant execute on function public.play_m4x_minigame(text, integer) to authenticated;

-- 7) Vật phẩm mặc định
insert into public.shop_items(name, item_type, price, image_url, limited, active, metadata)
select * from (values
  ('Khung avatar Neon M4X', 'avatar_frame', 1200, '', false, true, '{"theme":"neon"}'::jsonb),
  ('Màu tên Cyan', 'name_color', 800, '', false, true, '{"color":"#28D7F4"}'::jsonb),
  ('Màu tên Tím M4X', 'name_color', 1000, '', false, true, '{"color":"#935BFF"}'::jsonb),
  ('Hiệu ứng Sao băng', 'profile_effect', 2500, '', false, true, '{"effect":"meteor"}'::jsonb),
  ('Nền hồ sơ Đại dương', 'profile_background', 3000, '', false, true, '{"background":"ocean"}'::jsonb),
  ('Huy hiệu Nhà sáng tạo', 'badge', 3500, '', true, true, '{"badge":"creator"}'::jsonb)
) as v(name, item_type, price, image_url, limited, active, metadata)
where not exists (
  select 1 from public.shop_items s where s.name = v.name
);

notify pgrst, 'reload schema';

commit;
