# M4X Theme v3.8.5 — Theme UI Syntax Fix

## Lỗi trong log
- `MainActivity.kt:1721: Function 'QuestHub' must have a body`
- `MainActivity.kt:1722: Syntax error: Expecting '('`

## Nguyên nhân
Bản v3.8.4 bị chèn thừa:

```kotlin
@Composable
private fun QuestHub
```

ngay trước hàm `QuestHub(...)` thật.

## Đã sửa
- Xóa đoạn khai báo thừa.
- Giữ nguyên giao diện Theme kiểu video.
- Giữ tên tác giả trong trang chi tiết.
- Không cần SQL mới.
