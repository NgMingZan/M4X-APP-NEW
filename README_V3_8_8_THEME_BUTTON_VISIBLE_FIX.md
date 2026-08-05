# M4X Theme v3.8.8 — Theme Button Visible Fix

## Lỗi
Nút Mua/Tải chỉ hiện phần trên vì trang chi tiết theme vẽ tràn xuống vùng
thanh cử chỉ của HyperOS.

## Đã sửa
- Đổi `decorFitsSystemWindows` thành `true`.
- Dùng `WindowInsets.navigationBars`.
- Thêm khoảng đệm đáy cố định 28dp.
- Tăng chiều cao tối thiểu của nút lên 58dp.
- Giữ một nút duy nhất: Mua / Nhận miễn phí / Tải theme.
- Không cần SQL mới.
