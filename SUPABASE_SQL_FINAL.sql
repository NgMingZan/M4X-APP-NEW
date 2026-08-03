-- Chạy toàn bộ trong Supabase SQL Editor.
-- Bổ sung quyền upload Storage, duyệt theme và quản trị người dùng.

alter table public.themes enable row level security;
alter table public.profiles enable row level security;

-- Xóa policy cũ để chạy lại không lỗi.
drop policy if exists "Public can read approved themes" on public.themes;
drop policy if exists "Authenticated users can submit themes" on public.themes;
drop policy if exists "Users can read own submitted themes" on public.themes;
drop policy if exists "Admins can read pending themes" on public.themes;
drop policy if exists "Admins can update themes" on public.themes;
drop policy if exists "Admins can delete themes" on public.themes;

create policy "Public can read approved themes"
on public.themes for select
to anon, authenticated
using (approved = true);

create policy "Users can read own submitted themes"
on public.themes for select
to authenticated
using (author = auth.uid()::text);

create policy "Authenticated users can submit themes"
on public.themes for insert
to authenticated
with check (author = auth.uid()::text and approved = false);

create policy "Admins can read pending themes"
on public.themes for select
to authenticated
using (
  exists (
    select 1 from public.profiles p
    where p.id = auth.uid() and p.role in ('admin','super_admin')
  )
);

create policy "Admins can update themes"
on public.themes for update
to authenticated
using (
  exists (
    select 1 from public.profiles p
    where p.id = auth.uid() and p.role in ('admin','super_admin')
  )
)
with check (
  exists (
    select 1 from public.profiles p
    where p.id = auth.uid() and p.role in ('admin','super_admin')
  )
);

create policy "Admins can delete themes"
on public.themes for delete
to authenticated
using (
  exists (
    select 1 from public.profiles p
    where p.id = auth.uid() and p.role in ('admin','super_admin')
  )
);

-- Storage bucket themes phải được tạo trước và bật Public.
drop policy if exists "Authenticated upload theme files" on storage.objects;
drop policy if exists "Public read theme files" on storage.objects;
drop policy if exists "Owners delete own pending files" on storage.objects;

create policy "Authenticated upload theme files"
on storage.objects for insert
to authenticated
with check (
  bucket_id = 'themes'
  and (storage.foldername(name))[1] = auth.uid()::text
);

create policy "Public read theme files"
on storage.objects for select
to public
using (bucket_id = 'themes');

create policy "Owners delete own pending files"
on storage.objects for delete
to authenticated
using (
  bucket_id = 'themes'
  and (storage.foldername(name))[1] = auth.uid()::text
);
