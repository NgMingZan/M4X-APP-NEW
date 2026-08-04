-- M4X Theme v3.2.0 - Rust Security Suite
-- Chạy SAU SUPABASE_V2_SETUP.sql và các migration v3.x hiện có.
-- Có thể chạy lại nhiều lần.

alter table public.themes
  add column if not exists client_validation_status text not null default 'unchecked',
  add column if not exists client_validation_message text not null default '',
  add column if not exists client_file_sha256 text not null default '',
  add column if not exists client_file_size_bytes bigint not null default 0,
  add column if not exists client_validation_at timestamptz,
  add column if not exists client_safety_score integer not null default 0,
  add column if not exists client_safety_level text not null default 'danger',
  add column if not exists client_theme_metadata jsonb not null default '{}'::jsonb,
  add column if not exists client_module_report jsonb not null default '[]'::jsonb,
  add column if not exists client_validation_report jsonb not null default '{}'::jsonb,
  add column if not exists approved_file_sha256 text not null default '',
  add column if not exists approved_file_size_bytes bigint not null default 0,
  add column if not exists approved_hash_at timestamptz;

do $$
begin
  if not exists (
    select 1 from pg_constraint
    where conname = 'themes_client_safety_score_check'
      and conrelid = 'public.themes'::regclass
  ) then
    alter table public.themes
      add constraint themes_client_safety_score_check
      check (client_safety_score between 0 and 100);
  end if;

  if not exists (
    select 1 from pg_constraint
    where conname = 'themes_client_safety_level_check'
      and conrelid = 'public.themes'::regclass
  ) then
    alter table public.themes
      add constraint themes_client_safety_level_check
      check (
        client_safety_level in (
          'excellent',
          'good',
          'caution',
          'danger'
        )
      );
  end if;
end $$;

-- Khi Admin duyệt, khóa SHA-256 và dung lượng của file đã kiểm tra.
create or replace function public.freeze_approved_theme_hash()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  if new.status = 'approved'
     and old.status is distinct from new.status then

    -- Kiểm soát vận hành: file trực tiếp bị Rust từ chối hoặc dưới 60 điểm
    -- không được duyệt. Đây vẫn không thay thế kiểm tra độc lập phía server.
    if coalesce(new.file_url, '') <> '' then
      if new.client_validation_status = 'failed' then
        raise exception 'Không thể duyệt file đã bị Rust từ chối';
      end if;

      if new.client_safety_score > 0
         and new.client_safety_score < 60 then
        raise exception 'Điểm an toàn dưới 60; cần thay file hoặc kiểm tra lại';
      end if;
    end if;

    if coalesce(new.client_file_sha256, '') <> '' then
      new.approved_file_sha256 := lower(new.client_file_sha256);
      new.approved_file_size_bytes := greatest(
        coalesce(new.client_file_size_bytes, 0),
        0
      );
      new.approved_hash_at := now();
    end if;
  end if;

  return new;
end $$;

drop trigger if exists trg_freeze_approved_theme_hash
on public.themes;

create trigger trg_freeze_approved_theme_hash
before update of status on public.themes
for each row
execute function public.freeze_approved_theme_hash();

-- Đồng bộ theme đã duyệt từ phiên bản trước.
update public.themes
set approved_file_sha256 = lower(client_file_sha256),
    approved_file_size_bytes = greatest(client_file_size_bytes, 0),
    approved_hash_at = coalesce(
      approved_hash_at,
      client_validation_at,
      now()
    )
where status = 'approved'
  and coalesce(approved_file_sha256, '') = ''
  and coalesce(client_file_sha256, '') <> '';

create index if not exists idx_themes_safety_score
  on public.themes(client_safety_score desc, created_at desc);

create index if not exists idx_themes_approved_sha256
  on public.themes(approved_file_sha256)
  where approved_file_sha256 <> '';

revoke execute on function public.freeze_approved_theme_hash()
from public, anon, authenticated;

notify pgrst, 'reload schema';
