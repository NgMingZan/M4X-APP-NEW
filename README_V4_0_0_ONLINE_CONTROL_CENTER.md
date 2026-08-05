# M4X Theme v4.0.0 — Online Control Center

## Chức năng cập nhật online

Sau khi cài APK v4.0.0 một lần, Admin có thể thay đổi trực tiếp trong
**Admin → Online**:

- Banner trang chủ
- Thông báo trong ứng dụng
- Nhiệm vụ hằng ngày và Coin thưởng
- Chuỗi điểm danh và quà từng ngày
- Phí, phần thưởng và tỉ lệ vòng quay
- Bật/tắt M4X Fishing
- Hệ số giá cá
- Hệ số máu Boss
- Theme nổi bật

Giftcode tiếp tục được quản lý tại tab **Giftcode**.

## Cách đồng bộ

Ứng dụng tải lại cấu hình online khoảng mỗi 60 giây. Khi Admin lưu,
giao diện Admin được cập nhật ngay; các tài khoản khác nhận thay đổi mà
không cần cài lại APK.

## Supabase

Bắt buộc chạy:

`SUPABASE_V4_0_0_ONLINE_CONTROL_CENTER.sql`

SQL tạo bảng cấu hình, nhiệm vụ online, điểm danh, vòng quay và trigger
nhân giá cá theo cấu hình.
