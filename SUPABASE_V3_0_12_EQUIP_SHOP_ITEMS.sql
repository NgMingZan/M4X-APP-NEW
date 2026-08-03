-- ============================================================
-- M4X THEME v3.0.12
-- Sử dụng / bỏ sử dụng vật phẩm đã mua trong Cửa hàng M4X
-- Không xóa dữ liệu cũ.
-- ============================================================

begin;

-- Lưu trạng thái đang dùng và thông tin hiển thị ngay trong kho cá nhân.
alter table public.user_inventory
  add column if not exists equipped boolean not null default false;

alter table public.user_inventory
  add column if not exists item_metadata jsonb not null default '{}'::jsonb;

alter table public.user_inventory
  add column if not exists item_image_url text not null default '';

-- Bổ sung metadata cho các vật phẩm đã mua từ trước.
update public.user_inventory ui
set item_metadata = coalesce(si.metadata, '{}'::jsonb),
    item_image_url = coalesce(si.image_url, '')
from public.shop_items si
where ui.item_id = si.id
  and (
    ui.item_metadata = '{}'::jsonb
    or ui.item_image_url = ''
  );

-- Mỗi loại chỉ được kích hoạt một vật phẩm cùng lúc.
create unique index if not exists user_inventory_one_equipped_per_type
  on public.user_inventory(user_id, item_type)
  where equipped = true;

-- Cập nhật hàm mua để lưu metadata và ảnh vật phẩm.
create or replace function public.purchase_shop_item(p_item_id uuid)
returns bigint
language plpgsql
security definer
set search_path = public
as $$
declare
  item_row public.shop_items%rowtype;
  current_balance bigint;
begin
  if auth.uid() is null then
    raise exception 'Chưa đăng nhập';
  end if;

  select * into item_row
  from public.shop_items
  where id = p_item_id and active = true
  for update;

  if item_row.id is null then
    raise exception 'Vật phẩm không tồn tại hoặc đã tắt bán';
  end if;

  if exists (
    select 1
    from public.user_inventory
    where user_id = auth.uid()
      and item_id = p_item_id
  ) then
    raise exception 'Bạn đã sở hữu vật phẩm này';
  end if;

  select points into current_balance
  from public.profiles
  where id = auth.uid()
  for update;

  if current_balance is null then
    raise exception 'Không tìm thấy hồ sơ';
  end if;

  if current_balance < item_row.price then
    raise exception 'Không đủ M4X COIN';
  end if;

  if item_row.price > 0 then
    perform public.add_coin(
      auth.uid(),
      -item_row.price,
      'shop_purchase',
      item_row.id::text
    );
  end if;

  insert into public.user_inventory(
    user_id,
    item_id,
    item_name,
    item_type,
    equipped,
    item_metadata,
    item_image_url
  )
  values(
    auth.uid(),
    item_row.id,
    item_row.name,
    item_row.item_type,
    false,
    coalesce(item_row.metadata, '{}'::jsonb),
    coalesce(item_row.image_url, '')
  );

  select points into current_balance
  from public.profiles
  where id = auth.uid();

  return current_balance;
end
$$;

-- Bật hoặc tắt một vật phẩm.
-- Khi bật, vật phẩm cùng loại đang dùng trước đó sẽ tự tắt.
create or replace function public.equip_inventory_item(p_inventory_id uuid)
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
  inventory_row public.user_inventory%rowtype;
  next_state boolean;
begin
  if auth.uid() is null then
    raise exception 'Chưa đăng nhập';
  end if;

  select * into inventory_row
  from public.user_inventory
  where id = p_inventory_id
    and user_id = auth.uid()
  for update;

  if inventory_row.id is null then
    raise exception 'Bạn không sở hữu vật phẩm này';
  end if;

  next_state := not inventory_row.equipped;

  if next_state then
    update public.user_inventory
    set equipped = false
    where user_id = auth.uid()
      and item_type = inventory_row.item_type
      and equipped = true;

    update public.user_inventory
    set equipped = true
    where id = inventory_row.id
      and user_id = auth.uid();
  else
    update public.user_inventory
    set equipped = false
    where id = inventory_row.id
      and user_id = auth.uid();
  end if;

  return jsonb_build_object(
    'equipped', next_state,
    'inventory_id', inventory_row.id,
    'item_name', inventory_row.item_name,
    'item_type', inventory_row.item_type
  );
end
$$;

revoke all on function public.purchase_shop_item(uuid) from public, anon;
revoke all on function public.equip_inventory_item(uuid) from public, anon;

grant execute on function public.purchase_shop_item(uuid) to authenticated;
grant execute on function public.equip_inventory_item(uuid) to authenticated;

notify pgrst, 'reload schema';

commit;
