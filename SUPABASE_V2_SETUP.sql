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
