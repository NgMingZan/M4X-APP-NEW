-- M4X Theme v2 clean schema + online content update
-- Có thể chạy lại nhiều lần. Không dùng service_role trong APK.

create extension if not exists pgcrypto;

create table if not exists public.profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  username text unique not null,
  display_name text not null default '',
  role text not null default 'user' check (role in ('user','creator','admin','super_admin','banned')),
  points bigint not null default 0,
  created_at timestamptz not null default now()
);

-- Bổ sung cột cho bảng themes cũ nếu đã tồn tại.
create table if not exists public.themes (
  id uuid primary key default gen_random_uuid(),
  owner_id uuid references public.profiles(id) on delete cascade,
  title text not null default '',
  description text not null default '',
  category text not null default '',
  os_version text not null default '',
  file_url text not null default '',
  preview_url text not null default '',
  preview_urls jsonb not null default '[]'::jsonb,
  drive_url text not null default '',
  tags text not null default '',
  admin_note text not null default '',
  status text not null default 'pending' check (status in ('pending','approved','rejected')),
  reject_reason text not null default '',
  downloads bigint not null default 0,
  rating double precision not null default 0,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

alter table public.themes add column if not exists owner_id uuid references public.profiles(id) on delete cascade;
alter table public.themes add column if not exists title text not null default '';
alter table public.themes add column if not exists os_version text not null default '';
alter table public.themes add column if not exists preview_url text not null default '';
alter table public.themes add column if not exists preview_urls jsonb not null default '[]'::jsonb;
alter table public.themes add column if not exists drive_url text not null default '';
alter table public.themes add column if not exists tags text not null default '';
alter table public.themes add column if not exists admin_note text not null default '';
alter table public.themes add column if not exists status text not null default 'pending';
alter table public.themes add column if not exists reject_reason text not null default '';
alter table public.themes add column if not exists updated_at timestamptz not null default now();

-- Chuyển dữ liệu từ cấu trúc cũ nếu có.
do $$
begin
  if exists(select 1 from information_schema.columns where table_schema='public' and table_name='themes' and column_name='name') then
    execute 'update public.themes set title = coalesce(nullif(title, ''''), name) where coalesce(title, '''') = ''''';
  end if;
  if exists(select 1 from information_schema.columns where table_schema='public' and table_name='themes' and column_name='author') then
    execute 'update public.themes set owner_id = author::uuid where owner_id is null and author ~* ''^[0-9a-f-]{36}$''';
  end if;
  if exists(select 1 from information_schema.columns where table_schema='public' and table_name='themes' and column_name='approved') then
    execute 'update public.themes set status = case when approved then ''approved'' else ''pending'' end';
  end if;
  if exists(select 1 from information_schema.columns where table_schema='public' and table_name='themes' and column_name='file_url') then
    null;
  end if;
end $$;

create table if not exists public.app_config (
  id text primary key default 'main',
  min_version_code integer not null default 0,
  latest_version_code integer not null default 20,
  latest_version_name text not null default '2.0.0',
  update_url text not null default '',
  update_message text not null default '',
  force_update boolean not null default false,
  home_banner_title text not null default 'M4X Theme',
  home_banner_subtitle text not null default 'Kho giao diện HyperOS & MIUI',
  updated_at timestamptz not null default now()
);
insert into public.app_config(id) values ('main') on conflict (id) do nothing;

create or replace function public.is_admin()
returns boolean language sql stable security definer set search_path = public
as $$ select exists(select 1 from public.profiles where id = auth.uid() and role in ('admin','super_admin')); $$;

create or replace function public.is_super_admin()
returns boolean language sql stable security definer set search_path = public
as $$ select exists(select 1 from public.profiles where id = auth.uid() and role = 'super_admin'); $$;

create or replace function public.handle_new_user()
returns trigger language plpgsql security definer set search_path = public
as $$
declare requested_username text;
begin
  requested_username := lower(coalesce(new.raw_user_meta_data->>'username', split_part(new.email,'@',1)));
  insert into public.profiles(id, username, display_name, role)
  values(
    new.id,
    requested_username,
    coalesce(new.raw_user_meta_data->>'display_name',''),
    case when requested_username = 'minhdan' and not exists(select 1 from public.profiles where role='super_admin')
         then 'super_admin' else 'user' end
  );
  return new;
end;
$$;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created after insert on auth.users
for each row execute procedure public.handle_new_user();

create or replace function public.set_user_role(target_user_id uuid, new_role text)
returns void language plpgsql security definer set search_path = public
as $$
begin
  if not public.is_super_admin() then raise exception 'Chỉ Super Admin được đổi quyền'; end if;
  if new_role not in ('user','creator','admin','banned') then raise exception 'Vai trò không hợp lệ'; end if;
  if target_user_id = auth.uid() then raise exception 'Không thể tự đổi quyền'; end if;
  update public.profiles set role = new_role where id = target_user_id and role <> 'super_admin';
end;
$$;

create or replace function public.increment_theme_download(theme_id uuid)
returns void language plpgsql security definer set search_path = public
as $$
begin
  update public.themes set downloads = downloads + 1 where id = theme_id and status = 'approved';
end;
$$;

alter table public.profiles enable row level security;
alter table public.themes enable row level security;
alter table public.app_config enable row level security;

-- Xóa policy cũ để tránh xung đột tên/quyền.
do $$ declare p record; begin
  for p in select policyname, tablename from pg_policies where schemaname='public' and tablename in ('profiles','themes','app_config') loop
    execute format('drop policy if exists %I on public.%I', p.policyname, p.tablename);
  end loop;
end $$;

create policy profiles_read on public.profiles for select to authenticated using (true);
create policy profiles_insert_self on public.profiles for insert to authenticated with check (id = auth.uid());
create policy profiles_update_basic on public.profiles for update to authenticated
using (id = auth.uid())
with check (
  id = auth.uid()
  and role = (select role from public.profiles where id = auth.uid())
  and points = (select points from public.profiles where id = auth.uid())
);

create policy themes_read on public.themes for select to authenticated
using (status='approved' or owner_id=auth.uid() or public.is_admin());
create policy themes_insert on public.themes for insert to authenticated
with check (owner_id=auth.uid() and status='pending' and downloads=0);
create policy themes_admin_update on public.themes for update to authenticated
using (public.is_admin()) with check (public.is_admin());
create policy themes_admin_delete on public.themes for delete to authenticated
using (public.is_admin() or owner_id=auth.uid());

create policy app_config_read on public.app_config for select to authenticated using (true);
create policy app_config_admin_update on public.app_config for update to authenticated
using (public.is_admin()) with check (public.is_admin());

revoke execute on function public.set_user_role(uuid,text) from public, anon;
grant execute on function public.set_user_role(uuid,text) to authenticated;
revoke execute on function public.increment_theme_download(uuid) from public, anon;
grant execute on function public.increment_theme_download(uuid) to authenticated;

-- Storage bucket: tạo nếu chưa có. Public để link file tải trực tiếp hoạt động.
insert into storage.buckets(id, name, public, file_size_limit, allowed_mime_types)
values ('themes','themes',true,104857600,array['application/zip','application/octet-stream','image/jpeg','image/png','image/webp'])
on conflict (id) do update set public=true, file_size_limit=104857600, allowed_mime_types=excluded.allowed_mime_types;

-- Xóa policy Storage cùng tên nếu đã có.
drop policy if exists theme_files_public_read on storage.objects;
drop policy if exists theme_files_upload_own_folder on storage.objects;
drop policy if exists theme_files_delete_own_or_admin on storage.objects;

create policy theme_files_public_read on storage.objects for select using (bucket_id='themes');
create policy theme_files_upload_own_folder on storage.objects for insert to authenticated
with check (bucket_id='themes' and (storage.foldername(name))[1]=auth.uid()::text);
create policy theme_files_delete_own_or_admin on storage.objects for delete to authenticated
using (bucket_id='themes' and ((storage.foldername(name))[1]=auth.uid()::text or public.is_admin()));

-- =========================================================
-- M4X THEME V3 UNIVERSE: COIN, EVENT, QUEST, GIFTCODE, SHOP
-- =========================================================
alter table public.themes add column if not exists coin_price integer not null default 0 check (coin_price >= 0);
alter table public.profiles add column if not exists avatar_url text not null default '';
alter table public.profiles add column if not exists vip_level integer not null default 0;
alter table public.profiles add column if not exists level integer not null default 1;
alter table public.profiles add column if not exists badges text[] not null default '{}';

create table if not exists public.coin_transactions (
  id uuid primary key default gen_random_uuid(), user_id uuid not null references public.profiles(id) on delete cascade,
  amount bigint not null, reason text not null, ref_id text not null default '', created_at timestamptz not null default now()
);
create table if not exists public.events (
  id uuid primary key default gen_random_uuid(), title text not null, description text not null default '', banner_url text not null default '',
  start_at timestamptz not null default now(), end_at timestamptz not null default now() + interval '7 days', active boolean not null default true,
  theme jsonb not null default '{}'::jsonb, created_by uuid references public.profiles(id), created_at timestamptz not null default now()
);
create table if not exists public.quests (
  id uuid primary key default gen_random_uuid(), title text not null, description text not null default '', reward integer not null check(reward > 0),
  quest_type text not null default 'daily', sort_order integer not null default 0, active boolean not null default true, starts_at timestamptz default now(), ends_at timestamptz
);
create table if not exists public.quest_claims (
  id uuid primary key default gen_random_uuid(), user_id uuid not null references public.profiles(id) on delete cascade,
  quest_id uuid not null references public.quests(id) on delete cascade, claimed_at timestamptz not null default now(), unique(user_id, quest_id)
);
create table if not exists public.giftcodes (
  id uuid primary key default gen_random_uuid(), code text unique not null, reward integer not null check(reward > 0), max_uses integer not null default 1,
  used_count integer not null default 0, active boolean not null default true, expires_at timestamptz, created_at timestamptz not null default now()
);
create table if not exists public.giftcode_claims (
  id uuid primary key default gen_random_uuid(), giftcode_id uuid not null references public.giftcodes(id) on delete cascade,
  user_id uuid not null references public.profiles(id) on delete cascade, claimed_at timestamptz not null default now(), unique(giftcode_id,user_id)
);
create table if not exists public.theme_purchases (
  id uuid primary key default gen_random_uuid(), user_id uuid not null references public.profiles(id) on delete cascade,
  theme_id uuid not null references public.themes(id) on delete cascade, price integer not null default 0, purchased_at timestamptz not null default now(), unique(user_id, theme_id)
);
create table if not exists public.shop_items (
  id uuid primary key default gen_random_uuid(), name text not null, item_type text not null, price integer not null default 0,
  image_url text not null default '', limited boolean not null default false, active boolean not null default true, metadata jsonb not null default '{}'::jsonb
);
create table if not exists public.user_inventory (
  id uuid primary key default gen_random_uuid(), user_id uuid not null references public.profiles(id) on delete cascade,
  item_id uuid references public.shop_items(id) on delete set null, item_name text not null, item_type text not null, acquired_at timestamptz not null default now()
);
create table if not exists public.airdrops (
  id uuid primary key default gen_random_uuid(), reward integer not null check(reward between 100 and 2000), active boolean not null default true,
  claimed_by uuid references public.profiles(id), expires_at timestamptz not null, created_at timestamptz not null default now()
);
create table if not exists public.weekly_scores (
  user_id uuid primary key references public.profiles(id) on delete cascade, score bigint not null default 0, week_start date not null default date_trunc('week', now())::date
);

create or replace view public.weekly_leaderboard as
select row_number() over(order by ws.score desc)::integer as rank, p.display_name, p.username, ws.score
from public.weekly_scores ws join public.profiles p on p.id=ws.user_id
where ws.week_start=date_trunc('week',now())::date;

create or replace function public.add_coin(target uuid, delta bigint, why text, ref text default '') returns void
language plpgsql security definer set search_path=public as $$
begin
  update profiles set points=greatest(0,points+delta) where id=target;
  insert into coin_transactions(user_id,amount,reason,ref_id) values(target,delta,why,ref);
end; $$;

create or replace function public.claim_quest(quest_id uuid) returns void language plpgsql security definer set search_path=public as $$
declare r integer; begin
  select reward into r from quests where id=quest_id and active=true and (ends_at is null or ends_at>now());
  if r is null then raise exception 'Nhiệm vụ không khả dụng'; end if;
  insert into quest_claims(user_id,quest_id) values(auth.uid(),quest_id);
  perform add_coin(auth.uid(),r,'quest',quest_id::text);
  insert into weekly_scores(user_id,score) values(auth.uid(),r) on conflict(user_id) do update set score=weekly_scores.score+r;
end; $$;

create or replace function public.redeem_giftcode(gift_code text) returns integer language plpgsql security definer set search_path=public as $$
declare g giftcodes%rowtype; begin
  select * into g from giftcodes where upper(code)=upper(gift_code) and active=true and used_count<max_uses and (expires_at is null or expires_at>now()) for update;
  if g.id is null then raise exception 'Giftcode không hợp lệ hoặc đã hết lượt'; end if;
  insert into giftcode_claims(giftcode_id,user_id) values(g.id,auth.uid());
  update giftcodes set used_count=used_count+1 where id=g.id;
  perform add_coin(auth.uid(),g.reward,'giftcode',g.id::text);
  return g.reward;
end; $$;

create or replace function public.purchase_theme(theme_id uuid) returns void language plpgsql security definer set search_path=public as $$
declare p integer; balance bigint; begin
  select coin_price into p from themes where id=theme_id and status='approved';
  if p is null then raise exception 'Theme không tồn tại'; end if;
  if exists(select 1 from theme_purchases where user_id=auth.uid() and theme_purchases.theme_id=purchase_theme.theme_id) then return; end if;
  select points into balance from profiles where id=auth.uid() for update;
  if balance<p then raise exception 'Không đủ M4X COIN'; end if;
  perform add_coin(auth.uid(),-p,'theme_purchase',theme_id::text);
  insert into theme_purchases(user_id,theme_id,price) values(auth.uid(),theme_id,p);
  update themes set downloads=downloads+1 where id=theme_id;
end; $$;

create or replace function public.claim_active_airdrop() returns integer language plpgsql security definer set search_path=public as $$
declare a airdrops%rowtype; begin
  select * into a from airdrops where active=true and claimed_by is null and expires_at>now() order by created_at desc limit 1 for update skip locked;
  if a.id is null then raise exception 'Airdrop đã hết hoặc chưa được phát hành'; end if;
  update airdrops set claimed_by=auth.uid(),active=false where id=a.id;
  perform add_coin(auth.uid(),a.reward,'airdrop',a.id::text);
  return a.reward;
end; $$;

alter table public.coin_transactions enable row level security;
alter table public.events enable row level security;
alter table public.quests enable row level security;
alter table public.quest_claims enable row level security;
alter table public.giftcodes enable row level security;
alter table public.giftcode_claims enable row level security;
alter table public.theme_purchases enable row level security;
alter table public.shop_items enable row level security;
alter table public.user_inventory enable row level security;
alter table public.airdrops enable row level security;
alter table public.weekly_scores enable row level security;

create policy coin_read_own on public.coin_transactions for select to authenticated using(user_id=auth.uid() or public.is_admin());
create policy events_read on public.events for select to authenticated using(true);
create policy events_admin_all on public.events for all to authenticated using(public.is_admin()) with check(public.is_admin());
create policy quests_read on public.quests for select to authenticated using(active=true or public.is_admin());
create policy quests_admin_all on public.quests for all to authenticated using(public.is_admin()) with check(public.is_admin());
create policy quest_claims_read on public.quest_claims for select to authenticated using(user_id=auth.uid() or public.is_admin());
create policy giftcodes_admin_all on public.giftcodes for all to authenticated using(public.is_admin()) with check(public.is_admin());
create policy gift_claims_read on public.giftcode_claims for select to authenticated using(user_id=auth.uid() or public.is_admin());
create policy purchase_read on public.theme_purchases for select to authenticated using(user_id=auth.uid() or public.is_admin());
create policy shop_read on public.shop_items for select to authenticated using(active=true or public.is_admin());
create policy shop_admin_all on public.shop_items for all to authenticated using(public.is_admin()) with check(public.is_admin());
create policy inventory_read on public.user_inventory for select to authenticated using(user_id=auth.uid() or public.is_admin());
create policy airdrop_read on public.airdrops for select to authenticated using(true);
create policy airdrop_admin_all on public.airdrops for all to authenticated using(public.is_admin()) with check(public.is_admin());
create policy scores_read on public.weekly_scores for select to authenticated using(true);

revoke execute on function public.add_coin(uuid,bigint,text,text) from public,anon,authenticated;
grant execute on function public.claim_quest(uuid) to authenticated;
grant execute on function public.redeem_giftcode(text) to authenticated;
grant execute on function public.purchase_theme(uuid) to authenticated;
grant execute on function public.claim_active_airdrop() to authenticated;

insert into public.quests(title,description,reward,quest_type,sort_order,active) values
('Đăng nhập mỗi ngày','Mở ứng dụng và điểm danh hôm nay',100,'daily',1,true),
('Khám phá theme mới','Xem ít nhất 3 theme trong Kho M4X',150,'daily',2,true),
('Ủng hộ nhà sáng tạo','Tải hoặc mua một theme được duyệt',250,'weekly',3,true)
on conflict do nothing;

insert into public.shop_items(name,item_type,price,limited,metadata) values
('Khung avatar 4/8','avatar_frame',4080,true,'{"event":"admin_birthday"}'),
('Nền pháo hoa','profile_background',2500,true,'{"event":"admin_birthday"}'),
('Khách mời sinh nhật ADMIN','title',5000,true,'{"event":"admin_birthday"}')
on conflict do nothing;
