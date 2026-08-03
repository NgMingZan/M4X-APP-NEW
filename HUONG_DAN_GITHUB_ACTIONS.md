# Build APK bằng GitHub Actions trên điện thoại

1. Tạo repository mới trên GitHub.
2. Giải nén dự án này trên điện thoại.
3. Tải toàn bộ file và thư mục trong dự án lên repository. Phải giữ nguyên thư mục `.github/workflows/`.
4. Mở tab **Actions** của repository.
5. Chọn **Build M4X Theme APK**.
6. Nhấn **Run workflow** rồi xác nhận **Run workflow**.
7. Khi xuất hiện dấu tích xanh, mở lần chạy đó.
8. Kéo xuống phần **Artifacts** và tải **M4X-Theme-APK**.
9. Giải nén file tải về để nhận `M4X-Theme-debug.apk`, sau đó cài đặt.

Workflow cũng tự chạy khi có mã nguồn mới được đẩy lên nhánh `main` hoặc `master`.
