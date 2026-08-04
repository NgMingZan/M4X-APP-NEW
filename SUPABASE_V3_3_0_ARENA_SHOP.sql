-- ============================================================
-- M4X THEME v3.3.0
-- M4X Arena prototype: cửa hàng súng, giáp và vật phẩm.
-- Không xóa dữ liệu cũ.
-- ============================================================

begin;

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

with arena_items(name,item_type,price,image_url,limited,active,metadata) as (
  values
    (
      'SMG-7 Neon'::text,
      'arena_weapon'::text,
      850,
      ''::text,
      false,
      true,
      '{"damage":13,"fire_interval":0.085,"magazine":36,"reserve":144,"range":430}'::jsonb
    ),
    (
      'P90-X Plasma',
      'arena_weapon',
      1700,
      '',
      false,
      true,
      '{"damage":14,"fire_interval":0.072,"magazine":50,"reserve":150,"range":470}'::jsonb
    ),
    (
      'Viper-S Sniper',
      'arena_weapon',
      2500,
      '',
      true,
      true,
      '{"damage":54,"fire_interval":0.75,"magazine":8,"reserve":32,"range":900}'::jsonb
    ),
    (
      'Giáp MK-II',
      'arena_armor',
      900,
      '',
      false,
      true,
      '{"max_armor":80}'::jsonb
    ),
    (
      'Giày phản lực',
      'arena_boost',
      700,
      '',
      false,
      true,
      '{"move_speed":235}'::jsonb
    ),
    (
      'Túi cứu thương',
      'arena_utility',
      500,
      '',
      false,
      true,
      '{"medkits":3}'::jsonb
    )
)
update public.shop_items target
set item_type = source.item_type,
    price = source.price,
    image_url = source.image_url,
    limited = source.limited,
    active = source.active,
    metadata = source.metadata
from arena_items source
where target.name = source.name;

with arena_items(name,item_type,price,image_url,limited,active,metadata) as (
  values
    (
      'SMG-7 Neon'::text,
      'arena_weapon'::text,
      850,
      ''::text,
      false,
      true,
      '{"damage":13,"fire_interval":0.085,"magazine":36,"reserve":144,"range":430}'::jsonb
    ),
    (
      'P90-X Plasma',
      'arena_weapon',
      1700,
      '',
      false,
      true,
      '{"damage":14,"fire_interval":0.072,"magazine":50,"reserve":150,"range":470}'::jsonb
    ),
    (
      'Viper-S Sniper',
      'arena_weapon',
      2500,
      '',
      true,
      true,
      '{"damage":54,"fire_interval":0.75,"magazine":8,"reserve":32,"range":900}'::jsonb
    ),
    (
      'Giáp MK-II',
      'arena_armor',
      900,
      '',
      false,
      true,
      '{"max_armor":80}'::jsonb
    ),
    (
      'Giày phản lực',
      'arena_boost',
      700,
      '',
      false,
      true,
      '{"move_speed":235}'::jsonb
    ),
    (
      'Túi cứu thương',
      'arena_utility',
      500,
      '',
      false,
      true,
      '{"medkits":3}'::jsonb
    )
)
insert into public.shop_items(
  name,
  item_type,
  price,
  image_url,
  limited,
  active,
  metadata
)
select
  source.name,
  source.item_type,
  source.price,
  source.image_url,
  source.limited,
  source.active,
  source.metadata
from arena_items source
where not exists (
  select 1
  from public.shop_items existing
  where existing.name = source.name
);

-- Bảng lịch sử thử nghiệm chỉ dùng thống kê, không tự cộng Coin.
create table if not exists public.arena_prototype_matches (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references public.profiles(id) on delete cascade,
  rank integer not null check (rank between 1 and 10),
  kills integer not null default 0 check (kills >= 0),
  deaths integer not null default 0 check (deaths >= 0),
  winner text not null default '',
  client_version text not null default '3.3.0',
  played_at timestamptz not null default now()
);

create index if not exists arena_prototype_user_time_idx
  on public.arena_prototype_matches(user_id, played_at desc);

alter table public.arena_prototype_matches enable row level security;

drop policy if exists arena_prototype_read on public.arena_prototype_matches;
create policy arena_prototype_read
on public.arena_prototype_matches
for select
to authenticated
using (user_id = auth.uid() or public.is_admin());

-- Không cấp INSERT trực tiếp từ client để tránh tự giả kết quả.
revoke insert, update, delete
on public.arena_prototype_matches
from authenticated, anon;

notify pgrst, 'reload schema';

commit;
