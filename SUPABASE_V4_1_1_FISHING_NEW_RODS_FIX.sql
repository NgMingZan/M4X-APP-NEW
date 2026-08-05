-- ============================================================
-- M4X FISHING v4.1.1
-- KÍCH HOẠT LẠI 5 CẦN MỚI VÀ KIỂM TRA DANH MỤC
--
-- Chạy toàn bộ file trong Supabase SQL Editor.
-- ============================================================

begin;

insert into public.fishing_rods(
  code,
  name,
  description,
  price,
  power,
  stability,
  luck,
  sort_order,
  active
)
values
  (
    'ocean_wave',
    'Cần Sóng Biển',
    'Cân bằng, dễ kiểm soát và phù hợp người chơi mới nâng cấp.',
    500,
    2,
    3,
    2,
    6,
    true
  ),
  (
    'storm_hunter',
    'Cần Săn Bão',
    'Lực kéo mạnh, chuyên trị cá nhanh và cá có sức giãy lớn.',
    1100,
    4,
    2,
    2,
    7,
    true
  ),
  (
    'ice_guardian',
    'Cần Hộ Vệ Băng',
    'Độ ổn định cao, phù hợp Vịnh Băng Giá và cá nặng.',
    1500,
    3,
    5,
    2,
    8,
    true
  ),
  (
    'fortune_koi',
    'Cần Cá Koi May Mắn',
    'Tăng cơ hội gặp cá hiếm, dành cho người thích săn bộ sưu tập.',
    1800,
    3,
    3,
    5,
    9,
    true
  ),
  (
    'dragon_emperor',
    'Cần Long Đế',
    'Cần cao cấp với lực kéo lớn, ổn định tốt và may mắn cao.',
    2800,
    5,
    4,
    4,
    10,
    true
  )
on conflict (code) do update set
  name = excluded.name,
  description = excluded.description,
  price = excluded.price,
  power = excluded.power,
  stability = excluded.stability,
  luck = excluded.luck,
  sort_order = excluded.sort_order,
  active = true;

notify pgrst, 'reload schema';

commit;

select
  code,
  name,
  price,
  power,
  stability,
  luck,
  active
from public.fishing_rods
where code in (
  'ocean_wave',
  'storm_hunter',
  'ice_guardian',
  'fortune_koi',
  'dragon_emperor'
)
order by sort_order;

select
  count(*) as new_rods_active
from public.fishing_rods
where code in (
  'ocean_wave',
  'storm_hunter',
  'ice_guardian',
  'fortune_koi',
  'dragon_emperor'
)
and active = true;
