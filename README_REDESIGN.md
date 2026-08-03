# M4X Theme v2.1 — HyperOS Redesign

Bản thiết kế lại toàn bộ giao diện ứng dụng:

- Trang Khám phá có banner gradient, thống kê, tìm kiếm, bộ lọc và thẻ theme có ảnh.
- Trang Đăng theme hỗ trợ file `.mtz/.zip` hoặc link Google Drive khi file lớn.
- Người dùng có thể tải tối đa 5 ảnh xem trước.
- Kho của tôi hiển thị rõ trạng thái chờ duyệt, đã duyệt và bị từ chối.
- Bảng Admin có thẻ thống kê, ảnh theme, nút duyệt/từ chối và quản lý quyền.
- Hồ sơ có ảnh đại diện, số theme, lượt tải, điểm và thông tin cập nhật online.
- Đã thêm safe insets, cuộn và `imePadding` để không che chữ hoặc ô nhập.

## Bắt buộc chạy SQL

Mở Supabase > SQL Editor, dán toàn bộ `SUPABASE_V2_SETUP.sql` rồi Run.
SQL mới bổ sung:

- `drive_url`
- `preview_urls`
- `tags`
- `admin_note`
- quyền Storage cho ảnh JPEG/PNG/WebP

## GitHub Secrets

- `SUPABASE_URL` chỉ có dạng `https://PROJECT.supabase.co`
- `SUPABASE_ANON_KEY` dùng publishable key `sb_publishable_...`

## OTA

Theme, banner, dữ liệu, trạng thái duyệt và quyền Admin cập nhật online. Thay đổi giao diện Kotlin/Compose như bản này cần build APK mới một lần.
