# M4X Theme Android App

Dự án Android native sử dụng Kotlin và Jetpack Compose.

## Chức năng hiện có
- Trang chủ và danh sách theme.
- Chi tiết theme, lượt xem, tải xuống và yêu thích.
- Đăng theme mới.
- Bảng quản trị duyệt hoặc từ chối theme.
- Hồ sơ người dùng, thông báo và phần thưởng.
- Lưu dữ liệu cục bộ bằng Room.
- Kiểm tra cập nhật OTA ở mức mô phỏng.

## Build APK trên Android Studio
1. Giải nén dự án.
2. Mở Android Studio, chọn **Open** và chọn thư mục dự án.
3. Chờ **Gradle Sync** hoàn tất. Android Studio có thể đề nghị tải JDK/SDK còn thiếu.
4. Chọn **Build > Build APK(s)**.
5. APK debug nằm tại `app/build/outputs/apk/debug/app-debug.apk`.

## APK phát hành
Chọn **Build > Generate Signed App Bundle or APK > APK**, sau đó tạo hoặc chọn keystore của bạn.

## Lưu ý
- Đây là app demo dùng dữ liệu cục bộ, chưa kết nối máy chủ/Firebase thật.
- File `.mtz/.zip` hiện mới được mô phỏng trong giao diện; cần Firebase Storage hoặc máy chủ riêng để tải lên và tải xuống thật.
- Không cần Gemini API key cho các chức năng hiện tại.

## Build APK trên điện thoại bằng GitHub Actions

Dự án đã có workflow tại `.github/workflows/build-apk.yml`. Xem hướng dẫn chi tiết trong `HUONG_DAN_GITHUB_ACTIONS.md`.
