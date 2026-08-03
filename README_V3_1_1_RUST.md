# M4X Theme v3.1.1 — Rust Validation

Bản này được nâng cấp trực tiếp từ cấu trúc `M4X_THEME_V3_1_0_ARCADE_TREASURE_PET`.

## Thay đổi

- Tăng `versionCode` từ 43 lên 44.
- Tăng `versionName` từ 3.1.0 lên 3.1.1.
- Giữ nguyên toàn bộ Arcade, Kho báu và Thú cưng của v3.1.0.
- Thêm thư viện Rust `m4x_theme_core`.
- Kotlin gọi Rust qua JNI trước khi upload `.mtz/.zip`.
- Admin nhìn thấy trạng thái kiểm tra, dung lượng và 12 ký tự đầu SHA-256.
- File lỗi bị chặn trước khi upload.
- File cảnh báo vẫn vào trạng thái `pending` để Admin kiểm tra.
- Link Google Drive có trạng thái `unchecked` vì app không tải toàn bộ link về để quét.

## Kiểm tra Rust

- ZIP/MTZ có mở được hay không.
- Giới hạn 100 MB.
- SHA-256.
- Đường dẫn tuyệt đối hoặc `../`.
- Symbolic link.
- File APK, DEX, EXE, DLL, SO, BAT, CMD, MSI và PowerShell.
- Số lượng file, độ sâu thư mục.
- Tổng dung lượng sau giải nén.
- Tỷ lệ nén bất thường có nguy cơ ZIP bomb.

## Cài Supabase

Mở Supabase → SQL Editor và chạy:

`SUPABASE_V3_1_1_RUST_VALIDATION.sql`

## Build APK

1. Đẩy toàn bộ source lên repository GitHub.
2. Giữ hai GitHub Secret: `SUPABASE_URL`, `SUPABASE_ANON_KEY`.
3. Vào Actions.
4. Chạy `Build M4X Theme v3.1.1 Rust Validation APK`.
5. Tải artifact `M4X-Theme-v3.1.1-Rust-Validation-APK`.

## Lưu ý bảo mật

Kết quả `client_validation_*` được tạo trên điện thoại, vì vậy không được coi là chứng thực tuyệt đối. Theme vẫn luôn được tạo với `status = pending` và Admin là lớp duyệt cuối cùng. Muốn kiểm soát mạnh hơn nữa, bước tiếp theo là quét lại file độc lập trên server/Edge Function.
