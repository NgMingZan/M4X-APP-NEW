-- ============================================================
-- M4X THEME v3.8.0 — HỆ THỐNG KIỂM DUYỆT THEME NÂNG CAO
-- BẮT BUỘC CHẠY SAU SQL v3.7.0
-- ============================================================
-- Có: checklist, lịch sử, thu hồi, điểm uy tín, thông báo trực tiếp,
--      sửa và gửi lại theme bị từ chối.
-- ============================================================

create extension if not exists pgcrypto;

-- 1. Mở rộng lịch sử duyệt.
alter table public.theme_reviews
  add column if not exists checklist jsonb not null default '{}'::jsonb,
  add column if not exists theme_title text not null default '',
  add column if not exists owner_id uuid references public.profiles(id) on delete set null,
  add column if not exists owner_name text not null default '',
  add column if not exists reviewer_name text not null default '',
  add column if not exists reviewer_role text not null default 'creator';

alter table public.theme_reviews drop constraint if exists theme_reviews_decision_check;
alter table public.theme_reviews
  add constraint theme_reviews_decision_check
  check (decision in ('approved', 'rejected', 'revoked'));

-- 2. Điểm uy tín Nhà sáng tạo.
create table if not exists public.creator_reputation (
  user_id uuid primary key references public.profiles(id) on delete cascade,
  score integer not null default 50 check (score between 0 and 100),
  approved_count integer not null default 0,
  rejected_count integer not null default 0,
  revoked_count integer not null default 0,
  total_reviews integer not null default 0,
  updated_at timestamptz not null default now()
);

alter table public.creator_reputation enable row level security;

drop policy if exists creator_reputation_read_own_or_admin on public.creator_reputation;
create policy creator_reputation_read_own_or_admin
on public.creator_reputation for select to authenticated
using (user_id = auth.uid() or public.is_admin());

revoke all on table public.creator_reputation from anon, authenticated;
grant select on table public.creator_reputation to authenticated;

insert into public.creator_reputation(user_id)
select id from public.profiles where role = 'creator'
on conflict (user_id) do nothing;

create or replace function public.ensure_creator_reputation()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  if new.role = 'creator' then
    insert into public.creator_reputation(user_id)
    values(new.id)
    on conflict (user_id) do nothing;
  end if;
  return new;
end;
$$;

drop trigger if exists profiles_creator_reputation_trigger on public.profiles;
create trigger profiles_creator_reputation_trigger
after insert or update of role on public.profiles
for each row execute function public.ensure_creator_reputation();

-- 3. Thông báo trực tiếp cho người đăng.
create table if not exists public.theme_review_notifications (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references public.profiles(id) on delete cascade,
  theme_id uuid references public.themes(id) on delete set null,
  type text not null check (type in ('approved', 'rejected', 'revoked', 'resubmitted')),
  title text not null,
  message text not null,
  reason text not null default '',
  read_at timestamptz,
  created_at timestamptz not null default now()
);

alter table public.theme_review_notifications enable row level security;

drop policy if exists theme_review_notifications_read_own on public.theme_review_notifications;
create policy theme_review_notifications_read_own
on public.theme_review_notifications for select to authenticated
using (user_id = auth.uid());

drop policy if exists theme_review_notifications_update_own on public.theme_review_notifications;
create policy theme_review_notifications_update_own
on public.theme_review_notifications for update to authenticated
using (user_id = auth.uid())
with check (user_id = auth.uid());

revoke all on table public.theme_review_notifications from anon, authenticated;
grant select on table public.theme_review_notifications to authenticated;
grant update(read_at) on table public.theme_review_notifications to authenticated;

create index if not exists idx_theme_review_notifications_user_created
on public.theme_review_notifications(user_id, created_at desc);

create index if not exists idx_theme_reviews_reviewer_created
on public.theme_reviews(reviewer_id, created_at desc);

-- 4. RPC duyệt/từ chối với checklist bắt buộc.
-- Xóa hàm 3 tham số cũ trước để không tạo overload mơ hồ.
drop function if exists public.review_theme(uuid, boolean, text);

