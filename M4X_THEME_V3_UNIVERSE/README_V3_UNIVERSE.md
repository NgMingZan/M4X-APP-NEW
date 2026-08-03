# M4X Theme v3 Universe

Bản thiết kế lại toàn app với M4X COIN, Quest Map, Giftcode, Airdrop, sự kiện online, kho vật phẩm, bảng xếp hạng và M4X WEB.

## Trước khi build
1. Chạy toàn bộ `SUPABASE_V2_SETUP.sql` trong Supabase SQL Editor.
2. Giữ GitHub Secrets `SUPABASE_URL` và `SUPABASE_ANON_KEY`.
3. Push source lên GitHub; workflow tạo artifact `M4X-Theme-v3-Universe-APK`.

## Update online
Admin có thể phát hành Event, Giftcode, Airdrop, Quest, giá theme, vật phẩm và banner bằng Supabase mà không cần APK mới. M4X WEB mở website trực tiếp trong app. Thay đổi mã Kotlin/WebView native vẫn cần APK mới.
