# M4X Theme v2 Clean

Ứng dụng Android Native Kotlin + Jetpack Compose, kết nối Supabase trực tiếp.

## Có sẵn
- Đăng ký/đăng nhập Supabase.
- `minhdan` trở thành Super Admin đầu tiên.
- App không có dữ liệu mẫu.
- Chọn file `.mtz/.zip` bằng trình chọn file Android.
- Upload vào bucket `themes`, trạng thái `pending`.
- Admin duyệt/từ chối; chỉ `approved` xuất hiện công khai.
- Super Admin bổ nhiệm/hạ quyền Admin.
- Banner, theme, quyền và cấu hình cập nhật online từ Supabase.
- Kiểm tra phiên bản APK online qua bảng `app_config`.

## Giới hạn OTA
Không cần cài lại APK khi thay theme, banner, thông báo, quyền, trạng thái duyệt hoặc link cập nhật.
Thay đổi mã Kotlin, quyền Android, WebView/native hoặc SDK vẫn cần build APK mới. App sẽ đọc `app_config.update_url` để báo người dùng tải bản mới.

## Chuẩn bị Supabase
1. SQL Editor → chạy toàn bộ `SUPABASE_V2_SETUP.sql`.
2. Authentication → bật Email/Password.
3. Bucket `themes` sẽ được SQL tạo/cấu hình public, giới hạn 100 MB.

## GitHub Secrets
Trong repo app tạo:
- `SUPABASE_URL`
- `SUPABASE_ANON_KEY` (publishable/anon key, không dùng service_role)

## Cập nhật banner và phiên bản online
Trong Supabase Table Editor → `app_config` → dòng `main`:
- `home_banner_title`, `home_banner_subtitle`: đổi banner chữ ngay.
- `latest_version_code`: mã phiên bản mới, phải lớn hơn APK hiện tại (20).
- `latest_version_name`: ví dụ `2.1.0`.
- `update_url`: link APK/Release.
- `update_message`: nội dung cập nhật.
- `force_update`: dữ liệu đã có; giao diện hiện tại chỉ hiển thị nút cập nhật, chưa khóa cưỡng bức.