create or replace function public.review_theme(
  target_theme_id uuid,
  approve_theme boolean,
  review_reason text,
  review_checklist jsonb
)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
  reviewer_role text;
  reviewer_display text;
  current_status text;
  theme_owner uuid;
  theme_name text;
  owner_display text;
  final_reason text;
  next_status text;
  reputation_delta integer;
begin
  select role, coalesce(nullif(display_name, ''), username, 'Người duyệt')
  into reviewer_role, reviewer_display
  from public.profiles where id = auth.uid();

  if reviewer_role not in ('creator', 'admin', 'super_admin') then
    raise exception 'Bạn không có quyền duyệt theme';
  end if;

  select t.status, t.owner_id, t.title,
         coalesce(nullif(p.display_name, ''), p.username, 'Người đăng')
  into current_status, theme_owner, theme_name, owner_display
  from public.themes t
  left join public.profiles p on p.id = t.owner_id
  where t.id = target_theme_id
  for update of t;

  if not found then raise exception 'Không tìm thấy theme'; end if;
  if reviewer_role = 'creator' and current_status <> 'pending' then
    raise exception 'Nhà sáng tạo chỉ được xử lý theme đang chờ duyệt';
  end if;

  if approve_theme and not (
    review_checklist @> '{"preview_ok":true,"download_ok":true,"compatibility_ok":true,"safe_content":true,"not_duplicate":true}'::jsonb
  ) then
    raise exception 'Hãy hoàn thành đủ 5 mục checklist trước khi duyệt';
  end if;

  final_reason := trim(coalesce(review_reason, ''));
  if not approve_theme and length(final_reason) < 5 then
    raise exception 'Hãy nhập lý do từ chối rõ ràng';
  end if;

  next_status := case when approve_theme then 'approved' else 'rejected' end;

  update public.themes
  set status = next_status,
      reject_reason = case when approve_theme then '' else final_reason end,
      approved_file_sha256 = case when approve_theme then client_file_sha256 else approved_file_sha256 end,
      approved_file_size_bytes = case when approve_theme then client_file_size_bytes else approved_file_size_bytes end,
      updated_at = now()
  where id = target_theme_id;

  insert into public.theme_reviews(
    theme_id, reviewer_id, decision, reason, checklist,
    theme_title, owner_id, owner_name, reviewer_name, reviewer_role
  ) values (
    target_theme_id, auth.uid(), next_status,
    case when approve_theme then '' else final_reason end,
    coalesce(review_checklist, '{}'::jsonb), theme_name, theme_owner,
    owner_display, reviewer_display, reviewer_role
  );

  if reviewer_role = 'creator' then
    reputation_delta := case when approve_theme then 3 else 1 end;
    insert into public.creator_reputation(
      user_id, score, approved_count, rejected_count, total_reviews, updated_at
    ) values (
      auth.uid(), 50 + reputation_delta,
      case when approve_theme then 1 else 0 end,
      case when approve_theme then 0 else 1 end,
      1, now()
    )
    on conflict (user_id) do update set
      score = least(100, greatest(0, public.creator_reputation.score + reputation_delta)),
      approved_count = public.creator_reputation.approved_count + case when approve_theme then 1 else 0 end,
      rejected_count = public.creator_reputation.rejected_count + case when approve_theme then 0 else 1 end,
      total_reviews = public.creator_reputation.total_reviews + 1,
      updated_at = now();
  end if;

  insert into public.theme_review_notifications(user_id, theme_id, type, title, message, reason)
  values(
    theme_owner,
    target_theme_id,
    next_status,
    case when approve_theme then 'Theme đã được duyệt' else 'Theme cần chỉnh sửa' end,
    case when approve_theme
      then 'Theme "' || theme_name || '" đã được duyệt và có thể xuất hiện trên M4X Theme.'
      else 'Theme "' || theme_name || '" bị từ chối. Bạn có thể sửa và gửi lại sau khi hoàn thiện.'
    end,
    case when approve_theme then '' else final_reason end
  );
end;
$$;

revoke all on function public.review_theme(uuid, boolean, text, jsonb)
from public, anon, authenticated;
grant execute on function public.review_theme(uuid, boolean, text, jsonb)
to authenticated;

