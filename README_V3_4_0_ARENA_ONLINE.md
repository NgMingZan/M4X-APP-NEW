# M4X Theme v3.4.0 — Arena Online Beta

## Đã có

- Ghép tối đa 10 người thật trong cùng một phòng.
- Tìm người trong 8 giây; vị trí còn thiếu tự thêm bot.
- Chủ phòng mô phỏng vị trí, đạn, sát thương, bot và vật phẩm.
- Người chơi khác gửi joystick, bắn, thay đạn và hồi máu qua Supabase Realtime Broadcast.
- Chủ phòng gửi snapshot chung 10 lần/giây.
- Cùng một bản đồ, cùng bảng điểm và cùng kết quả trên các thiết bị.
- M4X Coin được cộng bằng PostgreSQL RPC, không cộng trực tiếp trong APK.
- Chống nhận thưởng lặp bằng khóa `(match_id, user_id)` và `claimed_at`.
- Trận dưới 45 giây không được nhận thưởng.
- Tối đa 6 trận có thưởng trong một giờ.
- Tự phát hành `M4X-Theme.apk` lên GitHub Release khi push tag `v3.4.0`.

## Công thức thưởng

- Tham gia: 35 Coin.
- Hạ gục: tối đa 60 Coin.
- Hạng 1: thêm 100 Coin.
- Hạng 2–3: thêm 60 Coin.
- Hạng 4–6: thêm 25 Coin.
- Tối đa thực tế: 200 Coin mỗi trận.
- Coin chỉ cộng một lần sau khi chủ phòng xác nhận kết quả.

## Kiến trúc

```text
Supabase RPC
├── arena_join_match
├── arena_match_status
├── arena_leave_match
├── arena_finish_match
└── arena_claim_reward

Supabase Realtime
├── arena_input      — điều khiển người chơi
├── arena_snapshot   — trạng thái trận từ host
└── match_finished   — kết quả cuối trận
```

`ArenaRealtimeRoom` sử dụng trực tiếp giao thức Phoenix WebSocket của Supabase, nên không thêm SDK Realtime mới vào Gradle.

## Cần chạy SQL

Chạy toàn bộ:

```text
SUPABASE_V3_4_0_ARENA_ONLINE.sql
```

trong Supabase Dashboard → SQL Editor.

## Hạn chế của Online Beta

- Chủ phòng hiện là một điện thoại trong trận, chưa phải dedicated game server.
- Nếu chủ phòng thoát hoặc mất mạng, trận bị hủy và không cộng Coin.
- Người dùng sửa APK vẫn có thể cố làm sai kết quả; SQL giới hạn thời gian, mức thưởng, tần suất và nhận trùng nhưng chưa thể chống gian lận tuyệt đối.
- Để phát hành quy mô lớn và thi đấu xếp hạng nghiêm túc, cần chuyển mô phỏng trận sang game server riêng.

## Phiên bản

```text
versionCode = 49
versionName = 3.4.0
```
