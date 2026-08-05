-- ============================================================
-- M4X THEME v4.1.0
-- ĐÁNH GIÁ SAO VÀ BÌNH LUẬN RIÊNG CHO TỪNG THEME
--
-- Chạy toàn bộ file trong Supabase SQL Editor trước khi cài APK.
-- ============================================================

begin;

create extension if not exists pgcrypto;

create table if not exists public.theme_community_reviews (
  id uuid primary key default gen_random_uuid(),
  theme_id uuid not null
    references public.themes(id) on delete cascade,
  user_id uuid not null
    references public.profiles(id) on delete cascade,
  stars smallint not null
    check (stars between 1 and 5),
  comment text not null default '',
  hidden boolean not null default false,
  moderated_by uuid
    references public.profiles(id) on delete set null,
  moderated_at timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique(theme_id, user_id)
);

create index if not exists
  theme_community_reviews_theme_updated_idx
on public.theme_community_reviews(
  theme_id,
  updated_at desc
);

create index if not exists
  theme_community_reviews_user_idx
on public.theme_community_reviews(user_id);

alter table public.theme_community_reviews
  enable row level security;

revoke all on table public.theme_community_reviews
  from public, anon, authenticated;


create or replace function public.refresh_theme_community_rating(
  p_theme_id uuid
)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
  new_rating double precision;
begin
  select coalesce(
    round(avg(stars)::numeric, 2)::double precision,
    0
  )
  into new_rating
  from public.theme_community_reviews
  where theme_id = p_theme_id
    and hidden = false;

  update public.themes
  set rating = new_rating,
      updated_at = now()
  where id = p_theme_id;
end;
$$;


create or replace function public.theme_community_rating_trigger()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  if tg_op = 'DELETE' then
    perform public.refresh_theme_community_rating(
      old.theme_id
    );
  else
    perform public.refresh_theme_community_rating(
      new.theme_id
    );

    if tg_op = 'UPDATE'
       and old.theme_id is distinct from new.theme_id then
      perform public.refresh_theme_community_rating(
        old.theme_id
      );
    end if;
  end if;

  return null;
end;
$$;

drop trigger if exists
  trg_theme_community_rating
on public.theme_community_reviews;

create trigger trg_theme_community_rating
after insert or update or delete
on public.theme_community_reviews
for each row
execute function public.theme_community_rating_trigger();


create or replace function public.get_theme_review_summary(
  p_theme_id uuid
)
returns jsonb
language sql
stable
security definer
set search_path = public
as $$
  select jsonb_build_object(
    'average_rating',
      coalesce(
        round(avg(r.stars)::numeric, 2),
        0
      ),
    'review_count',
      count(*)::integer
  )
  from public.theme_community_reviews r
  where r.theme_id = p_theme_id
    and r.hidden = false;
$$;


create or replace function public.get_theme_community_reviews(
  p_theme_id uuid
)
returns table(
  review_id uuid,
  theme_id uuid,
  user_id uuid,
  display_name text,
  username text,
  avatar_url text,
  stars integer,
  comment text,
  hidden boolean,
  is_mine boolean,
  created_at timestamptz,
  updated_at timestamptz
)
language plpgsql
stable
security definer
set search_path = public
as $$
declare
  actor_role text;
begin
  if auth.uid() is null then
    raise exception 'Bạn cần đăng nhập';
  end if;

  select p.role
  into actor_role
  from public.profiles p
  where p.id = auth.uid();

  return query
  select
    r.id,
    r.theme_id,
    r.user_id,
    coalesce(
      nullif(p.display_name, ''),
      nullif(p.username, ''),
      'Người dùng M4X'
    ),
    coalesce(p.username, ''),
    coalesce(p.avatar_url, ''),
    r.stars::integer,
    r.comment,
    r.hidden,
    r.user_id = auth.uid(),
    r.created_at,
    r.updated_at
  from public.theme_community_reviews r
  join public.profiles p
    on p.id = r.user_id
  where r.theme_id = p_theme_id
    and (
      r.hidden = false
      or r.user_id = auth.uid()
      or actor_role in ('admin', 'super_admin')
    )
  order by r.updated_at desc
  limit 50;
