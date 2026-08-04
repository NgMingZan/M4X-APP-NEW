# M4X Theme v3.6.4 — Arena Realtime 401 Fix

## Lỗi đã sửa
Video ghi nhận Arena vào trận nhưng WebSocket báo:
`Expected HTTP 101 response but was 401 Unauthorized`.

## Nguyên nhân
ArenaOnline.kt gửi access token người dùng trong header Authorization ngay khi
WebSocket handshake. Token phiên cũ/hết hạn khiến Supabase từ chối trước khi
kênh public Broadcast được join.

## Cách sửa
- Bỏ Authorization Bearer khỏi WebSocket handshake.
- Gửi Supabase API key qua query `apikey` và header `apikey`.
- Bỏ access_token khỏi phx_join của public Broadcast.
- RPC ghép trận, kết thúc trận và nhận M4X Coin vẫn dùng Session đăng nhập.
- Không cần SQL mới.
