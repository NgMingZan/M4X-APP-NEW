# M4X Theme v3.8.3 — Purchase → Download Flow

## Luồng mới

Danh sách theme
→ Xem chi tiết
→ Vuốt toàn bộ ảnh
→ Mua bằng M4X Coin
→ Tải theme

## Thay đổi

- Danh sách theme không có nút mua trực tiếp.
- Trang chi tiết dùng HorizontalPager để vuốt ảnh thật.
- Nút Mua và Tải được tách thành hai bước.
- Sau khi mua, theme được lưu trong `theme_purchases`.
- Người dùng có thể tải lại theme đã mua mà không bị trừ Coin lần nữa.
- Theme miễn phí cũng phải bấm Nhận miễn phí trước khi tải.
- Lượt tải chỉ tăng khi người dùng thực sự mở hoặc tải file.
- Không cần SQL mới vì `theme_purchases` đã tồn tại.
