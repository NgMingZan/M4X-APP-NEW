# M4X Theme v3.6.2 — Fishing API Hotfix

Hotfix sửa lỗi GitHub Actions `compileDebugKotlin`.

## Nguyên nhân
Patch v3.6.1 có FishingGame.kt và FishingArcadeArt.kt nhưng thiếu SupabaseApi.kt mới.
Vì vậy Kotlin không tìm thấy FishingGameState, FishingMapInfo, FishingRodInfo,
FishingCastStart, FishingCastFinish và các RPC Fishing.

## Đã sửa
- Bổ sung SupabaseApi.kt đầy đủ model và RPC Fishing.
- Nâng versionCode lên 55.
- Nâng versionName lên 3.6.2.
- Không cần chạy SQL mới.