end;
$$;


create or replace function public.submit_theme_community_review(
  p_theme_id uuid,
  p_stars integer,
  p_comment text
)
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
  uid uuid := auth.uid();
  clean_comment text;
  average_value numeric;
  review_total integer;
begin
  if uid is null then
    raise exception 'Bạn cần đăng nhập';
  end if;

  if p_stars not between 1 and 5 then
    raise exception 'Số sao phải từ 1 đến 5';
  end if;

  clean_comment := trim(coalesce(p_comment, ''));

  if char_length(clean_comment) > 600 then
    raise exception 'Bình luận tối đa 600 ký tự';
  end if;

  if not exists (
    select 1
    from public.themes t
    where t.id = p_theme_id
  ) then
    raise exception 'Theme không tồn tại';
  end if;

  if not exists (
    select 1
    from public.theme_purchases tp
    where tp.theme_id = p_theme_id
      and tp.user_id = uid
  ) then
    raise exception 'Bạn cần sở hữu theme trước khi đánh giá';
  end if;

  insert into public.theme_community_reviews(
    theme_id,
    user_id,
    stars,
    comment,
    hidden,
    moderated_by,
    moderated_at
  )
  values(
    p_theme_id,
    uid,
    p_stars,
    clean_comment,
    false,
    null,
    null
  )
  on conflict(theme_id, user_id)
  do update set
    stars = excluded.stars,
    comment = excluded.comment,
    updated_at = now();

  perform public.refresh_theme_community_rating(
    p_theme_id
  );

  select
    coalesce(round(avg(stars)::numeric, 2), 0),
    count(*)::integer
  into average_value, review_total
  from public.theme_community_reviews
  where theme_id = p_theme_id
    and hidden = false;

  return jsonb_build_object(
    'average_rating', average_value,
    'review_count', review_total
  );
end;
$$;


create or replace function public.moderate_theme_community_review(
  p_review_id uuid,
  p_hidden boolean
)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
  uid uuid := auth.uid();
  actor_role text;
  target_theme_id uuid;
begin
  if uid is null then
    raise exception 'Bạn cần đăng nhập';
  end if;

  select p.role
  into actor_role
  from public.profiles p
  where p.id = uid;

  if actor_role not in ('admin', 'super_admin') then
    raise exception 'Bạn không có quyền kiểm duyệt bình luận';
  end if;

  update public.theme_community_reviews
  set hidden = coalesce(p_hidden, true),
      moderated_by = uid,
      moderated_at = now(),
      updated_at = now()
  where id = p_review_id
  returning theme_id into target_theme_id;

  if target_theme_id is null then
    raise exception 'Không tìm thấy bình luận';
  end if;

  perform public.refresh_theme_community_rating(
    target_theme_id
  );
end;
$$;


revoke all on function public.refresh_theme_community_rating(uuid)
  from public, anon, authenticated;
revoke all on function public.get_theme_review_summary(uuid)
  from public, anon, authenticated;
revoke all on function public.get_theme_community_reviews(uuid)
  from public, anon, authenticated;
revoke all on function public.submit_theme_community_review(
  uuid, integer, text
) from public, anon, authenticated;
revoke all on function public.moderate_theme_community_review(
  uuid, boolean
) from public, anon, authenticated;

grant execute on function public.get_theme_review_summary(uuid)
  to authenticated;
grant execute on function public.get_theme_community_reviews(uuid)
  to authenticated;
grant execute on function public.submit_theme_community_review(
  uuid, integer, text
) to authenticated;
grant execute on function public.moderate_theme_community_review(
  uuid, boolean
) to authenticated;

-- Đồng bộ lại điểm trung bình cho các theme đã có dữ liệu.
update public.themes t
set rating = coalesce((
  select round(avg(r.stars)::numeric, 2)::double precision
  from public.theme_community_reviews r
  where r.theme_id = t.id
    and r.hidden = false
), 0);

notify pgrst, 'reload schema';

commit;

select
  'M4X v4.1.0 Theme Ratings & Comments installed successfully'
  as result;
