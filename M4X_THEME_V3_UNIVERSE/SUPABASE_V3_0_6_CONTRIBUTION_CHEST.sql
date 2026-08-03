-- M4X Theme v3.0.6 - Top đóng góp tuần + Rương M4X
-- Không xóa dữ liệu cũ. Chạy một lần trong Supabase SQL Editor.

alter table public.themes add column if not exists approved_at timestamptz;

create table if not exists public.app_usage_daily (
  user_id uuid not null references public.profiles(id) on delete cascade,
  usage_date date not null default current_date,
  active_minutes integer not null default 0 check(active_minutes between 0 and 120),
  primary key(user_id, usage_date)
);

create table if not exists public.theme_download_events (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references public.profiles(id) on delete cascade,
  theme_id uuid not null references public.themes(id) on delete cascade,
  downloaded_at timestamptz not null default now(),
  unique(user_id, theme_id)
);

create table if not exists public.chest_openings (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references public.profiles(id) on delete cascade,
  cost integer not null default 20,
  reward integer not null default 0,
  opened_at timestamptz not null default now()
);

create or replace function public.set_theme_approved_at()
returns trigger language plpgsql set search_path=public as $$
begin
  if new.status='approved' and old.status is distinct from 'approved' then
    new.approved_at=now();
  end if;
  return new;
end $$;

drop trigger if exists trg_theme_approved_at on public.themes;
create trigger trg_theme_approved_at before update of status on public.themes
for each row execute function public.set_theme_approved_at();

update public.themes set approved_at=coalesce(approved_at,created_at) where status='approved';

create or replace function public.record_app_usage(p_minutes integer default 1)
returns void language plpgsql security definer set search_path=public as $$
begin
  if auth.uid() is null then raise exception 'Chưa đăng nhập'; end if;
  insert into public.app_usage_daily(user_id,usage_date,active_minutes)
  values(auth.uid(),current_date,least(greatest(p_minutes,1),5))
  on conflict(user_id,usage_date) do update
  set active_minutes=least(120,public.app_usage_daily.active_minutes+excluded.active_minutes);
end $$;

-- Ghi lượt tải hợp lệ: mỗi người chỉ tính một lần cho mỗi theme, không tính tự tải theme của mình.
create or replace function public.increment_theme_download(theme_id uuid)
returns void language plpgsql security definer set search_path=public as $$
declare owner uuid;
begin
  if auth.uid() is null then raise exception 'Chưa đăng nhập'; end if;
  select owner_id into owner from public.themes where id=theme_id and status='approved';
  if owner is null then raise exception 'Theme không tồn tại hoặc chưa được duyệt'; end if;
  if owner <> auth.uid() then
    insert into public.theme_download_events(user_id,theme_id)
    values(auth.uid(),theme_id) on conflict(user_id,theme_id) do nothing;
  end if;
  update public.themes set downloads=downloads+1 where id=theme_id;
end $$;

create or replace view public.weekly_leaderboard as
with bounds as (
  select date_trunc('week',now()) as week_start, date_trunc('week',now())+interval '7 days' as week_end
), members as (
  select p.id,p.display_name,p.username from public.profiles p where p.role<>'banned'
), approved as (
  select t.owner_id,count(*)::integer approved_themes
  from public.themes t,bounds b
  where t.status='approved' and t.approved_at>=b.week_start and t.approved_at<b.week_end
  group by t.owner_id
), received as (
  select t.owner_id,count(*)::integer downloads_received
  from public.theme_download_events d join public.themes t on t.id=d.theme_id,bounds b
  where d.downloaded_at>=b.week_start and d.downloaded_at<b.week_end
  group by t.owner_id
), usage as (
  select u.user_id,sum(u.active_minutes)::integer active_minutes
  from public.app_usage_daily u,bounds b
  where u.usage_date>=b.week_start::date and u.usage_date<b.week_end::date
  group by u.user_id
), downloaded as (
  select d.user_id,count(*)::integer downloads_made
  from public.theme_download_events d,bounds b
  where d.downloaded_at>=b.week_start and d.downloaded_at<b.week_end
  group by d.user_id
), scored as (
  select m.id,m.display_name,m.username,
    coalesce(a.approved_themes,0) approved_themes,
    coalesce(r.downloads_received,0) downloads_received,
    coalesce(u.active_minutes,0) active_minutes,
    coalesce(d.downloads_made,0) downloads_made,
    (coalesce(a.approved_themes,0)*500
      +coalesce(r.downloads_received,0)*10
      +floor(coalesce(u.active_minutes,0)/10.0)::bigint*5
      +coalesce(d.downloads_made,0)*20)::bigint score
  from members m
  left join approved a on a.owner_id=m.id
  left join received r on r.owner_id=m.id
  left join usage u on u.user_id=m.id
  left join downloaded d on d.user_id=m.id
)
select row_number() over(order by score desc,display_name asc)::integer rank,
       display_name,username,score,approved_themes,downloads_received,active_minutes,downloads_made
from scored where score>0;

create or replace function public.open_coin_chest()
returns jsonb language plpgsql security definer set search_path=public as $$
declare
  balance bigint;
  roll numeric;
  prize integer;
  msg text;
begin
  if auth.uid() is null then raise exception 'Chưa đăng nhập'; end if;
  select points into balance from public.profiles where id=auth.uid() for update;
  if balance<20 then raise exception 'Bạn cần ít nhất 20 M4X COIN'; end if;

  roll=random()*100;
  prize := case
    when roll < 45.0 then 0
    when roll < 88.0 then 10
    when roll < 94.0 then 20
    when roll < 97.0 then 50
    when roll < 98.5 then 100
    when roll < 99.2 then 200
    when roll < 99.6 then 500
    when roll < 99.8 then 1000
    when roll < 99.9 then 2000
    else 3500
  end;

  update public.profiles set points=points-20+prize where id=auth.uid() returning points into balance;
  insert into public.coin_transactions(user_id,amount,reason,ref_id)
  values(auth.uid(),-20,'chest_cost','');
  if prize>0 then
    insert into public.coin_transactions(user_id,amount,reason,ref_id)
    values(auth.uid(),prize,'chest_reward','');
  end if;
  insert into public.chest_openings(user_id,cost,reward) values(auth.uid(),20,prize);

  msg := case when prize=0 then 'Chúc bạn may mắn lần sau' else 'Bạn đã mở trúng rương' end;
  return jsonb_build_object('reward',prize,'balance',balance,'message',msg);
end $$;

alter table public.app_usage_daily enable row level security;
alter table public.theme_download_events enable row level security;
alter table public.chest_openings enable row level security;

drop policy if exists usage_read_own on public.app_usage_daily;
create policy usage_read_own on public.app_usage_daily for select to authenticated using(user_id=auth.uid() or public.is_admin());
drop policy if exists downloads_read on public.theme_download_events;
create policy downloads_read on public.theme_download_events for select to authenticated using(user_id=auth.uid() or public.is_admin());
drop policy if exists chest_read_own on public.chest_openings;
create policy chest_read_own on public.chest_openings for select to authenticated using(user_id=auth.uid() or public.is_admin());

grant execute on function public.record_app_usage(integer) to authenticated;
grant execute on function public.open_coin_chest() to authenticated;
grant select on public.weekly_leaderboard to authenticated;

notify pgrst,'reload schema';
