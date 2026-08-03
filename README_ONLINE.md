# M4X Theme Online – Supabase

Bản này thay dữ liệu mẫu/offline bằng Supabase cho các luồng cốt lõi:

- Đăng ký, đăng nhập online.
- Profile và vai trò `user / creator / admin / super_admin / banned`.
- App mới không có theme mẫu.
- Chọn file `.mtz` hoặc `.zip` từ bộ nhớ máy.
- Upload trực tiếp vào bucket Supabase Storage `themes`.
- Ghi theme vào bảng `themes` với `approved = false`.
- Admin xem hàng chờ, duyệt hoặc từ chối.
- Chỉ theme đã duyệt xuất hiện trong Khám phá.
- Super Admin bổ nhiệm hoặc hạ quyền Admin bằng hàm `set_user_role`.

## 1. Chạy SQL

Mở Supabase → SQL Editor và chạy file `SUPABASE_SQL_FINAL.sql`.

## 2. Lấy URL và khóa

Supabase → Project Settings → API:

- Project URL
- Publishable key hoặc anon public key

Chép `supabase.properties.example` thành `supabase.properties`, sau đó điền hai giá trị.
Không dùng `service_role` trong APK.

## 3. Build bằng GitHub Actions

Repository → Settings → Secrets and variables → Actions → New repository secret:

- `SUPABASE_URL`
- `SUPABASE_ANON_KEY`

Sau đó vào Actions → `Build M4X Theme Online APK` → Run workflow.

## Lưu ý tài khoản minhdan

Trigger đã tạo trước đó chỉ cấp `super_admin` khi metadata đăng ký có username chính xác `minhdan`.
Hãy đăng ký tài khoản đầu tiên với username `minhdan`. Nếu đã có tài khoản nhưng role chưa đúng, chỉnh một lần trong Table Editor hoặc SQL Editor bằng UID tài khoản đó.
