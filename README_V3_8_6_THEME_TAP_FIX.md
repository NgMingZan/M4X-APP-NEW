# M4X Theme v3.8.6 — Theme Tap Fix

## Lỗi trong video
- Bấm tab `Trên máy / Ưa thích / Thích / Đặt hàng` không phản hồi.
- Bấm một số thẻ theme không mở trang chi tiết.

## Đã sửa
- Tab chuyển thành vùng bấm thật bằng `Modifier.clickable`.
- Tab đang chọn đổi viền và trạng thái.
- `Đặt hàng` hiển thị các theme đã sở hữu.
- Theme card đổi sang `ElevatedCard(onClick = ...)` để nhận chạm ổn định.
- Thêm chữ `Chạm để xem` trên thẻ.
- Giữ giao diện lưới 3 cột, trang chi tiết nền đen, ảnh vuốt ngang và tên tác giả.
- Không cần SQL mới.
