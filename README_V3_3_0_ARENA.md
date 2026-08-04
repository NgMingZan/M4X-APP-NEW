# M4X Theme v3.3.0 — M4X Arena Prototype

Bản thử nghiệm tích hợp một game bắn súng góc nhìn từ trên xuống trực tiếp
trong `M4X GAME CENTER`.

## Nội dung đã có

- Một bản đồ đấu trường tối đa 10 nhân vật.
- Bản thử nghiệm dùng 1 người chơi thật và tự thêm 9 bot.
- Trận kéo dài 3 phút hoặc kết thúc khi có người đạt 20 mạng.
- Nhân vật hồi sinh sau 3 giây.
- Joystick bên trái để di chuyển.
- Nút bắn bên phải, tự khóa mục tiêu gần nhất có đường ngắm.
- Nút thay đạn và dùng medkit.
- Máu, giáp, đạn, đồng hồ, bảng xếp hạng và kết quả cuối trận.
- Hiệu ứng vệt đạn, đầu nòng, va chạm, giáp vỡ và máu.
- Vật phẩm trên bản đồ: cứu thương, giáp và đạn.
- Tự chuyển ngang màn hình và ẩn thanh hệ thống khi vào trận.

## Bot chiến thuật

Bot dùng state machine thay vì chỉ chạy thẳng:

- Chọn mục tiêu theo khoảng cách, lượng máu, đường ngắm và đối thủ vừa gây sát thương.
- Ngắm đón theo vận tốc mục tiêu và tốc độ viên đạn.
- Né viên đạn đang bay tới.
- Di chuyển ngang khi giao tranh.
- Giữ khoảng cách phù hợp với từng loại súng.
- Flank khi mục tiêu bị vật cản che.
- Tìm vị trí núp khi ít máu hoặc đang thay đạn.
- Dùng medkit và tìm vật phẩm phù hợp.
- Có độ chính xác, phản ứng và trang bị khác nhau.

Đây là bot chiến thuật nâng cao chạy trên thiết bị, chưa phải mô hình machine
learning và chưa phải bot authoritative trên game server.

## Cửa hàng Arena

Sau khi chạy `SUPABASE_V3_3_0_ARENA_SHOP.sql`, cửa hàng có:

- SMG-7 Neon
- P90-X Plasma
- Viper-S Sniper
- Giáp MK-II
- Giày phản lực
- Túi cứu thương

Vật phẩm dùng hệ thống `shop_items`, `user_inventory`,
`purchase_shop_item` và `equip_inventory_item` hiện có. Tiền được trừ bằng
M4X Coin trên Supabase, không trừ cục bộ trong APK.

## Giới hạn bản thử nghiệm

- Chưa kết nối người chơi online thực.
- Chưa có game server authoritative.
- Kết quả trận chưa cộng M4X Coin.
- Lịch sử trận không cho client tự ghi để tránh giả kết quả.
- Cần kiểm tra hiệu năng và điều khiển trên APK thật.

Bước tiếp theo sau khi gameplay ổn định là nối matchmaking và match server,
sau đó thay dần bot bằng người chơi thật nhưng vẫn giữ đủ 10 vị trí.

## Phiên bản

```text
versionCode = 46
versionName = 3.3.0
```
