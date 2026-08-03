# M4X Theme v4 Full OTA

- APK chỉ là WebView native, giữ hỗ trợ chọn file, video toàn màn hình và PiP.
- Toàn bộ giao diện/chức năng nằm trong `web/` và được triển khai qua GitHub Pages.
- Sau khi sửa file trong `web/` và push, người dùng mở lại app sẽ nhận bản mới mà không cài APK.
- Chỉ cần APK mới khi sửa Android native, quyền hệ thống, WebView, PiP hoặc Kotlin.

## Cài đặt
1. Giải nén project vào repo và push.
2. Chờ hai workflow `Deploy M4X v4 Full OTA Online` và `Build M4X v4 Full OTA APK` xanh.
3. Cài APK artifact v4.0.0 một lần.
4. Những lần cập nhật sau chỉ sửa `web/` rồi push.

## Chức năng web đã có
Đăng nhập/đăng ký, khám phá theme, chi tiết/mua/tải riêng từng theme, đăng theme và tối đa 5 ảnh, nhiệm vụ, giftcode, mở rương, bảng đóng góp, M4X WEB, quản lý link bởi Admin, hồ sơ, duyệt theme và tải lại bản online.
