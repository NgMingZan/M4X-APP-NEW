# M4X Theme v3.0.1 Fix

## Lỗi đã sửa

- Thiếu cột `coin_price`.
- Thiếu bảng `giftcodes`.
- Thiếu bảng `airdrops`.
- Lỗi cột cũ `name` bắt buộc nhưng app chỉ gửi `title`.
- SQL chạy lại bị dừng vì policy đã tồn tại.
- PostgREST chưa cập nhật schema cache.
- App upload giờ gửi cả trường cũ và trường mới để tương thích database đã tạo trước đó.

## Thứ tự bắt buộc

1. Chạy toàn bộ `SUPABASE_V3_FIX_ALL.sql` trong Supabase SQL Editor.
2. Chờ dòng `Success. No rows returned`.
3. Đóng hẳn app và mở lại để kiểm tra database online.
4. Push source v3.0.1 lên GitHub và build APK mới để nhận phần sửa tương thích upload.
