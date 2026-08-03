-- M4X Theme v3.1.1 — Rust client validation
-- Chạy sau các SQL của v3.1.0.
-- Kết quả client_* chỉ là kiểm tra ban đầu trên thiết bị, không thay thế duyệt Admin.

alter table public.themes
  add column if not exists client_validation_status text not null default 'unchecked',
  add column if not exists client_validation_message text not null default '',
  add column if not exists client_file_sha256 text not null default '',
  add column if not exists client_file_size_bytes bigint not null default 0,
  add column if not exists client_validation_at timestamptz;

do $$
begin
  if not exists (
    select 1
    from pg_constraint
    where conname = 'themes_client_validation_status_check'
      and conrelid = 'public.themes'::regclass
  ) then
    alter table public.themes
      add constraint themes_client_validation_status_check
      check (
        client_validation_status in (
          'unchecked',
          'passed',
          'warning',
          'failed'
        )
      );
  end if;
end $$;

create index if not exists idx_themes_client_validation
  on public.themes(client_validation_status, created_at desc);

notify pgrst, 'reload schema';
