-- ============================================================
-- M4X THEME v3.9.1
-- SỞ HỮU THEME ỔN ĐỊNH + NHÀ SÁNG TẠO CHỈNH SỬA THEME
-- Chạy toàn bộ file này trong Supabase SQL Editor.
-- ============================================================

begin;

create or replace function public.purchase_theme_v2(
    target_theme_id uuid
)
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
    theme_price integer;
    current_balance bigint;
    inserted_purchase uuid;
begin
    if auth.uid() is null then
        raise exception 'Chưa đăng nhập';
    end if;

    select coin_price
    into theme_price
    from public.themes
    where id = target_theme_id
      and status = 'approved';

    if theme_price is null then
        raise exception 'Theme không tồn tại hoặc chưa được duyệt';
    end if;

    select points
    into current_balance
    from public.profiles
    where id = auth.uid()
    for update;

    if current_balance is null then
        raise exception 'Không tìm thấy hồ sơ';
    end if;

    if exists (
        select 1
        from public.theme_purchases
        where user_id = auth.uid()
          and theme_id = target_theme_id
    ) then
        return jsonb_build_object(
            'theme_id', target_theme_id,
            'balance', current_balance,
            'already_owned', true
        );
    end if;

    if current_balance < theme_price then
        raise exception 'Không đủ M4X COIN';
    end if;

    insert into public.theme_purchases(
        user_id,
        theme_id,
        price
    )
    values(
        auth.uid(),
        target_theme_id,
        theme_price
    )
    on conflict(user_id, theme_id) do nothing
    returning id into inserted_purchase;

    if inserted_purchase is null then
        select points
        into current_balance
        from public.profiles
        where id = auth.uid();

        return jsonb_build_object(
            'theme_id', target_theme_id,
            'balance', current_balance,
            'already_owned', true
        );
    end if;

    perform public.add_coin(
        auth.uid(),
        -theme_price,
        'theme_purchase',
        target_theme_id::text
    );

    select points
    into current_balance
    from public.profiles
    where id = auth.uid();

    return jsonb_build_object(
        'theme_id', target_theme_id,
        'balance', current_balance,
        'already_owned', false
    );
end;
$$;

create or replace function public.get_owned_theme_ids()
returns table(theme_id uuid)
language sql
stable
security definer
set search_path = public
as $$
    select tp.theme_id
    from public.theme_purchases tp
    where tp.user_id = auth.uid()
    order by tp.purchased_at desc;
$$;

create or replace function public.get_owned_themes()
returns setof public.themes
language sql
stable
security definer
set search_path = public
as $$
    select t.*
    from public.themes t
    join public.theme_purchases tp
      on tp.theme_id = t.id
    where tp.user_id = auth.uid()
    order by tp.purchased_at desc;
$$;

create or replace function public.creator_update_theme(
    target_theme_id uuid,
    new_title text,
    new_description text,
    new_category text,
    new_os_version text,
    new_drive_url text,
    new_coin_price integer
)
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
    actor_role text;
    theme_owner uuid;
begin
    if auth.uid() is null then
        raise exception 'Chưa đăng nhập';
    end if;

    select role
    into actor_role
    from public.profiles
    where id = auth.uid();

    if actor_role not in (
        'creator',
        'admin',
        'super_admin'
    ) then
        raise exception 'Bạn không có quyền chỉnh sửa theme';
    end if;

    select owner_id
    into theme_owner
    from public.themes
    where id = target_theme_id;

    if theme_owner is null then
        raise exception 'Không tìm thấy theme';
    end if;

    if actor_role = 'creator'
       and theme_owner <> auth.uid() then
        raise exception 'Bạn chỉ được sửa theme của chính mình';
    end if;

    if trim(coalesce(new_title, '')) = '' then
        raise exception 'Tên theme không được để trống';
    end if;

    if trim(coalesce(new_drive_url, '')) <> ''
       and trim(new_drive_url) not like 'https://%' then
        raise exception 'Link tải phải bắt đầu bằng https://';
    end if;

    update public.themes
    set title = trim(new_title),
        description = trim(coalesce(new_description, '')),
        category = trim(coalesce(new_category, '')),
        os_version = trim(coalesce(new_os_version, '')),
        drive_url = trim(coalesce(new_drive_url, '')),
        coin_price = greatest(coalesce(new_coin_price, 0), 0),
        status = 'pending',
        reject_reason = '',
        updated_at = now()
    where id = target_theme_id;

    return jsonb_build_object(
        'theme_id', target_theme_id,
        'status', 'pending'
    );
end;
$$;

drop function if exists public.creator_remove_theme(uuid);

revoke all on function public.purchase_theme_v2(uuid)
from public, anon, authenticated;
revoke all on function public.get_owned_theme_ids()
from public, anon, authenticated;
revoke all on function public.get_owned_themes()
from public, anon, authenticated;
revoke all on function public.creator_update_theme(
    uuid, text, text, text, text, text, integer
) from public, anon, authenticated;

grant execute on function public.purchase_theme_v2(uuid)
to authenticated;
grant execute on function public.get_owned_theme_ids()
to authenticated;
grant execute on function public.get_owned_themes()
to authenticated;
grant execute on function public.creator_update_theme(
    uuid, text, text, text, text, text, integer
) to authenticated;

notify pgrst, 'reload schema';

commit;

select 'M4X v3.9.1 installed successfully' as result;
