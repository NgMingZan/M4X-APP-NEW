# M4X Theme v4 Hybrid OTA

## Cách hoạt động
- APK là WebView shell cố định.
- Giao diện/logic nằm trong thư mục `web/` và được GitHub Pages phát hành.
- Push thay đổi `web/**` => online ngay, không cài lại APK.
- Nếu website lỗi hoặc chưa cấu hình, app dùng bản dự phòng trong `app/src/main/assets/web/`.

## Thiết lập một lần
1. GitHub repo > Settings > Pages > Source: GitHub Actions.
2. Tạo Actions secret `WEB_APP_URL` với URL Pages, ví dụ:
   `https://ngmingzan.github.io/M4X-APP-NEW`
3. Giữ `SUPABASE_URL` và `SUPABASE_ANON_KEY` như hiện tại.
4. Chạy workflow build APK và cài bản v4 một lần.

## Các lần cập nhật sau
Chỉ sửa file trong `web/`, commit và push. Workflow `Deploy M4X Online UI` tự chạy. Người dùng mở lại app hoặc bấm làm mới.

## Khi nào vẫn cần APK mới
Quyền Android, WebView native, file chooser, DownloadManager, thông báo hệ thống, PiP, Java/Kotlin.
