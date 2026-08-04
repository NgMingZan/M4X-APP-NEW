# M4X Theme v3.2.0 — Rust Security Suite

Bản này được nâng cấp trực tiếp từ source v3.1.1 đã sửa Cargo path.

## Ba chức năng Rust mới

### 1. Đọc metadata và cấu trúc theme

Khi chọn `.mtz/.zip`, Rust tự đọc:

- Tên theme.
- Tác giả hoặc nhà thiết kế.
- Phiên bản theme.
- UI version/nền tảng nếu metadata có cung cấp.
- Các module như Lockscreen, Icons, SystemUI, Framework, Launcher, Wallpaper, Font, âm thanh và đồng hồ.

Tên theme và mô tả có thể được tự điền vào biểu mẫu đăng theme.

### 2. Xác minh SHA-256 sau khi tải

Khi Admin duyệt, Supabase khóa SHA-256 của file tại thời điểm duyệt.

Khi người dùng tải theme trực tiếp từ Supabase Storage:

1. App tải file vào thư mục riêng.
2. Rust tính lại SHA-256.
3. So sánh với `approved_file_sha256`.
4. Chỉ mở file khi khớp.
5. File sai hash bị xóa ngay.

Link Google Drive vẫn mở bên ngoài nên chưa thể xác minh tự động.

### 3. Điểm an toàn và báo cáo Admin

Rust chấm từ 0–100 dựa trên:

- Lỗi/cảnh báo.
- Đường dẫn nguy hiểm.
- File thực thi.
- ZIP bomb.
- Metadata.
- Module nhận diện.
- Số lượng mục và cấu trúc gói.

Admin thấy:

- Điểm an toàn.
- Metadata.
- Module.
- Findings, warnings, errors.
- SHA-256 đã khóa khi duyệt.

Nút **Duyệt** bị khóa khi Rust từ chối file hoặc điểm dưới 60.

## Cài đặt

### Bước 1 — Chạy SQL

Supabase → SQL Editor → chạy:

```text
SUPABASE_V3_2_0_RUST_SECURITY_SUITE.sql
```

### Bước 2 — Chép source vào repository

Trên Termux:

```bash
cd ~/storage/downloads
unzip -o M4X_THEME_V3_2_0_RUST_SECURITY_SUITE.zip

cp -a M4X_THEME_V3_2_0_RUST_SECURITY_SUITE/M4X_THEME_V3_UNIVERSE/. \
~/storage/downloads/M4X-APP-NEW-RUST/

cd ~/storage/downloads/M4X-APP-NEW-RUST
git config --global --add safe.directory "$PWD"
git add .
git commit -m "Add Rust security suite v3.2.0"
git push origin main
```

### Bước 3 — Build

GitHub → Actions → **Build M4X Theme v3.2.0 Rust Security Suite APK**

Artifact:

```text
M4X-Theme-v3.2.0-Rust-Security-APK
```

## Lưu ý bảo mật

Kết quả quét trên app vẫn là kiểm tra phía client. Người sửa APK có thể bỏ qua bước này. Admin vẫn phải duyệt; các thao tác coin và quyền phải tiếp tục xử lý bằng Supabase RPC.


## Chi tiết kiểm soát tải xuống

Đối với file nằm trên Supabase Storage, ứng dụng dùng đồng thời:

- `approved_file_sha256`: SHA-256 được khóa lúc Admin chuyển theme sang `approved`.
- `approved_file_size_bytes`: dung lượng được khóa cùng thời điểm.
- Rust tính lại SHA-256 trên file vừa tải.
- App so sánh cả SHA-256 và dung lượng.
- File không khớp bị xóa, không được mở.
- File hợp lệ được lưu trong `Android/data/<package>/files/Download/M4XThemes`.

Ứng dụng sử dụng Android `FileProvider` để cấp quyền đọc tạm thời khi mở file đã xác minh.

## Kiểm soát duyệt

Migration v3.2.0 chặn chuyển sang `approved` khi:

- Rust đã trả trạng thái `failed`.
- File tải trực tiếp có điểm an toàn từ 1–59.

Theme dùng link Drive và chưa có báo cáo vẫn có thể được Admin kiểm tra thủ công.

## Các file chính đã thay đổi

```text
rust/m4x_theme_core/src/lib.rs
app/src/main/java/com/m4xtheme/app/rust/RustThemeValidator.kt
app/src/main/java/com/m4xtheme/app/SupabaseApi.kt
app/src/main/java/com/m4xtheme/app/MainActivity.kt
app/src/main/AndroidManifest.xml
app/src/main/res/xml/file_paths.xml
SUPABASE_V3_2_0_RUST_SECURITY_SUITE.sql
.github/workflows/build-apk.yml
```

## Phiên bản

```text
versionCode = 45
versionName = 3.2.0
```
