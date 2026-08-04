# M4X Theme v3.3.1 — Arena Redesign

## Đã sửa

- Khi vào trận, thanh tiêu đề M4X Theme và thanh điều hướng dưới được ẩn hoàn toàn.
- Android chuyển ngang và ẩn thanh hệ thống; vuốt từ cạnh vẫn có thể gọi thanh hệ thống tạm thời.
- Joystick lấy vị trí thật của ngón tay, nút tròn bám theo ngón tay trong giới hạn điều khiển.
- Thả hoặc hủy chạm thì joystick trở về giữa và dừng nhân vật.
- Thêm dead-zone nhỏ để tránh nhân vật tự trôi.
- Bản đồ đổi sang sàn kim loại sci-fi, lưới neon, bệ hồi sinh và thùng chắn có chiều sâu.
- Nhân vật hình tròn được thay bằng lính robot nhìn từ trên xuống: thân, mũ, tay, chân, súng, kính neon và hiệu ứng bước chạy.
- Đạn có lõi sáng, quầng lửa và chớp đầu nòng.
- HUD mới gọn hơn: bảng xếp hạng nhỏ, đồng hồ giữa, minimap góc phải và máu/giáp/đạn ở dưới.
- Nút bắn, thay đạn và cứu thương được thu nhỏ để không che bản đồ.

## Phiên bản

```text
versionCode = 47
versionName = 3.3.1
```

## File thay đổi

```text
app/src/main/java/com/m4xtheme/app/ArenaGame.kt
app/src/main/java/com/m4xtheme/app/GameScreens.kt
app/src/main/java/com/m4xtheme/app/MainActivity.kt
app/build.gradle.kts
update.json
.github/workflows/build-apk.yml
```

Không cần chạy lại SQL Supabase vì bản này chỉ sửa giao diện và điều khiển.