-- 5. Super Admin thu hồi theme và trừ điểm người duyệt.
create or replace function public.revoke_theme_approval(
  target_theme_id uuid,
  revoke_reason text
)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
  caller_role text;
  caller_name text;
  theme_owner uuid;
  theme_name text;
  owner_display text;
  original_reviewer uuid;
  original_reviewer_role text;
  final_reason text;
begin
  select role, coalesce(nullif(display_name, ''), username, 'Super Admin')
  into caller_role, caller_name
  from public.profiles where id = auth.uid();

  if caller_role <> 'super_admin' then
    raise exception 'Chỉ Super Admin được thu hồi theme';
  end if;

  final_reason := trim(coalesce(revoke_reason, ''));
  if length(final_reason) < 5 then raise exception 'Hãy nhập lý do thu hồi'; end if;

  select t.owner_id, t.title, coalesce(nullif(p.display_name, ''), p.username, 'Người đăng')
  into theme_owner, theme_name, owner_display
  from public.themes t left join public.profiles p on p.id = t.owner_id
  where t.id = target_theme_id and t.status = 'approved'
  for update of t;

  if not found then raise exception 'Theme không còn ở trạng thái đã duyệt'; end if;

  select reviewer_id, reviewer_role
  into original_reviewer, original_reviewer_role
  from public.theme_reviews
  where theme_id = target_theme_id and decision = 'approved'
  order by created_at desc limit 1;

  update public.themes
  set status = 'rejected', reject_reason = final_reason, updated_at = now()
  where id = target_theme_id;

  insert into public.theme_reviews(
    theme_id, reviewer_id, decision, reason, checklist,
    theme_title, owner_id, owner_name, reviewer_name, reviewer_role
  ) values (
    target_theme_id, auth.uid(), 'revoked', final_reason, '{}'::jsonb,
    theme_name, theme_owner, owner_display, caller_name, caller_role
  );

  if original_reviewer is not null and original_reviewer_role = 'creator' then
    update public.creator_reputation
    set score = greatest(0, score - 12),
        revoked_count = revoked_count + 1,
        updated_at = now()
    where user_id = original_reviewer;
  end if;

  insert into public.theme_review_notifications(user_id, theme_id, type, title, message, reason)
  values(
    theme_owner, target_theme_id, 'revoked', 'Theme đã bị thu hồi',
    'Theme "' || theme_name || '" đã bị Super Admin thu hồi. Bạn có thể sửa và gửi lại.',
    final_reason
  );
end;
$$;

revoke all on function public.revoke_theme_approval(uuid, text)
from public, anon, authenticated;
grant execute on function public.revoke_theme_approval(uuid, text)
to authenticated;

-- 6. Chủ theme sửa thông tin và gửi lại sau khi bị từ chối/thu hồi.
create or replace function public.resubmit_theme(
  target_theme_id uuid,
  new_title text,
  new_description text,
  new_drive_url text,
  new_coin_price integer
)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
  theme_name text;
begin
  if length(trim(coalesce(new_title, ''))) < 2 then raise exception 'Tên theme không hợp lệ'; end if;
  if coalesce(new_drive_url, '') <> '' and new_drive_url not like 'https://%' then
    raise exception 'Link tải phải bắt đầu bằng https://';
  end if;

  update public.themes
  set title = trim(new_title),
      description = trim(coalesce(new_description, '')),
      drive_url = trim(coalesce(new_drive_url, '')),
      coin_price = greatest(0, coalesce(new_coin_price, 0)),
      status = 'pending',
      reject_reason = '',
      updated_at = now()
  where id = target_theme_id
    and owner_id = auth.uid()
    and status = 'rejected'
  returning title into theme_name;

  if not found then raise exception 'Chỉ chủ theme mới được gửi lại theme bị từ chối'; end if;

  insert into public.theme_review_notifications(user_id, theme_id, type, title, message)
  values(
    auth.uid(), target_theme_id, 'resubmitted', 'Đã gửi lại theme',
    'Theme "' || theme_name || '" đã quay lại hàng chờ duyệt.'
  );
end;
$$;

revoke all on function public.resubmit_theme(uuid, text, text, text, integer)
from public, anon, authenticated;
grant execute on function public.resubmit_theme(uuid, text, text, text, integer)
to authenticated;

notify pgrst, 'reload schema';
