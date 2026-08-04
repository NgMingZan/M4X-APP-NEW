# M4X Theme v3.6.3 — Arena + Fishing Compile Fix

## Lỗi trong log 83964420340
- `ArenaGame.kt`: thiếu `joinArenaMatch`, `arenaMatchStatus`, `leaveArenaMatch`, `finishArenaMatch`, `claimArenaReward`.
- `FishingGame.kt`: Material3 hiện tại không hỗ trợ tham số `border` của `ElevatedCard`.

## Đã sửa
- Gộp Arena Online RPC và Fishing RPC trong cùng `SupabaseApi.kt`.
- Bổ sung `ArenaOnline.kt` chứa model và Realtime room.
- Chuyển 3 viền `ElevatedCard` sang `Modifier.border(...)`.
- Nâng versionCode 56, versionName 3.6.3.
- Không cần chạy SQL mới.
