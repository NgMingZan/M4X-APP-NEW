# M4X Theme 1.0 – Complete Android Project

Ứng dụng Android Native bằng Kotlin + Jetpack Compose, giữ cấu trúc nguồn M4X ban đầu.

## Chức năng đã có trong APK
- Đăng nhập/đăng xuất mô phỏng và chuyển vai trò User, Creator, Admin.
- Trang chủ, banner, theme nổi bật/mới/top tải/top đánh giá/cập nhật.
- Tìm kiếm và lọc theo HyperOS, MIUI, danh mục.
- Chi tiết theme, lượt xem, tải, yêu thích, theo dõi creator.
- Đánh giá 1–5 sao, bình luận và báo lỗi.
- Đăng theme `.mtz/.zip`, kiểm tra tên file/kích thước và trạng thái duyệt.
- Hồ sơ, theme đã đăng, lịch sử hoạt động và thông báo.
- Điểm thưởng, nhiệm vụ và nhận thưởng.
- Admin dashboard, thống kê, duyệt/từ chối/xóa/nổi bật theme.
- Quản lý người dùng, khóa/mở khóa và gửi thông báo toàn hệ thống.
- Kiểm tra OTA mô phỏng.
- Room database để app hoạt động offline và build ngay.

## Thành phần backend đi kèm
- `firestore.rules`
- `storage.rules`
- `firebase.json`
- `FIREBASE_SETUP.md`

Firebase thật cần `google-services.json` của chủ dự án và không thể được tạo thay bằng dữ liệu giả.

## Build APK bằng GitHub Actions
Push lên nhánh `main`, mở Actions, chạy `Build M4X Theme APK`, tải artifact `M4X-Theme-APK`.
